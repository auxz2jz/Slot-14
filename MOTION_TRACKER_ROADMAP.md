# Security Motion Tracker Roadmap

## v0.1.0 — Raw motion tracker Android baseline — CANDIDATE

- [x] Saved-video file picker
- [x] Reduced-resolution analysis
- [x] Frame-to-frame changed pixels
- [x] Adaptive background subtraction
- [x] Binary debug motion mask
- [x] Preserve source color
- [x] HSV appearance matching
- [x] Group nearby changed pixels
- [x] Persistent track IDs
- [x] Short-term motion prediction/coasting
- [x] Smoothed movement trails
- [x] Sensitivity controls
- [x] Guided test
- [x] Diagnostic event logging
- [x] Diagnostic ZIP export
- [x] GitHub Actions compile/build success
- [ ] User physical verification

## v0.2.x — Performance and security-camera cleanup

- [ ] Replace 5 FPS `MediaMetadataRetriever` loop with faster sequential MediaCodec/ImageReader decoder.
- [ ] Process at configurable analysis FPS.
- [ ] Map low-resolution tracking coordinates back to full-resolution source coordinates.
- [ ] Export annotated MP4.
- [ ] Export standalone mask MP4 optionally.
- [ ] Add repetitive-motion/foliage heatmap suppression.
- [ ] Add user-defined ignore zones (trees, flags, road edge, etc.).
- [ ] Add minimum persistence before creating a visible track ID.
- [ ] Add separate thresholds for highlights vs track creation.

## v0.3.x — Better target continuity

- [ ] Optical flow inside active target regions.
- [ ] Kalman filter or equivalent motion model.
- [ ] Color back-projection reacquisition near predicted position.
- [ ] Longer stationary-target hold mode when explicitly selected.
- [ ] Occlusion/reappearance handling.
- [ ] Track confidence timeline.

## v0.4.x — Multi-object robustness

- [ ] Better assignment for crossing objects.
- [ ] Appearance signature beyond coarse HSV histogram.
- [ ] Track split/merge handling.
- [ ] Re-identification after longer gaps.
- [ ] Per-track movement distance, direction, speed, and dwell time.

## v0.5.x — Optional semantic detection

Only after raw motion tracking is reliable:

- [ ] Optional goat/animal/person/vehicle detector.
- [ ] Use semantic detection to label tracks, not to replace raw motion tracking.
- [ ] Keep operation possible without an AI model.
