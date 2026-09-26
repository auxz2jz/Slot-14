# Diagnostics

v0.1.0 implements a bounded-purpose local diagnostic stream in `events.jsonl`.

Recorded categories include:

- LIFECYCLE
- PRECONDITION
- USER_ACTION
- OPERATION_START
- PROGRESS
- OPERATION_RESULT
- ERROR
- TEST
- TEST_RESULT
- EXPORT

The tracking progress record includes:

- frame number
- media time
- changed-pixel count
- active-track count
- predicted-track count

The diagnostic ZIP includes:

- README.txt
- summary.txt
- events.jsonl

Privacy rule: the user's source security video is not automatically copied into diagnostics.
