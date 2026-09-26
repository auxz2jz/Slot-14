# Project Memory — Security Motion Tracker

**Canonical repository:** `auxz2jz/Slot-14`

Read this file before changing the project, then read `MOTION_TRACKER_ROADMAP.md`, `TESTING.md`, and `DIAGNOSTICS.md`.
Also follow the canonical master instruction library at `auxz2jz/master-instruction-library`.

## Current status

- Project created: 2026-09-26
- Current Android version: **v0.1.0 — CANDIDATE**
- Last user-verified Android version: **NONE YET**
- Latest Python reference: **v0.2 — CANDIDATE / assistant-tested on supplied goat footage**
- Last user-verified Python version: **NONE explicitly recorded**

## Source footage evidence

The user supplied security-camera yard footage containing a goat moving across the scene.
The source file was used locally for algorithm validation but is **not stored in this repository** because it is private source media.

Observed from the real clip:

- The camera is stationary enough for frame differencing/background subtraction to be useful.
- The goat creates a coherent changed-pixel region.
- v0.1 raw motion tracking followed the goat but could weaken/lose confidence when motion became small.
- v0.2 Python reference keeps source color for HSV appearance matching and adds short-term velocity prediction/coasting.
- A 30-second v0.2 validation run maintained a primary motion track across the goat's visible movement.
- Path smoothing was added for the Android port to reduce centroid jitter.

## Architecture decision

The binary black/white mask is only a motion classification layer. It is not the complete tracker.

Pipeline:

1. Decode source frame.
2. Downscale to working resolution (currently max width 640).
3. Preserve color frame.
4. Build grayscale direct frame-difference mask.
5. Build adaptive MOG2 foreground mask.
6. Use direct motion to gate nearby MOG2 foreground pixels.
7. Morphological cleanup.
8. Connected-component/contour regions.
9. Merge nearby regions.
10. Compute HSV appearance histogram for each region.
11. Associate regions to existing tracks using predicted position + color similarity + size consistency.
12. Coast a track briefly if direct motion disappears.
13. Smooth the displayed movement path.
14. Render red changed pixels + green observed tracks + amber predicted tracks.

## Current Android implementation

- Kotlin Android app.
- OpenCV Android AAR from Maven Central.
- Saved-video selection through `ACTION_OPEN_DOCUMENT`.
- v0.1.0 uses `MediaMetadataRetriever` at 5 analysis FPS for simplicity/reliability.
- Motion engine itself is independent enough to accept Bitmap frames from a faster decoder later.
- No camera permission is required for saved-video analysis.

## Known limitations

1. `MediaMetadataRetriever` frame extraction is reliable but not the fastest way to decode long videos.
2. v0.1.0 does not export an annotated MP4 yet.
3. Persistent/repetitive foliage motion can still create extra motion regions.
4. Color histogram matching is regional; it is not full object re-identification.
5. A fully stationary object stops producing changed pixels; a track is only coasted for a limited time.
6. No semantic classifier exists yet; the engine does not know that a target is a goat/person/car.
7. Multi-object identity crossings need more testing.

## Diagnostics

- Central JSONL event log.
- User action vs operation result separation.
- Tracking progress metrics.
- Errors are captured.
- Diagnostic ZIP export includes summary + event trace.
- Private source video is not automatically included.

## Guided test

Use **Test This Version** in the app.
Automatic prerequisites require:

- at least 30 processed frames
- at least one active track at some point
- at least one changed pixel
- no recorded tracking error

Final tracking correctness is visual and therefore requires the user to choose **Tracking Looks Correct** or **Problem**.

## Failed/abandoned approaches

None for Android yet.

For the Python prototype, full-resolution 2560×1920 processing was unnecessarily slow for early tuning, so algorithm validation switched to a reduced working resolution. This is now an intentional design choice: analyze at reduced resolution first, then later map overlays back to full-resolution output.

## Exact next action

1. Build v0.1.0 in GitHub Actions.
2. If compilation succeeds, provide the debug APK to the user.
3. User runs **Test This Version** on the same goat clip.
4. Use exported diagnostics plus the user's visual result for the next targeted change.
