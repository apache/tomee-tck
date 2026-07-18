#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Fails the build when a Maven-invoker-driven TCK reactor produced test
# failures, errors, or a silently green module.
#
#   verify-invoker-result.sh <reactor-root> <invoker-reports> <module>...
#
# The invoker runs with ignoreFailures=true (and the modules with
# maven.test.failure.ignore=true) so the whole compatibility baseline is
# collected in one pass; Maven therefore exits 0 regardless of the result and
# a module that dies before writing any surefire/failsafe report is silently
# green. This check is the gate that turns a red baseline back into a red
# build:
#
#   - test failures/errors: summed from every surefire/failsafe TEST-*.xml and
#     failsafe-summary.xml under the reactor (the raw reports the ignore flags
#     do not touch), including the deployment-error case failsafe records as
#     <errors> in the summary without a "Tests run" build-log line;
#   - tooling failures: an expected module whose invoker BUILD result is not
#     "success", or whose build log never reached the surefire/failsafe phase
#     (no "Tests run" and no "No tests to run"), never wrote a report and would
#     otherwise pass unnoticed.
#
# A module whose tests are all excluded legitimately reports "No tests to run"
# (or a passing surefire summary) and is accepted.

set -eu

reactor_root=$1
invoker_reports=$2
shift 2

status=0

fail() {
  echo "verify-invoker-result: $1" >&2
  status=1
}

# --- Test failures and errors across every raw report under the reactor. ---
# TEST-*.xml carries failures="" errors="" on the <testsuite> element;
# failsafe-summary.xml carries <failures>/<errors> (and records deployment
# errors the build log does not surface as a "Tests run" line).
report_failures=0
report_errors=0
for report in $(find "$reactor_root" -name 'TEST-*.xml' -path '*-reports/*' 2>/dev/null); do
  f=$(sed -n 's/.*<testsuite[^>]* failures="\([0-9]*\)".*/\1/p' "$report" | head -1)
  e=$(sed -n 's/.*<testsuite[^>]* errors="\([0-9]*\)".*/\1/p' "$report" | head -1)
  report_failures=$((report_failures + ${f:-0}))
  report_errors=$((report_errors + ${e:-0}))
done
for summary in $(find "$reactor_root" -name 'failsafe-summary.xml' 2>/dev/null); do
  f=$(sed -n 's/.*<failures>\([0-9]*\)<.*/\1/p' "$summary" | head -1)
  e=$(sed -n 's/.*<errors>\([0-9]*\)<.*/\1/p' "$summary" | head -1)
  report_failures=$((report_failures + ${f:-0}))
  report_errors=$((report_errors + ${e:-0}))
done
if [ "$report_failures" -ne 0 ] || [ "$report_errors" -ne 0 ]; then
  fail "$report_failures test failure(s) and $report_errors error(s) in $reactor_root"
fi

# --- Every expected module must have run its tests. ---
for module in "$@"; do
  build_result=$(sed -n 's/.*result="\([^"]*\)".*/\1/p' "$invoker_reports/BUILD-$module.xml" 2>/dev/null | head -1)
  if [ "$build_result" != "success" ]; then
    fail "invoker module '$module' did not build (result='${build_result:-missing}')"
    continue
  fi

  build_log=$(sed -n 's/.*buildlog="\([^"]*\)".*/\1/p' "$invoker_reports/BUILD-$module.xml" 2>/dev/null | head -1)
  if [ -z "$build_log" ] || [ ! -f "$build_log" ]; then
    fail "invoker module '$module' has no build log to verify"
    continue
  fi
  if ! grep -q 'BUILD SUCCESS' "$build_log"; then
    fail "invoker module '$module' build log does not report BUILD SUCCESS"
    continue
  fi
  # A module that never reached surefire/failsafe wrote no report and would be
  # silently green; require either executed tests or an explicit empty run.
  if ! grep -qE '\[INFO\] Tests run:|No tests to run|No tests were executed' "$build_log"; then
    fail "invoker module '$module' built but never ran its tests (no surefire/failsafe output)"
    continue
  fi
done

if [ "$status" -ne 0 ]; then
  echo "verify-invoker-result: FAILED for $reactor_root" >&2
  exit 1
fi

echo "verify-invoker-result: OK ($reactor_root, $# module(s), 0 failures, 0 errors)"
