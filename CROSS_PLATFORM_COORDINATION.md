# Cross-Platform Coordination — Security Motion Tracker

This file is the shared product/feature coordination layer for Android/mobile ChatGPT, normal ChatGPT, and any Windows/PC agent working in `auxz2jz/Slot-14`.

**Principle:** ONE PRODUCT / SHARED INTENT / SEPARATE PLATFORM IMPLEMENTATIONS.

## Current platform status

- **Android:** v0.1.0 CANDIDATE. CI build succeeded. No user-verified Android baseline yet.
- **Windows/PC:** NOT STARTED.
- **Python reference:** algorithm/test harness only. It is not the Windows product implementation.

## Ownership zones without moving existing files

### Android-owned

- `app/**`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `.github/workflows/android-build.yml`
- `PROJECT_MEMORY.md`
- `MOTION_TRACKER_ROADMAP.md`
- `TESTING.md`
- `DIAGNOSTICS.md`
- Android candidate/verified records and APK artifacts

### Shared coordination/reference

- `README.md` as repository entry point
- `MASTER_RULE_ADOPTION.md`
- `CROSS_PLATFORM_COORDINATION.md`
- `reference-python/**` as an algorithm/reference harness

Before changing a shared file, read its latest repository version and make the smallest necessary edit.

### Windows/PC-owned

No Windows implementation exists yet.

When Windows work starts, create a clearly isolated `windows/` area (or another explicitly agreed Windows-only path) containing Windows source, build files, memory/checkpoint, roadmap, tests, diagnostics, candidate records, and release artifacts.

A Windows/PC agent must not reorganize or move the existing Android project just to create matching directory names.

## Shared product vision

Create a motion-tracking engine for mostly stationary security-camera footage that can:

- identify pixels/regions that meaningfully changed;
- group changing pixels into moving regions;
- maintain movement tracks across frames;
- show the path of motion across the scene;
- use source color/appearance information in addition to the binary motion mask;
- survive short periods of weak or missing motion;
- remain usable without mandatory semantic AI classification;
- expose diagnostics and guided testing so tracking failures can be reproduced and improved.

Platform UI, libraries, decoding pipelines, performance techniques, and release versions may differ.

## Shared feature catalog

Stable IDs below describe product intent, not platform implementation details.

### F-001 — Saved security-video analysis
Analyze a user-selected recorded security-camera video.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-002 — Changed-pixel visualization
Detect meaningful frame/background changes and visually highlight changed pixels.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-003 — Adaptive background model
Use a changing background model so a mostly fixed scene can distinguish foreground motion from normal background.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-004 — Motion-region grouping and persistent IDs
Group neighboring changed pixels into motion regions and assign persistent track IDs across frames.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-005 — Color/appearance-assisted identity
Retain source color and use appearance information to help decide whether a later region is the same tracked target.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-006 — Short-term prediction/coasting
Predict a track briefly when direct motion becomes weak or disappears.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-007 — User-adjustable raw motion sensitivity
Allow adjustment of changed-pixel threshold and minimum tracked region size.

Android: IMPLEMENTED / CANDIDATE  
Windows: NOT STARTED

### F-008 — Diagnostic export
Provide persistent diagnostic evidence and an explicit export action without automatically including private source video.

Android: IMPLEMENTED / CANDIDATE, Master-standard enhancements still planned  
Windows: NOT STARTED

### F-009 — Guided version testing
Provide a version-specific guided tracking test with automatic evidence plus human visual confirmation.

Android: IMPLEMENTED / CANDIDATE, Master-standard enhancements still planned  
Windows: NOT STARTED

### F-010 — Foliage/repetitive-motion suppression
Reduce false motion tracks caused by repeated tree, grass, flag, or similar background movement.

Android: PLANNED  
Windows: NOT STARTED

### F-011 — Ignore zones
Allow user-defined scene areas that should be ignored or deprioritized for motion tracking.

Android: PLANNED  
Windows: NOT STARTED

### F-012 — Annotated video export
Export video with motion highlights, track boxes, IDs, and trails.

Android: PLANNED  
Windows: NOT STARTED

### F-013 — Improved reacquisition/continuity
Use optical flow, better motion modeling, color back-projection, or equivalent platform-appropriate techniques to improve target continuity and reacquisition.

Android: PLANNED  
Windows: NOT STARTED

### F-014 — Optional semantic labels
Optionally identify tracks as animal/person/vehicle/etc. after raw motion tracking is reliable. Semantic classification must supplement rather than replace the raw motion engine.

Android: PLANNED  
Windows: NOT STARTED

## Shared requirements

1. The black/white motion mask is a debug/classification representation only; it does not mean the tracker is restricted to black-and-white information.
2. Source color/appearance information may be used for target identity.
3. Raw motion tracking must remain useful without a mandatory neural-network classifier.
4. Private source security footage must not be automatically included in diagnostic exports.
5. A successful build is not a user-verified tracking result.
6. Android and Windows may implement the same feature using different libraries or UI workflows.
7. Each platform maintains its own verified baseline and candidate version.
8. Shared feature status must never falsely mark another platform as implemented or verified.

## Shared decisions

### D-001 — Preserve existing Android layout
The existing Android `app/` project and root Android documentation remain in place. No migration to an example `android/` directory is required.

### D-002 — Add Windows non-disruptively
When Windows development starts, add a new isolated Windows area rather than moving Android source.

### D-003 — Python code is reference, not Windows product
`reference-python/` may help validate platform-neutral algorithms but does not count as the Windows implementation or Windows verified baseline.

### D-004 — Separate platform versions
Android and Windows version numbers may diverge. Feature IDs provide shared continuity.

## Worker startup/handoff procedure

Any worker resuming this repository should:

1. Read `auxz2jz/master-instruction-library/INSTRUCTION_INDEX.md` and all applicable mandatory rules.
2. Read `MASTER_RULE_ADOPTION.md`.
3. Read this file.
4. Decide which platform the current task belongs to.
5. Read only that platform's memory/checkpoint/roadmap/testing/diagnostics plus relevant shared files.
6. Identify that platform's last user-verified baseline and latest candidate.
7. Review shared features/decisions added since the previous session.
8. Do not modify another platform's source or baseline records without explicit user authorization.
9. For shared-file edits, fetch the latest version first and preserve unrelated content.
10. Record reusable cross-platform discoveries here; keep platform-specific compiler/build details in platform-owned documentation.

## Future Windows initialization

When the first Windows/PC agent begins:

- create Windows-specific memory/checkpoint, roadmap, testing, and diagnostics records;
- set Windows last verified baseline to NONE until the user actually tests it;
- review F-001 through F-014 and mark each Windows status honestly;
- reuse shared algorithm intent where useful, but do not copy Android implementation assumptions blindly;
- keep Windows builds/artifacts separate from Android APKs.
