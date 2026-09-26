# Security Motion Tracker — Android

Canonical repository: **`auxz2jz/Slot-14`**

This project is an Android-first tracking engine for mostly stationary security-camera footage.
It began with the user's yard footage containing a goat moving across the scene.

## What v0.1.0 does

- Select a saved security-camera video using Android's system file picker.
- Analyze the video at 5 frames/second in the initial reliable offline decoder.
- Compare neighboring frames for direct pixel changes.
- Maintain an adaptive MOG2 background model.
- Build a binary motion mask (white = changed/moving, black = not moving).
- Keep the ORIGINAL COLOR frame for tracking.
- Use HSV color histograms to help keep the same track ID.
- Group changed pixels into motion regions.
- Use recent velocity to predict through short weak-motion/dropout periods.
- Draw red changed pixels, green observed boxes/trails, and amber predicted/coasting boxes/trails.
- Provide tuning controls for pixel threshold and minimum motion-region area.
- Include **Test This Version** guided testing.
- Include **Export Diagnostics** ZIP output without automatically including private source video.

## Important: black/white does not mean black/white tracking

The black/white view is a **binary debug mask** only:

- white = this pixel is currently considered motion/change
- black = this pixel is not currently considered motion/change

The engine still uses the source video's color. v0.1.0 uses a Hue/Saturation histogram for appearance matching between frames.

## Build

Current project baseline:

- Android Gradle Plugin: 9.4.0
- Gradle used by CI: 9.6.0
- compileSdk: 36
- targetSdk: 36
- minSdk: 26
- OpenCV Android: `org.opencv:opencv:5.0.0.1`
- JDK: 17

GitHub Actions builds a debug APK on every push to `main`.

## Status

**v0.1.0 — CANDIDATE**

No Android version has been physically verified by the user yet.
See `PROJECT_MEMORY.md` before making future changes.
