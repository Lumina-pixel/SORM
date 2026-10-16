# SORM Dashboard (Member 4)

Web page that lists all lab computers with status, CPU, RAM, disk and last seen.
Refreshes every 10 seconds. Click a row to see its last 10 readings.

## Run
1. Open the `dashboard/` folder in VS Code.
2. Right-click `index.html` and choose "Open with Live Server".

## Fake data or real Backend
In `script.js`, top of the file:
- `USE_FAKE = true`  -> reads `fake_devices.json` (no Backend needed)
- `USE_FAKE = false` -> calls `http://localhost:8080/api/devices` (Backend must be running)

Usage above 90% is highlighted in red. If the Backend is down, the page shows an error message and keeps retrying.
