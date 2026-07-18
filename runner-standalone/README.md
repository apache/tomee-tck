# Standalone Jakarta EE 11 specification TCK runners

Jakarta EE 11 Web Profile certification requires, besides the Platform TCK
catalog in `runner-webprofile/`, a passing run of the independently published
TCK of each required component specification. This tree gathers those runners
so the whole certification effort lives in one repository.

Run a single TCK (fixed ports 8080/8443/8005/1527; one at a time):

```shell
runner-standalone/run-standalone-suite.sh concurrency
```

`TOMEE_CLASSIFIER` selects the distribution, default `plume`. The default
Maven build compiles these modules but runs nothing; execution requires
`-Dtck.standalone=true` (the script passes it).

Every wired runner applies the reviewed known-gap exclusion list from
[exclusions/](exclusions/README.md) by default, so a default run is expected
green and a failure is a regression. Pass
`-Dtck.exclusions.file=.../exclusions/none.txt` (or `=none` for the
TestNG-based runners) to collect the full compatibility baseline instead.
The excluded gaps are triaged centrally in
[KNOWN_ISSUES.md](../KNOWN_ISSUES.md).

## Status per required Web Profile 11 specification

Dates refer to runs against the TomEE Plume snapshot on Java 21. The failure
counts are the recorded full-baseline results that the exclusion lists were
derived from; with the default exclusions applied these suites run green.

| Specification | TCK source | Runner | Status |
|---|---|---|---|
| Annotations 3.0 | EFTL zip (installed as `jakartatck:jakarta-annotations-tck:3.0.0`) | `annotations` | **Passes** (signature test, 2026-07-18) |
| Dependency Injection 2.0 | EFTL zip (installed as `jakarta.inject:jakarta.inject-tck:2.0.2`) | `di` | **Passes 50/50** on OpenWebBeans via the CDI SE API (2026-07-18) |
| CDI 4.1 (core) + Interceptors 2.2 (non-EJB half) | `jakarta.enterprise:cdi-tck-core-impl:4.1.0` | `cdi` | **Runs: 1,388 tests, 90 failures, 82 skipped** (2026-07-18). The failures cluster in the new CDI 4.1 Method Invokers API (30) and build-compatible extensions (~25) — OpenWebBeans 4.1 feature gaps to triage upstream |
| CDI 4.1 (EE integration) + Interceptors 2.2 (EJB half) | `jakarta.tck:cdi-tck-ee-impl:11.0.3` | `cdi-ee` | **Runs: 1,829 tests, 117 failures, 92 skipped** (2026-07-18, needs the 4 GB server heap configured in its arquillian.xml) |
| Concurrency 3.1 | `jakarta.enterprise:jakarta.enterprise.concurrent-tck:3.1.1` | `concurrency` | **Runs: 187 tests, 45 failures, 49 errors, 14 skipped** (2026-07-18). Signature and virtual-thread tests pass; the remaining failures and errors are TomEE Concurrency 3.1 implementation gaps (null resource-definition injections, transaction semantics) |
| Data 1.0 | `jakarta.data:jakarta.data-tck:1.0.0` | `data` | **Runs: all 99 tests execute** after the runner's archive processor completes the TCK's own deployments (2026-07-18): 73 failures + 22 errors remain, all null repository injections — TomEE's Jakarta Data provider does not materialize repository implementations |
| Servlet 6.1 | EFTL zip (installed as `jakarta.tck:servlet-tck-runtime:6.1.0`) | `servlet` | **Runs: 1,706 tests, 69 errors, 7 skipped** (2026-07-18). The harness is complete: slf4j-simple resolves at the TCK-derived version, the TCK's bundled client certificate is trusted and mapped (both client-cert tests pass, including the https-targeted deployment via a metadata observer reporting the TLS port); remaining errors are behavioral differences (async dispatch connection handling, response-content mismatches) to triage as product results |
| Validation 3.1 | EFTL zip (installed as `jakarta.validation:validation-tck-tests:3.1.1`) | `validation` | **Runs: 1,049 tests, 124 failures** (2026-07-18) after pinning AssertJ 3.7.0 (the published TCK jar is compiled against its covariant signatures). 118 of the remaining failures are Apache BVal `validation.xml`/constraint-mapping XML parsing gaps (`Unable to parse null`, `Failed to parse XML deployment descriptor file`) — provider work, not harness |
| Security 4.0 | Source reactor zip 4.0.1 | `security` (Maven module) | **Runs: 26 app modules, 132 tests, 5 failures, 2 errors** (2026-07-18). The runner downloads and patches the reactor, injects a tomee-remote profile, and drives every module through the Maven invoker; the failures (e.g. BASIC auth mechanism answering 401 for valid credentials, two OpenID modules) are TomEE Jakarta Security results to triage |
| Authentication 3.1 | Source reactor zip 3.1.2 | `authentication` (Maven module) | **Runs: 11 of 12 Web Profile modules pass cleanly (45 tests, 0 failures)** (2026-07-18). The spi module executes with the full property wiring but fails 50 of 57 SPI conformance assertions against Tomcat's AuthConfigFactory — product results to triage. EJB/JACC/SOAP modules are outside the Web Profile scope |
| Faces 4.1 | Source reactor zip 4.1.2 | `faces` (Maven module) | **Runs: modern Arquillian modules complete — 315 tests, 45 failures, 46 errors** (2026-07-18) on Plume's Mojarra. A recurring product finding: TomEE's faces-config.xml unmarshaller rejects the `xsi:schemaLocation` attribute used by Faces 4.1 descriptors, failing those deployments. The ~5,500-test legacy JavaTest `old-tck` half is GlassFish-wired (asadmin deployment) and remains the one open port, as does the GlassFish-bound faces-signaturetest module |
| JSON Processing 2.1 | `jakarta.json:jakarta.json-tck-*:2.1.1` (Maven Central; byte-identical to the EFTL zip pinned in `environment/versions.env`) | `jsonp` | **Passes 197/197** (2026-07-18) against Apache Johnzon 2.1.0, the JSON-P provider bundled in the TomEE Plume snapshot: 179 functional + signature tests and 18 pluggability tests. The signature test checks the `jakarta.json` packages of the distribution's `jakartaee-api` jar |
| JSON Binding 3.0 | `jakarta.json.bind:jakarta.json.bind-tck:3.0.0` (Maven Central; byte-identical to the EFTL zip pinned in `environment/versions.env`) | `jsonb` | **Runs: 295 tests, 4 failures, 2 errors, 5 skipped** (2026-07-18) against Apache Johnzon 2.1.0 with OpenWebBeans as the CDI SE container (the runner boots it because the TCK's own private `@BeforeAll` bootstrap is ignored by JUnit). The signature test passes; 2 failures are the TCK's pre-CLDR-34 French locale expectations (JDK data, not Johnzon), the other 4 are Johnzon 2.1.0 gaps: BigDecimal/BigInteger serialized as JSON strings instead of numbers, `JsonbDeserializer` instances not resolved through CDI, and `@JsonbDateFormat` ignored on a `@JsonbCreator` parameter during polymorphic deserialization |
| Debugging Support 2.0 | EFTL zip (installed as `jakartatck:jakarta-debugging-tck:2.0.0`) | `debugging` | **Passes** (2026-07-18). The runner compiles the TCK's `testclient.war` JSPs offline with the Jasper compiler bundled in the TomEE distribution (SMAP generation and dumping enabled, no server needed) and the TCK's `VerifySMAP` validates the embedded `SourceDebugExtension` of both generated classes plus both dumped `.smap` files. The Platform catalog's Pages debugging classes also pass |
| Enterprise Beans 4.0 Lite | Covered by the Platform TCK catalog (`runner-webprofile`, `ejb30`/`ejb32`) | — | See `runner-webprofile/KNOWN_FAILURES.md` |
| Standard Tag Library 3.0 | Covered by the Platform TCK catalog (`tags-tck`) | — | Blocked by the Jakarta Tags TLD registration gap |
| Expression Language 6.0 | Standalone EL TCK (Maven Central `jakarta.el:...`-tck) | not yet scaffolded | Platform EL integration already runs in `runner-webprofile` |
| Pages 4.0 / WebSocket 2.2 / REST 4.0 / Transactions 2.0 / Persistence 3.2 | Standalone TCKs (zip or Maven Central) | not yet scaffolded | Platform integration already runs in `runner-webprofile` |

## Layout conventions

- `exclusions/<id>.txt` holds the reviewed known-gap exclusions applied by
  default through each runner's `tck.exclusions.file` property. JUnit-based
  runners use surefire's `excludesFile`; the TestNG-based runners (cdi,
  cdi-ee, validation) register the `ExclusionsAnnotationTransformer` from
  `tck-common` because suite-XML runs ignore `excludesFile`; the
  source-reactor runners pass the file into every inner TCK module as
  `-Dsurefire.excludesFile`/`-Dfailsafe.excludesFile` through the invoker.
- `<id>-install` modules download an EFTL distribution zip (SHA-256 pinned)
  and install its artifacts into the local repository; run them once before
  the matching runner (`run-standalone-suite.sh` chains them). Their
  install-file executions bind to the `tck.standalone.install.phase`
  property (unbound by default) because the goal has no skip parameter.
  TCKs whose Maven Central artifacts are byte-identical to the EFTL zip
  (jsonp, jsonb) skip the install module and resolve Central coordinates
  directly; the zip checksums stay recorded in `environment/versions.env`.
- Container-based runners inherit the TomEE overlay, Derby, and certificate
  lifecycle from the shared parent; `src/tomee-conf/` files replace
  same-named files from `environment/tomee/conf`.
- Runners that need no server (annotations, di, jsonp, jsonb, debugging)
  set `tck.standalone.container.skip=true` and run in the local JVM; the
  provider-level suites (jsonp, jsonb) test the exact Johnzon version the
  TomEE distribution bundles, and debugging drives the distribution's own
  Jasper compiler offline.
- TestNG-based TCKs (cdi, cdi-ee, validation) force the surefire TestNG
  provider; JUnit-based ones use the JUnit Platform or JUnit 4 provider.
