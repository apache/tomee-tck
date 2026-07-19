#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Converts a JT Harness (JavaTest) run of a legacy old-tck suite into JUnit
# XML so the Jenkins junit step can surface per-test results.
#
#   runner-standalone/javatest-report-to-junit.sh <module-dir>
#
# Statuses come from the harness text report (target/*report/<suite>/text/
# summary.txt, one "<file>#<testid>  <Status>." line per test). For failed
# and errored tests the matching .jtr file in the work directory
# (target/*work/<suite>/) text-format sibling of the report - contributes
# the harness log from its testresult section as the failure body, so the
# Jenkins test page shows the per-test log without the archived HTML report.
# The XML lands in target/surefire-reports/, which the Jenkinsfile junit and
# archiveArtifacts globs already ingest; these antrun-driven modules run no
# surefire themselves, so the directory is otherwise unused.
#
# Only POSIX sh, awk and tr are used: the suites run inside plain
# eclipse-temurin containers that ship no python.

set -eu

MODULE_DIR=${1:?usage: javatest-report-to-junit.sh <module-dir>}
MODULE_DIR=$(CDPATH= cd -- "$MODULE_DIR" && pwd)
TARGET="$MODULE_DIR/target"
MODULE=$(basename "$MODULE_DIR")

converted=0
for summary in "$TARGET"/*report/*/text/summary.txt; do
  [ -f "$summary" ] || continue
  reportdir=${summary%/text/summary.txt}   # .../<x>report/<suite>
  suite=${reportdir##*/}
  prefix=${reportdir%/*}; prefix=${prefix##*/}; prefix=${prefix%report}
  workdir="$TARGET/${prefix}work/$suite"
  out="$TARGET/surefire-reports/TEST-javatest.$MODULE.$suite.xml"

  mkdir -p "$TARGET/surefire-reports"
  awk -v WORK="$workdir" -v SUITE="javatest-$MODULE" '
    function xesc(s) {
      gsub(/&/, "\\&amp;", s); gsub(/</, "\\&lt;", s)
      gsub(/>/, "\\&gt;", s); gsub(/"/, "\\&quot;", s)
      return s
    }
    # Reads the .jtr from its testresult section on (skipping the noisy
    # description/environment dumps), stripping XML-invalid control chars.
    function jtrbody(jtr,   cmd, l, started, content) {
      cmd = "tr -d \047\\000-\\010\\013\\014\\016-\\037\047 2>/dev/null < \047" jtr "\047"
      started = 0; content = ""
      while ((cmd | getline l) > 0) {
        if (!started) {
          if (l ~ /^#-----testresult-----/) started = 1
          continue
        }
        if (l ~ /^execStatus=/) { msg = substr(l, 12); gsub(/\\/, "", msg) }
        content = content l "\n"
      }
      close(cmd)
      if (length(content) > 100000)
        content = "[truncated, showing tail]\n" substr(content, length(content) - 100000)
      return content
    }
    {
      if (match($0, /  +/) == 0) next
      ref = substr($0, 1, RSTART - 1)
      status = substr($0, RSTART + RLENGTH)
      h = index(ref, "#"); if (h == 0) next
      path = substr(ref, 1, h - 1)
      id = substr(ref, h + 1)

      classname = path; sub(/\.java$/, "", classname); gsub(/\//, ".", classname)
      dir = path; sub(/\/[^\/]*$/, "", dir)
      base = path; sub(/^.*\//, "", base); sub(/\.java$/, "", base)
      jtr = WORK "/" dir "/" base "_" id ".jtr"

      tests++
      open = "  <testcase classname=\"" xesc(classname) "\" name=\"" xesc(id) "\""
      if (status ~ /^Passed/) {
        cases = cases open "/>\n"
      } else if (status ~ /^Not run/) {
        skipped++
        cases = cases open ">\n    <skipped/>\n  </testcase>\n"
      } else {
        tag = (status ~ /^Failed/) ? "failure" : "error"
        if (tag == "failure") failures++; else errors++
        msg = status
        body = jtrbody(jtr)
        if (body == "") body = "(no .jtr found at " jtr ")"
        cases = cases open ">\n    <" tag " message=\"" xesc(msg) "\">" \
                xesc(body) "</" tag ">\n  </testcase>\n"
      }
    }
    END {
      printf "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      printf "<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" errors=\"%d\" skipped=\"%d\">\n", \
        SUITE, tests, failures + 0, errors + 0, skipped + 0
      printf "%s", cases
      printf "</testsuite>\n"
    }
  ' "$summary" > "$out"
  echo "javatest-report-to-junit: wrote $out" >&2
  converted=$((converted + 1))
done

if [ "$converted" -eq 0 ]; then
  echo "javatest-report-to-junit: no JavaTest report under $TARGET" >&2
fi
