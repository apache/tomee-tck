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
| concurrency | 187 tests, 45 F + 49 E | 94 tests | TomEE Concurrency 3.1 gaps |
| data | 99 tests, 73 F + 22 E | 95 tests (5 classes) | TomEE Jakarta Data provider |
| servlet | 1,706 tests, 69 E | 69 tests | TomEE/Tomcat behavioral diffs |
| validation | 1,049 tests, 124 F | 124 tests | Apache BVal gaps |
| cdi (core) | 1,388 run, 90 F | 63 methods + 27 deploy-failing classes | OpenWebBeans 4.1 gaps |
| cdi-ee | 1,829 run, 117 F | 87 methods + 30 deploy-failing classes | OpenWebBeans 4.1 + EE integration |
| security | 132 tests, 5 F + 2 E | 7 tests | TomEE Jakarta Security |
| authentication | 105 tests, 50 F | 50 methods (spi) | Tomcat AuthConfigFactory SPI |
| websocket | 715 tests, 30 E | 25 classes + 5 methods | Tomcat halts webapp deploy on invalid endpoints; extension/timeout behavior |
| faces (modern modules) | 263 tests on record, 9 F + 30 E | 39 tests | TomEE faces-config parsing + Mojarra integration |

## TomEE product gaps

Fixes belong in Apache TomEE (or Tomcat); each removes exclusion entries.

1. **Concurrency 3.1 implementation** — resources declared with
   `@ManagedExecutorDefinition`, `@ManagedScheduledExecutorDefinition`,
   `@ManagedThreadFactoryDefinition`, and `@ContextServiceDefinition` are
   injected as `null`, context propagation to unmanaged threads is
   incomplete, and ContextService/transaction interplay differs from spec.
   94 excluded tests in [concurrency.txt](runner-standalone/exclusions/concurrency.txt).
2. **Jakarta Data provider** — repository interfaces are never materialized
   inside deployments, so every repository injection is `null`. One fix
   should clear essentially all 95 entries in
   [data.txt](runner-standalone/exclusions/data.txt).
3. **Servlet 6.1 behavioral differences** — async dispatch connection
   handling (`DispatchTests` and async-context classes) and a set of
   response-content mismatches. 69 entries in
   [servlet.txt](runner-standalone/exclusions/servlet.txt).
4. **Faces descriptor parsing** — TomEE's `faces-config.xml` unmarshaller
   rejects the `xsi:schemaLocation` attribute that Faces 4.1 descriptors
   carry, so affected applications never deploy. Dominant cause of the 31
   class exclusions in [faces.txt](runner-standalone/exclusions/faces.txt).
5. **Jakarta Security** — the BASIC mechanism answers 401 for valid
   credentials in the decorated/custom-handler variants, and both OpenID
   default modules fail token validation.
   [security.txt](runner-standalone/exclusions/security.txt).
6. **Persistence integration** — undeploy calls `close()` on an
   already-closed `EntityManagerFactory` (fails the
   `entityManagerFactoryCloseExceptions` vehicles), and the Jakarta
   Persistence 3.2 CDI qualifier beans (`EntityManagerFactory`/
   `EntityManager` etc. from `persistence.xml`) are not registered
   (`ServletEMLookupTest`). Affects Plume and webprofile alike.
7. **Jakarta Tags TLD registration** — the `jakarta.tags.*` URIs of the
   replacement Jakarta Tags 3.0 jar are not exposed to applications; all 50
   Tags classes plus the EJB-Lite JSP vehicles fail as collateral.
8. **Transactions** — CDI `@Transactional` interceptors fail propagation,
   rollback-rule, and `TransactionScoped` assertions; `UserTransaction`
   rollback/timeout semantics leak state between requests.
9. **Enterprise Beans** — timer callbacks expose incomplete/not-retried
   transactions, `java:comp` is mutable where the spec requires
   `OperationNotSupportedException`, and failed CDI/EJB deployments leak
   deployment IDs (`DuplicateDeploymentIdException` in later apps).
10. **webprofile ZIP signature leak** — the combined `jakartaee-api` jar
    exposes Jakarta Batch and Messaging packages although `javaee.level=web`
    does not declare them; strip them or declare and certify them.
11. **WebSocket 2.2 behavior (Tomcat)** — a WAR containing an invalid
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
   partition).
5. **Johnzon/CXF integration** — CDI injection into `@JsonbTypeDeserializer`
   fields, JSON-P scalar writers, Bean Validation interceptors, and CDI
   resource-class handling in the REST stack
   ([TOMEE-4436](https://issues.apache.org/jira/browse/TOMEE-4436),
   [TOMEE-4166](https://issues.apache.org/jira/browse/TOMEE-4166)).
6. **OpenJPA** (webprofile classifier only) — 249 persistence classes fail;
   none reproduce on Plume/EclipseLink, tracked partly as
   [OPENJPA-2940](https://issues.apache.org/jira/browse/OPENJPA-2940).
   Kept visible via the `persistence-javatest (webprofile)` CI branch.

## Harness work remaining

Not product bugs — gaps in this repository's coverage.

- **Faces legacy halves**: the ~5,500-test JavaTest `old-tck` reactor is
  GlassFish-wired (asadmin deployment) and unported; `faces-signaturetest`
  is GlassFish-bound too. The faces baseline (263 tests on record) covers
  only the modern Arquillian modules.
- **Signature modules disabled** in the security and authentication source
  reactors (GlassFish-only coordinates).
- **Standalone TCKs not yet scaffolded**: Expression Language, JSON
  Processing, JSON Binding, Pages, REST, Transactions, Persistence — the
  Platform catalog covers their integration halves only.
- **Source-reactor suites not in CI**: security, authentication, and faces
  run locally via `run-standalone-suite.sh`; their exclusion wiring
  (invoker-passed `excludesFile`) still needs a verified end-to-end run, and
  the Jenkins `junit`/archive globs do not yet ingest the inner-reactor
  report paths.
- **Exclusion lists need a confirming run**: all lists were generated from
  the 2026-07-18 surefire/TestNG reports. The mechanisms (surefire
  `excludesFile`, TestNG annotation transformer) are verified, but only a
  full green run of each suite confirms no order-dependent or flaky
  failures remain — the servlet async-dispatch area is the likeliest
  candidate for flakiness.

## What CI runs

`Jenkinsfile` stages: environment validation, smoke (JDK 17/21), the full
Platform catalog on Plume plus `persistence-javatest` on the webprofile ZIP,
and the standalone suites `annotations`, `di`, `concurrency`, `data`,
`servlet`, `validation`, `cdi`, `cdi-ee` — all with default exclusions, all
expected green. Source-reactor suites join once their exclusion wiring has a
verified run.
