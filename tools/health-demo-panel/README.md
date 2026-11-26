# Health demo panel

A one-page companion for demonstrating the Health tab. It sets a day's metrics for one
Fit-Nutrition account, so the BR4 and BR5 alert rules can be triggered on demand instead of
waited for.

## Why it exists

The Health tab used to read a Fitbit account through the Fitbit Web API. Google decommissioned
that API on 30 October 2026, and the app now reads Health Connect instead. Health Connect does
not exist on an emulator, so the sample-data source is the only one that works there — and a
source that generates its own deterministic days cannot be steered during a demo.

This panel steers it. `MockHealthDataSource` reads
`users/{uid}/mock_health/{yyyy-MM-dd}` from the Realtime Database before falling back to the
generated day, and this page is what writes there.

## Running it

```bash
python3 -m http.server 8777 --directory tools/health-demo-panel
```

Then open <http://localhost:8777>. Serving it beats opening the file directly, because
Firebase Auth is happier with an http origin.

You need two things to sign in:

- **The Realtime Database URL.** Firebase console, Realtime Database, the URL printed above the
  data tree. It is saved in the browser afterwards.
- **The email and password of the account you are demoing.** The same credentials you sign into
  the app with. The database rules only allow a user to touch their own node, so the panel
  cannot write to anyone else's.

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

The Firebase web config in `index.html` is the same public project identifier that ships in
`app/google-services.json`. A web API key is not a secret; the database rules are what protect
the data.
