#!/usr/bin/env python3
"""Serves the health demo panel and bridges it to a connected device over adb.

The panel used to write overrides to Firestore, which meant signing in as the account being
demoed. Nobody reliably remembers which account an emulator is signed into, so the panel now
sends the numbers to the app over adb instead. No credentials, and it follows whichever
install is in front of you rather than an account.

The app does the write, through a debug-only broadcast receiver. A file pushed with `adb push`
is owned by the shell user and the app cannot read it back, so the broadcast is what makes the
round trip work.

    python3 tools/health-demo-panel/serve.py

Then open http://localhost:8777.
"""

from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

PACKAGE = "sheridan.dheripu.fitnutrition"
RECEIVER = f"{PACKAGE}/.debug.MockHealthDebugReceiver"
ACTION = f"{PACKAGE}.SET_MOCK_DAY"
REMOTE_DIR = f"/sdcard/Android/data/{PACKAGE}/files/mock_health"
PORT = 8777
HERE = Path(__file__).resolve().parent
ISO_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")

# Reason: the panel sends only these, and every one is a number. Anything else is ignored
# rather than forwarded, so a typo in the page cannot put odd extras on the broadcast.
INT_FIELDS = ("steps", "activeMinutes", "heartRate", "sleepMinutes", "calories")
FLOAT_FIELDS = ("distance",)


class AdbError(RuntimeError):
    pass


def adb(*args: str, stdin_bytes: bytes | None = None) -> str:
    if shutil.which("adb") is None:
        raise AdbError("adb is not on your PATH. Add the Android SDK platform-tools to it.")
    result = subprocess.run(
        ("adb", *args), input=stdin_bytes, capture_output=True, timeout=30,
    )
    if result.returncode != 0:
        message = (result.stderr or result.stdout).decode("utf-8", "replace").strip()
        raise AdbError(message or f"adb {' '.join(args)} failed")
    return result.stdout.decode("utf-8", "replace")


def devices() -> list[str]:
    lines = adb("devices").splitlines()[1:]
    return [line.split("\t")[0] for line in lines if line.strip().endswith("device")]


def device_state() -> dict:
    """What the panel shows instead of a sign-in form. No account details are read."""
    try:
        attached = devices()
    except AdbError as error:
        return {"ok": False, "message": str(error)}

    if not attached:
        return {"ok": False, "message": "No device or emulator attached. Start one and reload."}
    if len(attached) > 1:
        return {"ok": False, "message": f"{len(attached)} devices attached. Leave one running."}

    serial = attached[0]
    try:
        model = adb("-s", serial, "shell", "getprop", "ro.product.model").strip()
        installed = PACKAGE in adb("-s", serial, "shell", "pm", "list", "packages", PACKAGE)
    except AdbError as error:
        return {"ok": False, "message": str(error)}

    if not installed:
        return {"ok": False, "serial": serial, "model": model,
                "message": f"{PACKAGE} is not installed on {model}. Run ./gradlew :app:installDebug."}

    return {"ok": True, "serial": serial, "model": model, "package": PACKAGE}


def day_path(date: str) -> str:
    if not ISO_DATE.match(date):
        raise AdbError(f"Not an ISO date: {date}")
    return f"{REMOTE_DIR}/{date}.json"


def broadcast(date: str, extras: list[str]) -> None:
    day_path(date)  # validates the date before it reaches the device
    output = adb(
        "shell", "am", "broadcast", "-n", RECEIVER, "-a", ACTION, "--es", "date", date, *extras,
    )
    if "Broadcast completed" not in output:
        raise AdbError(output.strip() or "The app did not accept the broadcast.")


def write_day(date: str, values: dict) -> None:
    extras: list[str] = []
    for field in INT_FIELDS:
        if field in values:
            extras += ["--ei", field, str(int(values[field]))]
    for field in FLOAT_FIELDS:
        if field in values:
            extras += ["--ef", field, str(float(values[field]))]
    broadcast(date, extras)


def read_day(date: str) -> dict | None:
    listing = adb("shell", "ls", REMOTE_DIR) if directory_exists() else ""
    if f"{date}.json" not in listing.split():
        return None
    try:
        return json.loads(adb("shell", "cat", day_path(date)))
    except (json.JSONDecodeError, AdbError):
        return None


def directory_exists() -> bool:
    try:
        adb("shell", "ls", REMOTE_DIR)
        return True
    except AdbError:
        return False


def clear_day(date: str) -> None:
    broadcast(date, ["--ez", "clear", "true"])


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(HERE), **kwargs)

    def log_message(self, fmt, *args):  # quieter than the default one-line-per-asset log
        if "/api/" in str(args[0] if args else ""):
            super().log_message(fmt, *args)

    def send_json(self, payload: dict, status: int = 200) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def query_date(self) -> str:
        return parse_qs(urlparse(self.path).query).get("date", [""])[0]

    def do_GET(self):
        route = urlparse(self.path).path
        if route == "/api/device":
            return self.send_json(device_state())
        if route == "/api/day":
            try:
                return self.send_json({"ok": True, "day": read_day(self.query_date())})
            except AdbError as error:
                return self.send_json({"ok": False, "message": str(error)}, 400)
        return super().do_GET()

    def do_POST(self):
        if urlparse(self.path).path != "/api/day":
            return self.send_error(404)
        length = int(self.headers.get("Content-Length", 0))
        try:
            body = json.loads(self.rfile.read(length) or b"{}")
            write_day(body.get("date", ""), body)
            return self.send_json({"ok": True})
        except (json.JSONDecodeError, AdbError, KeyError) as error:
            return self.send_json({"ok": False, "message": str(error)}, 400)

    def do_DELETE(self):
        if urlparse(self.path).path != "/api/day":
            return self.send_error(404)
        try:
            clear_day(self.query_date())
            return self.send_json({"ok": True})
        except AdbError as error:
            return self.send_json({"ok": False, "message": str(error)}, 400)


def main() -> int:
    state = device_state()
    print(f"Health demo panel on http://localhost:{PORT}")
    print(f"  device: {state.get('model', 'none')} — {state.get('message', 'ready')}")
    try:
        ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
    except KeyboardInterrupt:
        print("\nstopped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
