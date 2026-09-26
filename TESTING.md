# Testing — v0.1.0

## User-facing guided test

Use **Test This Version** inside the app.

### Step 1 — Select source video

**WHAT TO DO**
Tap **Select Video** and choose the yard security-camera clip containing the goat.

**EXPECTED RESULT**
The app reports that a video is selected and ready.

### Step 2 — Start tracking

**WHAT TO DO**
Tap **Start Tracking**.

**EXPECTED RESULT**
The preview begins updating. Changed pixels are red. Observed motion regions are green. A briefly missing/weak region may turn amber while the engine predicts/coasts.

### Step 3 — Run enough frames

Allow at least 30 analysis frames to process.

Automatic prerequisites:

- processedFrames >= 30
- maxTrackCount > 0
- maxChangedPixels > 0
- no tracking exception

### Step 4 — Visual confirmation

**EXPECTED RESULT**
The primary motion box should stay on the goat/moving object and the trail should follow its movement across the yard.

Use:

- **Tracking Looks Correct** = MANUAL_PASS, only after automatic prerequisites pass.
- **Problem** = MANUAL_FAIL.

On failure, export diagnostics and share the ZIP.

## Important

A successful build is not a verified tracker.
The version becomes VERIFIED only after the user physically tests it and confirms the behavior.
