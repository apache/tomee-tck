# Platform TCK exclusions

Each suite partition has a Failsafe exclusion file named after its manifest ID.
The files in this directory describe the default TomEE Plume distribution
(EclipseLink). A subdirectory named after another distribution classifier,
such as `webprofile/` (OpenJPA), overrides individual partitions for that
distribution; `run-platform-suite.sh` selects the override when
`TOMEE_CLASSIFIER` names it and the file exists, and falls back to this
directory otherwise.
Blank lines and lines beginning with `#` are ignored. Exclude the narrowest
possible class or method and add, immediately above it:

- the observed failure;
- the TomEE or TCK issue URL;
- the date and Java version on which it was reproduced.

An exclusion is a recorded compatibility gap, not a passing test. Never use
`testFailureIgnore` or a wildcard that hides unrelated tests.
