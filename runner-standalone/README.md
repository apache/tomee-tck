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
`concurrency`, `data`, `servlet`, `pages`, `rest`, `websocket`,
`transactions`, `faces-old`, and `security-old` runners additionally accept
`-Dtomee.http.port`, `-Dtomee.https.port`, and `-Dtomee.shutdown.port`
(`transactions`, `faces-old`, and `security-old` also `-Dtck.harness.log.port`
for their JavaTest listeners), so they can run next to another harness
instance. The
`validation`, `cdi`, and `cdi-ee` runners parameterize only the HTTP and
shutdown ports (`-Dtomee.http.port`/`-Dtomee.shutdown.port`); their
arquillian.xml pins no `httpsPort`. The source-reactor runners security,
authentication, and faces still assume the fixed TomEE ports.
Overriding the ports matters beyond convenience: the Arquillian remote
adapter attaches to whatever server already answers on the configured http
port, so a leftover or foreign TomEE on 8080 silently absorbs the
deployments. `tck.derby.port` propagates into the staged `tomee.xml`
datasources, so a full side-by-side run overrides it together with the
TomEE ports.

The `rest` runner reserves port 8080 for the TCK instead of giving it to
TomEE. Its `SeBootstrapIT` tests boot their own embedded Jetty on the
SeBootstrap default port the spec mandates and cannot be pointed elsewhere,
so the runner selects TomEE's HTTP port around 8080 and leaves it free.

`TOMEE_CLASSIFIER` selects the distribution, default `plume`. The default
Maven build compiles these modules but runs nothing; execution requires
`-Dtck.standalone=true` (the script passes it).

The JavaTest-harness suites (`transactions`, `security-old`, `faces-old`)
report through the JT Harness work/report directories instead of surefire.
After each run (red or green) `run-standalone-suite.sh` converts the harness
text report into JUnit XML under the module's `target/surefire-reports/` via
[javatest-report-to-junit.sh](javatest-report-to-junit.sh), embedding each
failed test's `.jtr` harness log in its `<failure>` element, so CI's junit
ingestion shows per-test results and failure logs alongside the archived
HTML report.

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
| CDI 4.1 (core) + Interceptors 2.2 (non-EJB half) | `jakarta.enterprise:cdi-tck-core-impl:4.1.0` | `cdi` | **Runs: 1,388 tests, 81 failures, 82 skipped; mandatory CDI API signature test (`cdi-api-jdk17.sig`) passes** (2026-07-19). The failures cluster in the CDI 4.1 build-compatible-extensions feature (build-compatible extensions plus the Method Invokers the TCK registers through them) — OpenWebBeans 4.1 ships no `BuildServices` provider — to triage upstream. The runner carries the apache/tomee `tck/cdi-tomee` OpenWebBeans overlay (`strictDynamicValidation`, spec-default `defaultBeanDiscoveryMode=ANNOTATED`, TomEE-aware `Beans` porting SPI) shipped through the porting jar in `org.jboss.cdi.tck.libraryDirectory` |
| CDI 4.1 (EE integration) + Interceptors 2.2 (EJB half) | `jakarta.tck:cdi-tck-ee-impl:11.0.3` | `cdi-ee` | **Runs: 1,829 tests, 106 failures, 92 skipped** (2026-07-19, needs the 4 GB server heap configured in its arquillian.xml). Same OpenWebBeans 4.1 build-compatible-extensions gap as the core suite plus EE-integration issues; carries the same OpenWebBeans overlay and porting SPI |
| Concurrency 3.1 | `jakarta.enterprise:jakarta.enterprise.concurrent-tck:3.1.1` | `concurrency` | **Passes 197 tests, 0 failures, 0 errors, 14 skipped (the TCK's own skips), signature test included, no exclusions** (2026-07-18). The runner sets `openejb.environment.default=true` (declares the `java:comp/Default*` resource-env-refs the TCK injects via `@Resource(lookup=…)`; without it they silently inject `null`), adds a `beans.xml` per WAR so OWB activates the TCK's `@Priority` `AsynchronousInterceptor` (both mirroring apache/tomee `tck/concurrency-standalone`), and stages the Derby engine jar for the tx tests' embedded `@DataSourceDefinition` |
| Data 1.0 | `jakarta.data:jakarta.data-tck:1.0.1` | `data` | **Runs: 99 tests, 7 failures, 29 errors**, all in `standalone.entity.EntityTests`; the other six classes pass completely, signature test included. Runs the 1.0.1 certification service release, which gates keyword-support assertions on `jakarta.tck.database.type=RELATIONAL` (set on the container JVM via `catalina_opts`) and rewrites the enum-sort entity. The runner's archive processor completes the TCK deployments the way a Jakarta Data runtime is expected to (adds the missing sibling classes, a `persistence.xml` on `java:comp/DefaultDataSource`, and a `beans.xml` with `bean-discovery-mode="all"` so `@Repository` interfaces are scanned and materialized — mirroring apache/tomee `tck/data-standalone`); `java:comp/DefaultDataSource` is defined explicitly on embedded Derby in the module's `tomee.xml` so the persistence unit runs there rather than auto-linking to the network-Derby datasources. The 36 failing methods are openejb-jakarta-data query-generation gaps: cursored pagination (6), JDQL literal/`NOT`/`OR`/parenthesis translation (8), derived-query/`ignoreCase` compilation (6), static-metamodel and find-first resolution (4), sort ordering incl. the enum-ordinal sort (10), and update `@Query` methods (2) |
| RESTful Web Services 4.0 | EFTL zip (installed as `jakarta.ws.rs:jakarta-restful-ws-tck:4.0.1`) | `rest` | **Runs: 2,803 tests, 2 failures, 1 error, 128 skipped without exclusions** (2026-07-18, the skips are the TCK's own `@Disabled` tests) against TomEE's CXF, with the CXF client driving the client half of every test; the signature test passes. The runner sets `openejb.jaxrs.fail-on-constrainedto=false` in the server JVM (spec-required tolerance of bundled `@ConstrainedTo(CLIENT)` providers; TomEE's strict default rejects the deployment) and pins the CXF client to the `HttpURLConnection` conduit via `org.apache.cxf.transport.http.forceURLConnection=true` (the async conduit's no-entity handling — upstream CXF-9039 — otherwise fails the `getLength()`/`hasEntity()` assertions). The 3 failing tests are TomEE/CXF product results: REST 3.1 `META-INF/services` Feature/DynamicFeature discovery missing (2, TOMEE-4321/CXF-9005) and 405-instead-of-404 matching on a superclass-only `@Path` (1) |
| Servlet 6.1 | EFTL zip (installed as `jakarta.tck:servlet-tck-runtime:6.1.0`) | `servlet` | **Runs: 1,706 tests, 12 errors, 7 skipped** (2026-07-18). The harness is complete: slf4j-simple resolves at the TCK-derived version, the TCK's bundled client certificate is trusted and mapped (both client-cert tests pass, including the https-targeted deployment via a metadata observer reporting the TLS port), cross-context dispatch is enabled and the STRICT_SERVLET_COMPLIANCE subset (`alwaysAccessSession`, `contextGetResourceRequiresSlash`, `useRelativeRedirects`, session activity checks) applied through a `conf/context.xml` overlay, request trailers allowed on the HTTP connector, and the global `conf/web.xml` overlay drops TomEE's UTF-8 encoding pins and adds the `ja`->`Shift_Jis` locale mapping the spec tests expect. The remaining errors are two deployment behavioral differences: TomEE aborts context startup when a war references a servlet/filter class it does not package (`RegistrationTests`, `DefaultMappingTests`), excluded in [servlet.txt](exclusions/servlet.txt) |
| Pages 4.0 | EFTL zip (installed as `jakarta.tck:jakarta-pages-tck:4.0.0`) | `pages` | **Passes 682/682 (0 failures, 0 errors, signature test included, no exclusions)** (2026-07-18). Needs the Central `jakarta.tck:common`/`signaturetest` support line at 11.1.1 with sigtest 2.6 (the TCK pom's own pins depend on the unpublished JavaTest harness), and the overlay removes TomEE's global UTF-8 default encodings from conf/web.xml because the TCK asserts the spec default ISO-8859-1. Its TomEE/Derby ports are overridable (`tomee.http.port`, `tomee.https.port`, `tomee.shutdown.port`, `tck.derby.port`) to run beside another harness instance |
| Validation 3.1 | EFTL zip (installed as `jakarta.validation:validation-tck-tests:3.1.1`) | `validation` | **Runs: 1,049 tests, 0 failures, no exclusions; the mandatory API signature test runs and passes** against the `jakarta.validation` packages the Plume distribution ships. Needs AssertJ 3.7.0 pinned (the published TCK jar is compiled against its covariant signatures) and the JAXB RI pinned as `jakarta.xml.bind.JAXBContextFactory` in the server JVM (Plume ships both MOXy and the RI; BVal's descriptor parsing requires the RI) |
| WebSocket 2.2 | EFTL zip (installed as `jakarta.tck:websocket-tck-spec-tests:2.2.0`) | `websocket` | **Runs: 737 tests, 0 failures, 3 errors** (2026-07-18) against Tomcat's WebSocket implementation, driven through the `tomcat-websocket` client container; the signature test passes. The negative-deployment classes run and pass: an invalid server endpoint aborts the whole webapp deployment as the spec requires, and the runner's Arquillian extension (`NegativeDeploymentToleranceObserver`) tolerates the deployment failure so each client probe still runs. The 3 errors are one behavioral difference in a single class: the client container advertises its built-in `permessage-deflate` extension, which the server configurator then reports in the requested/negotiated lists the test does not expect |
| Security 4.0 | Source reactor zip 4.0.1 | `security` (Maven module) | **Runs: 26 app modules plus the signature test, 132 tests, 5 failures, 2 errors at baseline; green with the reviewed exclusions (all 27 invoker projects pass, verified end-to-end 2026-07-18)**. The runner downloads and patches the reactor, injects a tomee-remote profile (including the `trustStore.path`/`trustStore.password` properties the OpenID modules' keytool steps read), and drives every module through the Maven invoker; the excluded failures (BASIC auth mechanism answering 401 for valid credentials, two OpenID modules) are TomEE Jakarta Security results to triage. The signature test passes against `org.apache.tomee:jakartaee-api`, the artifact whose `-tomcat` repackaging the distribution ships. The runner drives the reactor's modern Arquillian app modules plus the signature test; the reactor's legacy JavaTest `old-tck` module is run by the separate `security-old` runner (row below) |
| Security 4.0 (legacy old-tck) | Source reactor zip 4.0.1 (old-tck built from bundled sources) | `security-old` (Maven module) | **Baseline 83 tests, 82 pass, 1 failure (Plume, JDK 21, 2026-07-19); green with the reviewed exclusion.** The legacy JavaTest half exercises ~65 `com.sun.ts.tests.securityapi` Client classes across the idstore (in-war, database, LDAP), ham (HTTP authentication mechanism) and securitycontext trees. It deploys through the TCK's own `tomcat` handler (WAR copy into `webapps/`) against a TomEE instance the runner provisions and starts itself, alongside a Derby network server (the `DatabaseIdentityStore` tree looks up `jdbc/securityAPIDB`, seeded by the harness' `init.derby`) and an embedded UnboundID LDAP server (started in-process by the harness on `ldap://localhost:11389`). The first run builds the old-tck bundle from source (cached in `target/`). The single failure is a TomEE product gap: `SecurityContext.hasAccessToWebResource()` returns `false` for an authorized caller (`securitycontext/callerdata`), excluded in `exclusions/security-old.txt`. Single test dir: `-Dtck.test=com/sun/ts/tests/securityapi/...` |
| Authentication 3.1 | Source reactor zip 3.1.2 | `authentication` (Maven module) | **Runs: 12 Web Profile modules plus the signature test; green with the reviewed exclusions (all 13 invoker projects pass, 0 failures, verified end-to-end 2026-07-18)**. The spi module registers the TCK's test `AuthConfigProvider` under Tomcat's JASPIC app-context naming (`Catalina/localhost /spitests_servlet_web`), so `ServletProfileSPITest` runs against Tomcat's `AuthConfigFactory` and passes 56 of its 57 assertions; the runner patches the GlassFish-style app-context-ids in `spi/common/ProviderConfiguration.xml` and pins the matching `logical.hostname.servlet` (jakartaee/authentication#220). The single exclusion, `ServletProfileSPITest#CheckMsgInfoKey`, is a TCK challenge (jakartaee/authentication#219) that hard-codes a Jakarta Authorization (JACC) requirement the Web Profile does not mandate. The signature test passes against `org.apache.tomcat:tomcat-jaspic-api` (the distribution's `lib/jaspic-api.jar`), with `tomcat-catalina` on the module classpath because Tomcat's `AuthConfigFactory` hard-references its default factory implementation. EJB/JACC/SOAP modules are outside the Web Profile scope |
| Faces 4.1 | Source reactor zip 4.1.2 | `faces` (Maven module) | **Runs: modern Arquillian modules (faces22/23/40/41) plus the Chrome/Selenium `old-tck-selenium` modules (ajax, commandLink, protectedViews) — 298 tests, 0 failures, 0 errors, 17 skipped** on Plume's Mojarra, with `exclusions/faces.txt` applied. The 10 excluded classes are one TomEE product gap (faces-config.xml unmarshaller rejecting the nonstandard *https* `xsi:schemaLocation`, `Spec1760IT`) and Mojarra/TomEE integration behaviors — Faces CDI implicit-object injection, `<f:validateWholeBean>`/cross-field and CDI method validation, and one Mojarra ajax re-init quirk under Chrome. The runner injects its tomee-remote profile into the failsafe execution (where the TCK runs every IT), pinning the `plume` classifier and staging ShrinkWrap API 1.2.6 via `tomee.additionalLibs` so the faces40/41 and old-tck-selenium WARs' bundled `util` deployment scans cleanly. `old-tck-selenium` runs its Chrome `BaseITNG` variant under `test.selenium=true` (the HtmlUnit variant is skipped) and needs no exclusions. The `faces-signaturetest` module runs in the same invoker pass and passes against the Mojarra artifact TomEE bundles (`faces.impl.version` in the runner pom tracks `lib/jakarta.faces-*.jar`) |
| Faces 4.1 (legacy old-tck) | Source reactor zip 4.1.2 (old-tck built from bundled sources) | `faces-old` (Maven module) | **Passes 5,391/5,391 (standalone mode, no exclusions)** (2026-07-18). The recorded full run shows 5 failures in `htmloutcometargetbutton`, all from a foreign server answering port 8080 mid-run; they pass on re-run against TomEE. The JavaTest half deploys through the TCK's own `tomcat` handler (WAR copy into `webapps/`) against a TomEE instance the runner provisions and starts itself; the first run builds the old-tck bundle from source (cached in `target/`). Ports overridable via `tomee.http.port`, `tomee.https.port`, `tomee.shutdown.port`, and `tck.harness.log.port`. Single test dir: `-Dtck.test=com/sun/ts/tests/jsf/...`; exclusions in `exclusions/faces-old.txt` (JavaTest jtx lines, appended to `ts.jtx`) |
| JSON Processing 2.1 | `jakarta.json:jakarta.json-tck-*:2.1.1` (Maven Central; byte-identical to the EFTL zip pinned in `environment/versions.env`) | `jsonp` | **Passes 197/197** (2026-07-18) against Apache Johnzon 2.1.0, the JSON-P provider bundled in the TomEE Plume snapshot: 179 functional + signature tests and 18 pluggability tests. The signature test checks the `jakarta.json` packages of the distribution's `jakartaee-api` jar |
| JSON Binding 3.0 | `jakarta.json.bind:jakarta.json.bind-tck:3.0.0` (Maven Central; byte-identical to the EFTL zip pinned in `environment/versions.env`) | `jsonb` | **Runs: 295 tests, 1 failure, 1 error, 5 skipped** against Apache Johnzon 2.1.0 with OpenWebBeans as the CDI SE container (the runner boots it because the TCK's own private `@BeforeAll` bootstrap is ignored by JUnit). The runner sets `johnzon.use-bigdecimal-stringadapter=false`/`johnzon.use-biginteger-stringadapter=false` (Johnzon's TCK-compat switches, same as apache/tomee `tck/jsonb-standalone`) so BigDecimal/BigInteger serialize as JSON numbers per spec §3.4.1, and passes `-Djava.locale.providers=COMPAT` so the JVM's pre-CLDR-34 locale data matches the TCK's French number-format expectations (U+00A0 grouping separator). The signature test passes; the 2 remaining non-passing tests are Johnzon 2.1.0 gaps: `JsonbDeserializer` instances not resolved through CDI, and `@JsonbDateFormat` ignored on a `@JsonbCreator` parameter during polymorphic deserialization |
| Debugging Support 2.0 | EFTL zip (installed as `jakartatck:jakarta-debugging-tck:2.0.0`) | `debugging` | **Passes** (2026-07-18). The runner compiles the TCK's `testclient.war` JSPs offline with the Jasper compiler bundled in the TomEE distribution (SMAP generation and dumping enabled, no server needed) and the TCK's `VerifySMAP` validates the embedded `SourceDebugExtension` of both generated classes plus both dumped `.smap` files. The Platform catalog's Pages debugging classes also pass |
| Enterprise Beans 4.0 Lite | Covered by the Platform TCK catalog (`runner-webprofile`, `ejb30`/`ejb32`) | — | See `runner-webprofile/KNOWN_FAILURES.md` |
| Standard Tag Library 3.0 | Covered by the Platform TCK catalog (`tags-tck`) | — | Blocked by the Jakarta Tags TLD registration gap |
| Expression Language 6.0 | EFTL zip (installed as `jakarta.tck:jakarta-expression-language-tck:6.0.1`, the jar's own embedded coordinates) | `el` | **Passes 361/361 (no exclusions)** (2026-07-18) against Tomcat's Jasper EL, the implementation every TomEE distribution bundles, in the local JVM: 360 functional tests plus the signature test. The runner supplies the vendor VariableMapper the TCK's `VarMapperELContext` instantiates through the `variable.mapper` porting property (`org.apache.el.lang.VariableMapperImpl`) in [el/pom.xml](el/pom.xml) |
| Transactions 2.0 | EFTL zip 2.0.1 (JavaTest harness, prebuilt servlet/jsp vehicle WARs) | `transactions` (Maven module) | **Runs: 49 tests, 40 pass, 9 fail; all three signature-test vehicles pass** (2026-07-19). The runner provisions TomEE, predeploys the vehicle WARs (the TCK's `none` deployment handler), and drives JavaTest in the web vehicles; the no-container `standalone` vehicle (RI-local JTA stack) stays out except for the signature test. The 9 failures are cross-request `UserTransaction` state leakage: a transaction left non-clean by one request poisons the next request on the same pooled Tomcat exec thread (leaker/victim pairs — the same test passes in one vehicle and fails in the other). Excluding only the failing ids shifts the victims to other tests in the same areas, so [transactions.txt](exclusions/transactions.txt) excludes the three affected areas (`rollback`, `setrollbackonly`, `settransactiontimeout`) whole for an order-stable green default run. In isolation on a fresh server `rollback` passes 10/10 and `settransactiontimeout` 4/4 (its 30s-sleep-then-`commit()` test passes — there is no commit-after-timeout gap), `setrollbackonly` 7/8. Not a Web Profile certification input; the Platform catalog covers the wider jta tree |
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
- Runners that need no server (annotations, di, el, jsonp, jsonb, debugging)
  set `tck.standalone.container.skip=true` and run in the local JVM; the
  provider-level suites (jsonp, jsonb) test the exact Johnzon version the
  TomEE distribution bundles, el drives Tomcat's Jasper EL directly, and
  debugging drives the distribution's own Jasper compiler offline.
- TestNG-based TCKs (cdi, cdi-ee, validation) force the surefire TestNG
  provider; JUnit-based ones use the JUnit Platform or JUnit 4 provider.
