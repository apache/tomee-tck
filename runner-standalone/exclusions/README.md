# Reviewed exclusion lists for the standalone TCK runners

One file per runner, applied by default through the runner's
`tck.exclusions.file` property. Every entry is a reviewed known gap from the
2026-07-18 baseline runs on the TomEE Plume snapshot (Java 21); the lists keep
the remaining suites runnable and CI-green, they do not turn a result into a
compatibility pass. [KNOWN_ISSUES.md](../../KNOWN_ISSUES.md) is the central
triage list for everything these files exclude.

Two formats, depending on how the runner executes tests:

- **JUnit-based runners** (`concurrency`, `data`, `servlet`, `pages`,
  `rest`, `websocket`, `jsonp`, `jsonb`, `debugging`, `el`, `persistence`)
  and the **source-reactor runners** (`security`, `authentication`,
  `faces`): maven-surefire/failsafe `excludesFile` patterns —
  `**/path/to/Class.java` excludes a class, `**/path/to/Class.java#method`
  a single method. The source-reactor runners hand the file to every inner
  TCK module as `-Dsurefire.excludesFile`/`-Dfailsafe.excludesFile`.
- **TestNG-based runners** (`cdi`, `cdi-ee`, `validation`): fully qualified
  class names (whole class, required where the deployment/configuration
  phase itself fails) or `Class#method` lines, read by the
  `ExclusionsAnnotationTransformer` listener in `tck-common`, because
  suite-XML runs ignore surefire's `excludesFile`. The listener matches
  both the declaring class and the concrete runtime class — several TCK
  base classes contribute inherited test methods that only the subclass
  name identifies.
- **JavaTest-based runners** (`transactions`, `faces-old`): JavaTest jtx
  exclusion lines (`test/path/File.java#testid`), appended by the runner to
  the harness `ts.jtx` before the run.

To collect a full compatibility baseline without exclusions, run a suite with
`-Dtck.exclusions.file=$(pwd)/runner-standalone/exclusions/none.txt` (the
TestNG-based runners also accept `-Dtck.exclusions.file=none`), then compare
the surefire reports against the file here and against
[KNOWN_ISSUES.md](../../KNOWN_ISSUES.md). When a product or provider fix lands,
delete the corresponding entries rather than leaving them to rot.
