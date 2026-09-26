# Project Memory — Security Motion Tracker

**Canonical repository:** `auxz2jz/Slot-14`

This existing project has adopted the current Master Instruction Library without restructuring working source.

Required startup order before substantial work:

1. Read `auxz2jz/master-instruction-library/INSTRUCTION_INDEX.md` and all applicable mandatory standards.
2. Read `MASTER_RULE_ADOPTION.md`.
3. Read `CROSS_PLATFORM_COORDINATION.md`.
4. Read this `PROJECT_MEMORY.md`.
5. Read `MOTION_TRACKER_ROADMAP.md`, `TESTING.md`, and `DIAGNOSTICS.md`.
6. Identify the platform being worked on, its last user-verified baseline, and its latest legitimate candidate before changing source.

## Current status

- Project created: 2026-09-26
- Current Android version: **v0.1.0 — CANDIDATE (CI build successful)**
- Last user-verified Android version: **NONE YET**
- Latest Python reference: **v0.2 — CANDIDATE / assistant-tested on supplied goat footage**
- Last user-verified Python version: **NONE explicitly recorded**
- Master Instruction Library adoption: **ACTIVE**
- Pre-adoption repository checkpoint: `fc84431997ecd05ca4658bd440ac490fbde600fd`
- Initial adoption documentation checkpoint: `e8b1d5fa83477d3504850416e6da51e06802331d`
- Windows/PC product implementation: **NOT STARTED**

## Current task

Adopt the current Master Instruction Library around the existing project while preserving working behavior and the established Android structure.

Status: **DOCUMENTATION/SAFEGUARD MAPPING COMPLETE; NO TRACKER SOURCE MODIFIED FOR ADOPTION.**

## Adoption implementation plan

1. Preserve the successful Android candidate and its exact source/artifact identity.
2. Inspect the existing repository, checkpoints, roadmap, diagnostics, guided testing, and build workflow before changing anything.
3. Map the current paths to Master-rule ownership instead of moving/renaming them.
4. Add shared multi-agent/cross-platform coordination documentation.
5. Record missing diagnostic/testing safeguards as incremental future work rather than rewriting working code.
6. Keep Windows/PC status at NOT STARTED until a Windows implementation actually begins.
7. If any future structural migration is proposed, record a migration plan and checkpoint affected platform baselines before changing paths.

See `MASTER_RULE_ADOPTION.md` and `CROSS_PLATFORM_COORDINATION.md`.

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

## Build verification

- Successful GitHub Actions run: `36275458803`
- Successful source commit: `74974383ce67e412fd86048a309ebe377ef05b92`
- APK artifact ID: `10916866932`
- Artifact ZIP SHA-256: `604f7af14a290955ffa7092a56ff360642480c9e0cd7d91129b07bdf55dccaf2`
- Extracted APK SHA-256: `fb003c37b35f05843f9c659cb473275bb49e5af668f5ae3ad393a9d52fff70f7`
- Build status is CANDIDATE only; no physical Android test has been recorded.

## Failed/abandoned approaches

CI setup failures before source compilation:

1. `android-actions/setup-android@v3` requested obsolete SDK package `tools` and failed before compilation.
2. Direct `sdkmanager` call failed because the executable was not on PATH.
3. The full sdkmanager path was correct, but a YAML `run:` scalar beginning with a quoted command made the workflow invalid before job creation.
4. First real Kotlin compile exposed unresolved contour helper calls in the OpenCV binding. Region extraction was changed from contour area/bounding boxes to `connectedComponentsWithStats`, which is also a direct fit for grouping changed pixels. The next compile succeeded.

For the Python prototype, full-resolution 2560×1920 processing was unnecessarily slow for early tuning, so algorithm validation switched to a reduced working resolution. This is now an intentional design choice: analyze at reduced resolution first, then later map overlays back to full-resolution output.

## Files/results already received

- User-supplied private security footage: `NVR_road_NORMAL_1790370925915_95270004X34XC3GM.mp4` (used for algorithm testing; not stored in GitHub).
- Python/reference motion-tracking test outputs were generated during development.
- Android v0.1.0 debug APK was successfully built by GitHub Actions.
- Exact successful build/artifact identities and hashes are recorded above.
- No Android physical test result or diagnostic ZIP from the user's phone has been received yet.

## Master-standard safeguard backlog

The current implementation already has meaningful diagnostics and guided testing, but the following are planned incremental safeguards identified during adoption:

- operation/run IDs and correlation IDs;
- crash preservation with stack traces/cause chains;
- recent-event buffer and stall/watchdog detection;
- stronger precondition/result validation and diagnostic redaction;
- persistent per-step guided-test progress;
- structured TXT/JSON test reports and tester notes;
- stronger failed-test-to-diagnostic-event linkage.

These are safeguards to add one logical change at a time. They are not a reason to refactor the tracking engine.

## Exact next action

1. User installs the successful v0.1.0 debug APK.
2. User runs **Test This Version** on the same goat clip.
3. If visual tracking is wrong or the app errors, user exports diagnostics.
4. Use diagnostics plus the user's visual result for the next targeted change.
