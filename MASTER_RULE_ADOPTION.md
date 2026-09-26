# Master Instruction Library Adoption

**Adopted:** 2026-09-26  
**Project:** Security Motion Tracker  
**Repository:** `auxz2jz/Slot-14`

This existing project adopts the current rules from `auxz2jz/master-instruction-library` without reorganizing, renaming, moving, or rewriting working source merely to resemble the examples in the Master Instruction Library.

## Master rules read for this adoption

- `INSTRUCTION_INDEX.md`
- `CORE_DEVELOPMENT_RECOVERY_RULES.md`
- `DIAGNOSTICS_STANDARD.md`
- `GUIDED_TESTING_STANDARD.md`
- `CROSS_PLATFORM_COLLABORATION_STANDARD.md`
- `RULE_CHANGELOG.md`

The Cross-Platform standard is treated as applicable because the repository may be used by Android/mobile ChatGPT and a Windows/PC agent. It explicitly allows existing projects to keep equivalent files and layouts rather than forcing the example folder names.

## Pre-adoption checkpoint

The repository was inspected before any adoption changes.

- Pre-adoption main HEAD: `fc84431997ecd05ca4658bd440ac490fbde600fd`
- Last successfully compiled Android candidate source: `74974383ce67e412fd86048a309ebe377ef05b92`
- Successful GitHub Actions run: `36275458803`
- APK artifact ID: `10916866932`
- Artifact ZIP SHA-256: `604f7af14a290955ffa7092a56ff360642480c9e0cd7d91129b07bdf55dccaf2`
- APK SHA-256: `fb003c37b35f05843f9c659cb473275bb49e5af668f5ae3ad393a9d52fff70f7`
- Android status: **CANDIDATE**
- Android last user-verified baseline: **NONE YET**
- Windows/PC implementation: **NOT STARTED**

A comparison from the successful source commit to the pre-adoption HEAD showed only documentation changes in `README.md`, `PROJECT_MEMORY.md`, and `MOTION_TRACKER_ROADMAP.md`. No Android source changed after the successful candidate build.

## Existing structure mapped to the Master rules

The existing layout is retained.

| Existing path | Master-rule role | Ownership |
| --- | --- | --- |
| `app/` | Android implementation source | Android worker |
| `app/build.gradle.kts`, root Gradle files | Android build configuration | Android worker |
| `.github/workflows/android-build.yml` | Android CI/build workflow | Android worker |
| `PROJECT_MEMORY.md` | Android project memory/checkpoint/handoff | Android worker |
| `MOTION_TRACKER_ROADMAP.md` | Android platform roadmap | Android worker |
| `TESTING.md` | Android testing/guided-test documentation | Android worker |
| `DIAGNOSTICS.md` | Android diagnostic documentation | Android worker |
| `reference-python/` | Algorithm/reference test harness; not a Windows product implementation | Shared/reference; change deliberately |
| `README.md` | Repository entry point/current Android summary | Shared entry point; preserve existing content |
| `CROSS_PLATFORM_COORDINATION.md` | Shared product/feature/ownership coordination | Shared |
| `MASTER_RULE_ADOPTION.md` | Mapping and adoption record | Shared |

A future Windows/PC implementation should be created in a new clearly isolated `windows/` area or another explicitly agreed Windows-only path. **Do not move the existing Android source merely to create symmetry.**

## Core development/recovery compliance

Already present:

- durable `PROJECT_MEMORY.md`
- explicit CANDIDATE vs VERIFIED distinction
- exact successful source commit and artifact hashes
- roadmap
- known limitations
- failed approaches and first-real-failure history
- exact next action
- evidence-first compiler debugging history
- successful CI build checkpoint

Safeguards adopted for all future work:

1. Read the Master Library first.
2. Read this adoption file and `CROSS_PLATFORM_COORDINATION.md`.
3. Read `PROJECT_MEMORY.md`, roadmap, testing, and diagnostics.
4. Identify the platform being worked on.
5. Identify that platform's last user-verified baseline and latest legitimate candidate.
6. Record the current task and implementation plan before substantial source changes.
7. Checkpoint before risky/refactoring/architecture changes.
8. Make the smallest evidence-based change.
9. Build/test before unrelated feature work.
10. Record result, failure evidence, and exact next action before ending work.

## Diagnostics standard mapping

### Already implemented

The current Android source already includes:

- central JSONL diagnostic logger
- sequence numbers
- UTC timestamps
- monotonic elapsed time
- application session ID
- semantic `USER_ACTION` events
- `OPERATION_START`, `PROGRESS`, `OPERATION_RESULT`, `ERROR`, `TEST_RESULT`, and export events
- bounded log rotation
- tracking progress metrics
- diagnostic ZIP export
- export-start/export-success/export-failure logging
- source-video privacy protection: source video is not automatically included
- basic lifecycle/precondition logging

### Missing or partial safeguards to add incrementally

These are **documented gaps**, not justification for an immediate rewrite:

- operation/run IDs and correlation IDs
- explicit event IDs/severity fields where useful
- global uncaught-crash preservation
- full stack traces/cause chains for important failures
- recent-event buffer separate from the persistent trace
- progress/stall watchdog
- stronger precondition/result validation
- before/after values for setting changes
- centralized redaction/sanitization layer
- structured per-run result report when useful

These should be implemented one logical safeguard at a time and tested without changing tracker behavior unnecessarily.

## Guided testing standard mapping

### Already implemented

- user-facing **Test This Version**
- version displayed to tester
- persistent test session ID/state
- explicit WHAT TO DO / EXPECTED RESULT instructions
- automatic prerequisites
- visual human confirmation
- manual **Problem** failure control
- diagnostic correlation through test session ID in test events

### Missing or partial safeguards to add incrementally

- explicit persistent current test step/completed-step state
- per-step IDs and start/pass/fail events
- automatic result-source field such as AUTO_VERIFIED / MANUAL_PASS / MANUAL_FAIL
- tester notes on failure
- structured TXT/JSON test report export
- timeout rules where meaningful
- tighter linkage between a failed test and the relevant diagnostic event IDs

Do not replace the current guided test wholesale merely to satisfy naming examples.

## Cross-platform/multi-agent adoption

See `CROSS_PLATFORM_COORDINATION.md`.

Key rules:

- Android and Windows maintain separate candidate versions, verified baselines, checkpoints, builds, tests, diagnostics, and artifacts.
- A Windows/PC agent must not modify `app/`, Android build configuration, Android project memory, or Android verified/candidate records unless explicitly authorized.
- Android work must not modify a future Windows implementation without explicit authorization.
- Shared changes must start from the latest shared file version and preserve unrelated edits.
- Shared feature intent propagates through the shared feature catalog; implementation code does not have to be identical.

## Structural migration decision

**No structural migration is being performed.**

Moving the current Android project under a new `android/` directory would provide little immediate benefit and would create unnecessary build/path risk. The current structure is clear enough to map ownership by documentation.

If a later structural migration becomes genuinely beneficial:

1. record the reason and exact path-move plan;
2. checkpoint all affected platform states;
3. preserve each user-verified baseline;
4. identify every build/workflow path that must change;
5. obtain explicit user direction before moving existing platform source;
6. migrate separately from feature changes;
7. rebuild and retest affected verified behavior.

The preferred non-disruptive expansion is to leave Android where it is and add a separate Windows-only area when Windows development actually begins.
