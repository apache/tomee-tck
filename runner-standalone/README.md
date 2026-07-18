# Standalone Jakarta EE 11 specification TCK runners

Jakarta EE 11 Web Profile certification requires, besides the Platform TCK
catalog in `runner-webprofile/`, a passing run of the independently published
TCK of each required component specification. This tree gathers those runners
so the whole certification effort lives in one repository.

Run a single TCK (default ports 8080/8443/8005/1527; one at a time):

```shell
runner-standalone/run-standalone-suite.sh concurrency
```

The Derby port is overridable everywhere through `-Dtck.derby.port`; the
`concurrency`, `data`, `servlet`, `validation`, `cdi`, `cdi-ee`, `pages`,
`rest`, `websocket`, `transactions`, and `faces-old` runners additionally
accept `-Dtomee.http.port`, `-Dtomee.https.port`, and `-Dtomee.shutdown.port`
(`transactions` and `faces-old` also `-Dtck.harness.log.port` for their
JavaTest listeners), so they can run next to another harness instance (the
source-reactor runners security, authentication, and faces still assume the
fixed TomEE ports).
Overriding the ports matters beyond convenience: the Arquillian remote
adapter attaches to whatever server already answers on the configured http
port, so a leftover or foreign TomEE on 8080 silently absorbs the
deployments. For side-by-side runs, override the TomEE ports but keep Derby
on 1527: overriding `tck.derby.port` breaks the suites whose deployments use
the shared `tomee.xml` datasources on 1527 (cdi-ee's persistence-context
tests).

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
| Concurrency 3.1 | `jakarta.enterprise:jakarta.enterprise.concurrent-tck:3.1.1` | `concurrency` | **Passes 197 tests, 0 failures, 0 errors, 14 skipped (the TCK's own skips), signature test included, no exclusions** (2026-07-18). The runner sets `openejb.environment.default=true` (declares the `java:comp/Default*` resource-env-refs the TCK injects via `@Resource(lookup=…)`; without it they silently inject `null`), adds a `beans.xml` per WAR so OWB activates the TCK's `@Priority` `AsynchronousInterceptor` (both mirroring apache/tomee `tck/concurrency-standalone`), and stages the Derby engine jar for the tx tests' embedded `@DataSourceDefinition` |
| Data 1.0 | `jakarta.data:jakarta.data-tck:1.0.0` | `data` | **Runs: 99 tests, 7 failures, 29 errors** (2026-07-18), all in `standalone.entity.EntityTests`; the other six classes pass completely, signature test included. The runner's archive processor completes the TCK deployments the way a Jakarta Data runtime is expected to (adds the missing sibling classes, a `persistence.xml` on `java:comp/DefaultDataSource`, and a `beans.xml` with `bean-discovery-mode="all"` so `@Repository` interfaces are scanned and materialized — mirroring apache/tomee `tck/data-standalone`). The 36 failing methods are openejb-jakarta-data query-generation gaps (`ignoreCase` paths, JDQL literal/`NOT`/`OR`/parenthesis handling, cursored pagination, static-metamodel sorts) |
| RESTful Web Services 4.0 | EFTL zip (installed as `jakarta.ws.rs:jakarta-restful-ws-tck:4.0.1`) | `rest` | **Runs: 2,803 tests, 4 failures, 11 errors, 128 skipped** (2026-07-18, the skips are the TCK's own `@Disabled` tests) against TomEE's CXF, with the CXF client driving the client half of every test; the signature test passes. One error was a harness classpath gap (the CXF client needs `cxf-rt-rs-extension-providers` for spec-required JSON-B support) and is fixed in the runner; the remaining 14 failing tests are TomEE/CXF product results: deployment rejected for apps bundling `@ConstrainedTo(CLIENT)` providers (8), REST 3.1 `META-INF/services` Feature/DynamicFeature discovery missing (2), `getLength()` no-entity semantics (2), client `Response.hasEntity()` on entity-less responses (1), and 405-instead-of-404 matching (1) |
| Servlet 6.1 | EFTL zip (installed as `jakarta.tck:servlet-tck-runtime:6.1.0`) | `servlet` | **Runs: 1,706 tests, 69 errors, 7 skipped** (2026-07-18). The harness is complete: slf4j-simple resolves at the TCK-derived version, the TCK's bundled client certificate is trusted and mapped (both client-cert tests pass, including the https-targeted deployment via a metadata observer reporting the TLS port); remaining errors are behavioral differences (async dispatch connection handling, response-content mismatches) to triage as product results |
| Pages 4.0 | EFTL zip (installed as `jakarta.tck:jakarta-pages-tck:4.0.0`) | `pages` | **Passes 682/682 (0 failures, 0 errors, signature test included, no exclusions)** (2026-07-18). Needs the Central `jakarta.tck:common`/`signaturetest` support line at 11.1.1 with sigtest 2.6 (the TCK pom's own pins depend on the unpublished JavaTest harness), and the overlay removes TomEE's global UTF-8 default encodings from conf/web.xml because the TCK asserts the spec default ISO-8859-1. Its TomEE/Derby ports are overridable (`tomee.http.port`, `tomee.https.port`, `tomee.shutdown.port`, `tck.derby.port`) to run beside another harness instance |
| Validation 3.1 | EFTL zip (installed as `jakarta.validation:validation-tck-tests:3.1.1`) | `validation` | **Runs: 1,049 tests, 124 failures** (2026-07-18) after pinning AssertJ 3.7.0 (the published TCK jar is compiled against its covariant signatures). 118 of the remaining failures are Apache BVal `validation.xml`/constraint-mapping XML parsing gaps (`Unable to parse null`, `Failed to parse XML deployment descriptor file`) — provider work, not harness |
| WebSocket 2.2 | EFTL zip (installed as `jakarta.tck:websocket-tck-spec-tests:2.2.0`) | `websocket` | **Runs: 715 tests, 0 failures, 30 errors** (2026-07-18) against Tomcat's WebSocket implementation, driven through the `tomcat-websocket` client container; the signature test passes. 25 errors are the server-side negative-deployment classes: Tomcat halts the whole webapp deployment on an invalid endpoint (the spec-required halt, but Arquillian reports the failed deploy as an error before the probe runs). The remaining 5 are behavioral: the server configurator observes `permessage-deflate` in the requested/negotiated extension lists (3 tests) and two idle-timeout/close-code differences |
| Security 4.0 | Source reactor zip 4.0.1 | `security` (Maven module) | **Runs: 26 app modules plus the signature test, 132 tests, 5 failures, 2 errors at baseline; green with the reviewed exclusions (all 27 invoker projects pass, verified end-to-end 2026-07-18)**. The runner downloads and patches the reactor, injects a tomee-remote profile (including the `trustStore.path`/`trustStore.password` properties the OpenID modules' keytool steps read), and drives every module through the Maven invoker; the excluded failures (BASIC auth mechanism answering 401 for valid credentials, two OpenID modules) are TomEE Jakarta Security results to triage. The signature test passes against `org.apache.tomee:jakartaee-api`, the artifact whose `-tomcat` repackaging the distribution ships |
| Authentication 3.1 | Source reactor zip 3.1.2 | `authentication` (Maven module) | **Runs: 12 Web Profile modules plus the signature test; green with the reviewed exclusions (all 13 invoker projects pass, 0 failures, verified end-to-end 2026-07-18)**. The spi module's 50 excluded assertions are Tomcat AuthConfigFactory SPI conformance gaps — product results to triage. The signature test passes against `org.apache.tomcat:tomcat-jaspic-api` (the distribution's `lib/jaspic-api.jar`), with `tomcat-catalina` on the module classpath because Tomcat's `AuthConfigFactory` hard-references its default factory implementation. EJB/JACC/SOAP modules are outside the Web Profile scope |
| Faces 4.1 | Source reactor zip 4.1.2 | `faces` (Maven module) | **Runs: modern Arquillian modules — 263 tests on record, 9 failures + 30 errors** (2026-07-18) on Plume's Mojarra. The dominant product finding: TomEE's faces-config.xml unmarshaller rejects the `xsi:schemaLocation` attribute used by Faces 4.1 descriptors, failing those deployments (the 31 class exclusions in `exclusions/faces.txt`). The `faces-signaturetest` module runs in the same invoker pass and passes against the Mojarra artifact TomEE bundles (`faces.impl.version` in the runner pom tracks `lib/jakarta.faces-*.jar`) |
| Faces 4.1 (legacy old-tck) | Source reactor zip 4.1.2 (old-tck built from bundled sources) | `faces-old` (Maven module) | **Passes 5,391/5,391 (standalone mode, no exclusions)** (2026-07-18). The recorded full run shows 5 failures in `htmloutcometargetbutton`, all from a foreign server answering port 8080 mid-run; they pass on re-run against TomEE. The JavaTest half deploys through the TCK's own `tomcat` handler (WAR copy into `webapps/`) against a TomEE instance the runner provisions and starts itself; the first run builds the old-tck bundle from source (cached in `target/`). Ports overridable via `tomee.http.port`, `tomee.https.port`, `tomee.shutdown.port`, and `tck.harness.log.port`. Single test dir: `-Dtck.test=com/sun/ts/tests/jsf/...`; exclusions in `exclusions/faces-old.txt` (JavaTest jtx lines, appended to `ts.jtx`) |
| JSON Processing 2.1 | `jakarta.json:jakarta.json-tck-*:2.1.1` (Maven Central; byte-identical to the EFTL zip pinned in `environment/versions.env`) | `jsonp` | **Passes 197/197** (2026-07-18) against Apache Johnzon 2.1.0, the JSON-P provider bundled in the TomEE Plume snapshot: 179 functional + signature tests and 18 pluggability tests. The signature test checks the `jakarta.json` packages of the distribution's `jakartaee-api` jar |
| JSON Binding 3.0 | `jakarta.json.bind:jakarta.json.bind-tck:3.0.0` (Maven Central; byte-identical to the EFTL zip pinned in `environment/versions.env`) | `jsonb` | **Runs: 295 tests, 2 failures, 2 errors, 5 skipped** (2026-07-18) against Apache Johnzon 2.1.0 with OpenWebBeans as the CDI SE container (the runner boots it because the TCK's own private `@BeforeAll` bootstrap is ignored by JUnit). The runner sets `johnzon.use-bigdecimal-stringadapter=false`/`johnzon.use-biginteger-stringadapter=false` (Johnzon's TCK-compat switches, same as apache/tomee `tck/jsonb-standalone`) so BigDecimal/BigInteger serialize as JSON numbers per spec §3.4.1. The signature test passes; 2 failures are the TCK's pre-CLDR-34 French locale expectations (JDK data, not Johnzon), the other 2 are Johnzon 2.1.0 gaps: `JsonbDeserializer` instances not resolved through CDI, and `@JsonbDateFormat` ignored on a `@JsonbCreator` parameter during polymorphic deserialization |
| Debugging Support 2.0 | EFTL zip (installed as `jakartatck:jakarta-debugging-tck:2.0.0`) | `debugging` | **Passes** (2026-07-18). The runner compiles the TCK's `testclient.war` JSPs offline with the Jasper compiler bundled in the TomEE distribution (SMAP generation and dumping enabled, no server needed) and the TCK's `VerifySMAP` validates the embedded `SourceDebugExtension` of both generated classes plus both dumped `.smap` files. The Platform catalog's Pages debugging classes also pass |
| Enterprise Beans 4.0 Lite | Covered by the Platform TCK catalog (`runner-webprofile`, `ejb30`/`ejb32`) | — | See `runner-webprofile/KNOWN_FAILURES.md` |
| Standard Tag Library 3.0 | Covered by the Platform TCK catalog (`tags-tck`) | — | Blocked by the Jakarta Tags TLD registration gap |
| Expression Language 6.0 | EFTL zip (installed as `jakarta.tck:jakarta-expression-language-tck:6.0.1`, the jar's own embedded coordinates) | `el` | **Runs: 361 tests, 9 errors, signature test passing** (2026-07-18) against Tomcat's Jasper EL, the implementation every TomEE distribution bundles, in the local JVM. The 9 errors are the provider-level confirmation of the two Tomcat EL 6.0 gaps already known from the Platform catalog (MethodExpression overload selection, `StandardELContext` without a `VariableMapper`); with the reviewed exclusions in [el.txt](exclusions/el.txt) the suite runs green 352/352 |
| Transactions 2.0 | EFTL zip 2.0.1 (JavaTest harness, prebuilt servlet/jsp vehicle WARs) | `transactions` (Maven module) | **Runs: 49 tests, 40 pass, 9 fail; all three signature-test vehicles pass** (2026-07-18). The runner provisions TomEE, predeploys the vehicle WARs (the TCK's `none` deployment handler), and drives JavaTest in the web vehicles; the no-container `standalone` vehicle (RI-local JTA stack) stays out except for the signature test. The 9 failures are TomEE UserTransaction rollback/timeout semantics that leak state between requests, so [transactions.txt](exclusions/transactions.txt) excludes the three affected client files wholly (a failing test poisons the next one in the same vehicle). Not a Web Profile certification input; the Platform catalog covers the wider jta tree |
| Persistence 3.2 | EFTL zip 3.2.1 (installed as `jakarta.tck:persistence-tck-*`) | `persistence` | **Passes 2,135/2,135 (0 failures, 0 errors, 4 skipped, signature test included, no exclusions)** (2026-07-18) in the TCK's standalone (SE) vehicle against the EclipseLink build bundled in TomEE Plume, on the harness Derby with the TCK schema and stored procedures (`-Dtck.derby.port` overridable). Not a Web Profile certification input; the Platform catalog covers the EE integration half |

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
  the matching runner (`run-standalone-suite.sh` chains them). Their whole
  download/install pipeline lives inside the `standalone-tck` profile
  because the install-file goal has no skip parameter and would otherwise
  break the default reactor.
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
