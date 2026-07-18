# Known issues

Central triage list for everything in this harness that is currently broken
and needs attention. Snapshot date: **2026-07-18**, TomEE 11.0.0-SNAPSHOT
(Plume unless noted), Java 21. Every failing test named here is excluded from
the default runs by a reviewed exclusion list, so the suites and CI stay
green and a red run is a regression — the exclusions do **not** turn any
result into a compatibility pass.

Detail lives next to each runner:

- Platform Web Profile catalog: [runner-webprofile/KNOWN_FAILURES.md](runner-webprofile/KNOWN_FAILURES.md)
  and [runner-webprofile/exclusions/](runner-webprofile/exclusions/)
- Standalone specification TCKs: [runner-standalone/exclusions/](runner-standalone/exclusions/README.md)
  with per-suite lists, and the status table in
  [runner-standalone/README.md](runner-standalone/README.md)

## Suite scoreboard (2026-07-18 baseline)

| Suite | Baseline result | Excluded | Root cause |
|---|---|---|---|
| Platform catalog (Plume) | all retained partitions pass | reviewed lists per partition | see [KNOWN_FAILURES.md](runner-webprofile/KNOWN_FAILURES.md) |
| Platform `persistence-javatest` (webprofile ZIP) | 201/450 classes retained | 249 classes | OpenJPA gaps; none reproduce on Plume/EclipseLink |
| annotations | passes | — | — |
| di | 50/50 pass | — | — |
| concurrency | 197 tests, 0 F + 0 E (14 TCK skips, signature passes) | — | — (the previous 94 exclusions were harness misconfiguration, fixed in the runner; see [concurrency.txt](runner-standalone/exclusions/concurrency.txt)) |
| data | 99 tests, 7 F + 29 E | 36 methods (EntityTests only) | openejb-jakarta-data query generation (the previous 95 blanket exclusions were harness misconfiguration) |
| servlet | 1,706 tests, 69 E | 69 tests | TomEE/Tomcat behavioral diffs |
| pages | 682/682 pass | — | — (needs the runner's spec-default encoding overlay) |
| rest | 2,803 tests, 4 F + 11 E | 14 tests | TomEE/CXF gaps (1 error was a fixed harness classpath gap) |
| validation | 1,049 tests, 124 F | 124 tests | Apache BVal gaps |
| cdi (core) | 1,388 run, 90 F | 63 methods + 27 deploy-failing classes | OpenWebBeans 4.1 gaps |
| cdi-ee | 1,829 run, 117 F | 87 methods + 30 deploy-failing classes | OpenWebBeans 4.1 + EE integration |
| el | 361 tests, 9 E | 9 tests | Tomcat EL 6.0 gaps (provider-level confirmation of the Platform findings) |
| persistence | 2,135/2,135 pass (incl. signature test) | — | — (standalone/SE vehicle on Plume's EclipseLink) |
| transactions | 49 tests, 40 pass, 9 F (all 3 signature vehicles pass) | 22 test ids (3 client files) | TomEE UserTransaction rollback/timeout state leaks |
| jsonp | 197/197 pass (incl. pluggability + signature) | — | — |
| jsonb | 295 tests, 2 F + 2 E | 4 tests | 2 Johnzon 2.1.0 gaps + 2 TCK pre-CLDR-34 locale expectations (BigDecimal/BigInteger now pass via Johnzon's spec-compat switches) |
| debugging | passes (4 SMAPs validated) | — | — |
| security | 132 tests, 5 F + 2 E; signature test passes | 7 tests | TomEE Jakarta Security |
| authentication | 105 tests, 50 F; signature test passes | 50 methods (spi) | Tomcat AuthConfigFactory SPI |
| websocket | 715 tests, 30 E | 25 classes + 5 methods | Tomcat halts webapp deploy on invalid endpoints; extension/timeout behavior |
| faces (modern modules) | 263 tests on record, 9 F + 30 E | 39 tests | TomEE faces-config parsing + Mojarra integration |

## TomEE product gaps

Fixes belong in Apache TomEE (or Tomcat); each removes exclusion entries.

1. **Jakarta Data query generation (openejb-jakarta-data)** — repositories
   materialize and inject fine once the deployment carries the
   `persistence.xml`/`beans.xml` the Data runtime is expected to provide
   (the earlier "every repository injection is null" finding was harness
   misconfiguration, fixed in the runner). What remains is 36
   `standalone.entity.EntityTests` methods: `ignoreCase` state-field paths
   that EclipseLink cannot resolve, literal/`NOT`/`OR`/parenthesis handling
   in `@Query` JDQL, empty/partial query generation, cursored pagination,
   and static-metamodel sorts. All entries in
   [data.txt](runner-standalone/exclusions/data.txt).
2. **Servlet 6.1 behavioral differences** — async dispatch connection
   handling (`DispatchTests` and async-context classes) and a set of
   response-content mismatches. 69 entries in
   [servlet.txt](runner-standalone/exclusions/servlet.txt).
4. **RESTful Web Services 4.0** — TomEE refuses to deploy an application
   bundling a `@ConstrainedTo(RuntimeType.CLIENT)` provider
   (`IllegalArgumentException: ... is not a SERVER provider` from
   `TomeeJaxRsService`) where the spec requires ignoring it; REST 3.1
   `META-INF/services` discovery of `Feature`/`DynamicFeature` is not
   implemented; `ContainerRequestContext.getLength()`/`HttpHeaders
   .getLength()` no-entity semantics differ; the CXF client answers
   `hasEntity() == true` for entity-less responses; and an unmatched
   subresource path answers 405 where 404 is required. 14 entries in
   [rest.txt](runner-standalone/exclusions/rest.txt).
5. **Faces descriptor parsing** — TomEE's `faces-config.xml` unmarshaller
   rejects the `xsi:schemaLocation` attribute that Faces 4.1 descriptors
   carry, so affected applications never deploy. Dominant cause of the 31
   class exclusions in [faces.txt](runner-standalone/exclusions/faces.txt).
6. **Jakarta Security** — the BASIC mechanism answers 401 for valid
   credentials in the decorated/custom-handler variants, and both OpenID
   default modules fail token validation.
   [security.txt](runner-standalone/exclusions/security.txt).
7. **Persistence integration** — undeploy calls `close()` on an
   already-closed `EntityManagerFactory` (fails the
   `entityManagerFactoryCloseExceptions` vehicles), and the Jakarta
   Persistence 3.2 CDI qualifier beans (`EntityManagerFactory`/
   `EntityManager` etc. from `persistence.xml`) are not registered
   (`ServletEMLookupTest`). Affects Plume and webprofile alike.
8. **Jakarta Tags TLD registration** — the `jakarta.tags.*` URIs of the
   replacement Jakarta Tags 3.0 jar are not exposed to applications; all 50
   Tags classes plus the EJB-Lite JSP vehicles fail as collateral.
9. **Transactions** — CDI `@Transactional` interceptors fail propagation,
   rollback-rule, and `TransactionScoped` assertions; `UserTransaction`
   rollback/timeout semantics leak state between requests. Confirmed by the
   standalone Transactions 2.0 TCK web vehicles: `commit()` does not throw
   after `setTransactionTimeout` expiry, and rollback/`setRollbackOnly`
   `IllegalStateException` semantics poison the following request — 22
   entries in
   [transactions.txt](runner-standalone/exclusions/transactions.txt).
10. **Enterprise Beans** — timer callbacks expose incomplete/not-retried
   transactions, `java:comp` is mutable where the spec requires
   `OperationNotSupportedException`, and failed CDI/EJB deployments leak
   deployment IDs (`DuplicateDeploymentIdException` in later apps).
11. **webprofile ZIP signature leak** — the combined `jakartaee-api` jar
    exposes Jakarta Batch and Messaging packages although `javaee.level=web`
    does not declare them; strip them or declare and certify them.
12. **WebSocket 2.2 behavior (Tomcat)** — a WAR containing an invalid
    server endpoint fails the whole webapp deployment (the spec-required
    deployment halt, but the TCK's Arquillian harness reports the failed
    deploy as an error; 25 negative-deployment classes), the server-side
    configurator observes Tomcat's built-in `permessage-deflate` in the
    requested/negotiated extension lists (3 tests), and two idle-timeout/
    close-code assertions differ.
    [websocket.txt](runner-standalone/exclusions/websocket.txt).

## Upstream provider gaps

Need triage/fixes in the upstream projects TomEE ships.

1. **Apache OpenWebBeans 4.1** — the CDI 4.1 Method Invokers API (~30
   tests) and build-compatible extensions (~25 deploy-failing classes) are
   not implemented; assorted observer/interceptor edge cases. Drives the
   [cdi.txt](runner-standalone/exclusions/cdi.txt) and most of the
   [cdi-ee.txt](runner-standalone/exclusions/cdi-ee.txt) lists.
2. **Apache BVal** — `validation.xml`/constraint-mapping XML parsing fails
   ("Unable to parse null", "Failed to parse XML deployment descriptor
   file"), 118 of the 124 validation failures; the rest are
   method-validation/metadata gaps.
   [validation.txt](runner-standalone/exclusions/validation.txt).
3. **Tomcat Jakarta Authentication SPI** — `ServletProfileSPITest` fails 50
   of 57 AuthConfigFactory/ServerAuthConfig conformance assertions.
   [authentication.txt](runner-standalone/exclusions/authentication.txt).
4. **Tomcat EL 6.0** — `MethodExpression` overload selection and the missing
   `StandardELContext` `VariableMapper` (webprofile `expression-language`
   partition). Confirmed at provider level by the standalone EL 6.0 TCK:
   the same two gaps account for all 9 errors in
   [el.txt](runner-standalone/exclusions/el.txt); the other 352 tests and
   the signature test pass.
5. **Johnzon/CXF integration** — CDI injection into `@JsonbTypeDeserializer`
   fields, JSON-P scalar writers, Bean Validation interceptors, and CDI
   resource-class handling in the REST stack
   ([TOMEE-4436](https://issues.apache.org/jira/browse/TOMEE-4436),
   [TOMEE-4166](https://issues.apache.org/jira/browse/TOMEE-4166)).
6. **Apache Johnzon 2.1.0 (JSON Binding 3.0)** — confirmed at provider level
   by the standalone JSON-B TCK: `JsonbDeserializer` instances are not
   resolved through CDI, leaving `@Inject` fields null (the adapter and
   serializer CDI tests pass — the deserializer half of TOMEE-4436);
   `@JsonbDateFormat` on a `@JsonbCreator` constructor parameter is ignored
   during polymorphic (`@JsonbTypeInfo`) deserialization. 2 excluded tests
   in [jsonb.txt](runner-standalone/exclusions/jsonb.txt); the other 2
   entries there are the TCK's pre-CLDR-34 French locale expectations, a
   JDK-data mismatch rather than a Johnzon defect. Johnzon's
   BigDecimal/BigInteger-as-string default is not a defect: the runner sets
   the spec-compat switches `johnzon.use-bigdecimal-stringadapter=false` /
   `johnzon.use-biginteger-stringadapter=false` (same as apache/tomee
   `tck/jsonb-standalone`) and both mapping tests pass. JSON-P
   (johnzon-core) passes its TCK completely.
7. **OpenJPA** (webprofile classifier only) — 249 persistence classes fail;
   none reproduce on Plume/EclipseLink, tracked partly as
   [OPENJPA-2940](https://issues.apache.org/jira/browse/OPENJPA-2940).
   Kept visible via the `persistence-javatest (webprofile)` CI branch.

## Harness work remaining

Not product bugs — gaps in this repository's coverage.

- **Faces legacy halves**: the ~5,500-test JavaTest `old-tck` reactor is
  GlassFish-wired (asadmin deployment) and unported; `faces-signaturetest`
  is GlassFish-bound too. The faces baseline (263 tests on record) covers
  only the modern Arquillian modules.
- **Faces reactors not in CI**: the faces and faces-old runners run locally
  via `run-standalone-suite.sh`; they join the Jenkins branch list once
  their baseline and exclusion wiring have a verified green run.
- **Exclusion lists need a confirming run**: all lists were generated from
  the 2026-07-18 surefire/TestNG reports. The mechanisms (surefire
  `excludesFile`, TestNG annotation transformer) are verified, but only a
  full green run of each suite confirms no order-dependent or flaky
  failures remain — the servlet async-dispatch area is the likeliest
  candidate for flakiness. Confirmed green with default exclusions so far:
  `annotations`, `di`, `el`, `concurrency` (no exclusions), `data`,
  `servlet` (1,637 run, 0 failures — the async-dispatch area held),
  `pages` (no exclusions), `rest`, `validation` (925 run after the
  exclusion listener learned to match inherited test methods by their
  concrete class), `cdi` (1,216 run), `websocket`, `jsonp` (no
  exclusions), `jsonb`, `debugging`, `persistence` (no exclusions), and
  `transactions` (2026-07-18).

## What CI runs

`Jenkinsfile` stages: environment validation, smoke (JDK 17/21), the full
Platform catalog on Plume plus `persistence-javatest` on the webprofile ZIP,
and the standalone suites `annotations`, `di`, `el`, `concurrency`, `data`,
`servlet`, `pages`, `rest`, `validation`, `websocket`, `jsonp`, `jsonb`,
`debugging`, `persistence`, `transactions`, `cdi`, `cdi-ee`, `security`,
`authentication` — all with default exclusions, all expected green. The
`junit`/archive globs also ingest the surefire/failsafe reports inside the
extracted TCK reactors that the source-reactor runners drive through the
Maven invoker, plus the JavaTest report directories. The faces reactors
join once their baseline has a verified green run.
