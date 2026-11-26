# Health demo panel

A one-page companion for demonstrating the Health tab. It sets a day's metrics for one
Fit-Nutrition account, so the BR4 and BR5 alert rules can be triggered on demand instead of
waited for.

## Why it exists

The Health tab used to read a Fitbit account through the Fitbit Web API. Google decommissioned
that API on 30 October 2026, and the app now reads Health Connect instead. An emulator on
Android 14 or later has Health Connect and the permission flow works there, but nothing writes
steps or sleep to it, so a real read comes back empty. The sample-data source is what fills the
tab — and a source that generates its own deterministic days cannot be steered during a demo.

This panel steers it. `MockHealthDataSource` reads a day from the app's own
`files/mock_health/{yyyy-MM-dd}.json` before falling back to the generated day, and this page
is what puts it there.

## Running it

```bash
python3 tools/health-demo-panel/serve.py
```

Then open <http://localhost:8777>. There is nothing to sign into. The page talks to a small
local server that drives `adb`, so it follows whichever device is attached and whatever
account that install happens to be signed into — which is the point: nobody remembers the
throwaway account an emulator was set up with.

You need `adb` on your PATH and exactly one device or emulator attached, with a debug build
installed. The panel says so plainly if any of that is missing.

## Using it in a demo

1. Switch **Use sample health data** on in the app's Health tab.
2. Pick a date in the panel, set the numbers or hit a preset, and save.
3. Sync the Health tab. The day shows the panel's numbers, and the alerts follow.

The **High resting HR** preset fires a BR5 alert on its own. BR4 needs three consecutive poor
nights, so set the **Poor sleep** preset on today and the two days before it.

"Clear this day" removes the override and hands the day back to the generated data.

## Keeping it honest

The thresholds the panel previews are duplicated from `model/health/HealthRules.kt`:

| Rule | Threshold |
| --- | --- |
| High resting heart rate | above 100 bpm |
| Low resting heart rate | below 40 bpm, and above 0 |
| Poor night | under 360 minutes asleep |
| Poor sleep streak | 3 consecutive poor nights |

If one of those moves in the Kotlin, move it here too. Otherwise the panel promises an alert
the app will not raise, which is a worse demo than no panel at all.

## How it reaches the app

`serve.py` sends an `am broadcast` to `MockHealthDebugReceiver`, which lives in `src/debug` and
so is absent from a release build. The app writes the file itself rather than taking an
`adb push`, because a pushed file is owned by the shell user and the app cannot read it back.
The app writes to its external files directory, which keeps the file readable from `adb` too,
so the panel can show what a day is already set to.
