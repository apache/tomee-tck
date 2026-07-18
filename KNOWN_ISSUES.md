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
| concurrency | 197 tests, 0 F + 0 E (14 TCK skips), signature passes | — | — |
| data | 99 tests, 7 F + 29 E | 36 methods (EntityTests only) | openejb-jakarta-data query generation |
| servlet | 1,706 tests, 12 E | 2 classes (12 methods) | TomEE aborts context startup on a missing referenced servlet/filter class |
| pages | 682/682 pass | — | — (needs the runner's spec-default encoding overlay) |
| rest | 2,803 tests, 2 F + 1 E | 3 tests | Feature/DynamicFeature `META-INF/services` discovery (TOMEE-4321/CXF-9005) + 405-vs-404 matching |
| validation | 1,049 tests, 0 F (incl. signature test) | — | — (server pins the JAXB RI; see product gaps) |
| cdi (core) | 1,388 run, 81 F (incl. signature test) | 54 methods + 27 deploy-failing classes | OpenWebBeans 4.1 build-compatible-extensions gap |
| cdi-ee | 1,829 run, 106 F | 79 methods + 27 deploy-failing classes | OpenWebBeans 4.1 build-compatible-extensions gap + EE integration |
| el | 361/361 pass (incl. signature test) | — | — |
| persistence | 2,135/2,135 pass (incl. signature test) | — | — (standalone/SE vehicle on Plume's EclipseLink) |
| transactions | 49 tests, 40 pass, 9 F (all 3 signature vehicles pass) | 22 test ids (3 client files) | TomEE UserTransaction rollback/timeout state leaks |
| jsonp | 197/197 pass (incl. pluggability + signature) | — | — |
| jsonb | 295 tests, 1 F + 1 E | 2 tests | 2 Johnzon 2.1.0 gaps |
| debugging | passes (4 SMAPs validated) | — | — |
| security | 132 tests, 5 F + 2 E; signature test passes | 7 tests | TomEE Jakarta Security |
| authentication | 106 tests, 1 F; signature test passes | 1 method (spi `CheckMsgInfoKey`) | TCK challenge #219 (hard-codes a JACC requirement) |
| websocket | 737 tests, 3 E | 3 methods | Client container advertises permessage-deflate in the negotiated extension lists |
| faces (modern modules) | 327 tests, 45 F + 6 E (failsafe reruns inflate the counts; 10 distinct failing classes) | 10 classes | 1 TomEE faces-config gap + Mojarra/TomEE CDI-injection, whole-bean/method validation, and one Chrome ajax quirk |
| faces-old (JavaTest) | 5,391 tests, all pass (recorded run: 5 F from a foreign server answering :8080 mid-run; pass on re-run) | — | — (standalone mode, no exclusions) |
| faces-signaturetest | passes against Plume's Mojarra (org.glassfish:jakarta.faces 4.1.9) | — | — |

## TomEE product gaps

Fixes belong in Apache TomEE (or Tomcat); each removes exclusion entries.

1. **Jakarta Data query generation (openejb-jakarta-data)** — repositories
   materialize and inject correctly; the gaps sit in the queries the
   provider generates for 36 `standalone.entity.EntityTests` methods:
   `ignoreCase` state-field paths that EclipseLink cannot resolve,
   literal/`NOT`/`OR`/parenthesis handling in `@Query` JDQL, empty/partial
   query generation, cursored pagination, and static-metamodel sorts. All
   entries in [data.txt](runner-standalone/exclusions/data.txt).
2. **Servlet 6.1 deployment strictness** — TomEE aborts the whole context
   startup when a war references a servlet or filter class it does not
   package: the pluggability `RegistrationTests` deployment declares filter
   `AddFilterString` and the spec `DefaultMappingTests` deployment declares
   servlet `TestServlet1` without bundling either class, so both contexts
   fail to start and every method in each class errors on the missing
   deployment URL. 2 class entries (12 methods) in
   [servlet.txt](runner-standalone/exclusions/servlet.txt).
4. **RESTful Web Services 4.0** — REST 3.1 `META-INF/services` discovery of
   `Feature`/`DynamicFeature` is not implemented (TOMEE-4321, CXF-9005), and
   a request to a path only matching the superclass `@Path` answers 405
   where the spec requires 404. 3 entries in
   [rest.txt](runner-standalone/exclusions/rest.txt). The runner sets
   `openejb.jaxrs.fail-on-constrainedto=false` in the server JVM — TomEE's
   strict default rejects deployments bundling a
   `@ConstrainedTo(RuntimeType.CLIENT)` provider where the spec requires
   ignoring it — and pins the CXF client to the `HttpURLConnection` conduit
   (`org.apache.cxf.transport.http.forceURLConnection=true`); the async
   conduit's no-entity handling, tracked upstream as
   [CXF-9039](https://issues.apache.org/jira/browse/CXF-9039), otherwise
   fails the `getLength()`/`hasEntity()` assertions.
5. **Faces (Mojarra on TomEE)** — five distinct integration behaviors, all in
   [faces.txt](runner-standalone/exclusions/faces.txt):
   - *faces-config parsing (product gap):* TomEE's `faces-config.xml`
     unmarshaller (`ReadDescriptors.readFacesConfig`) rejects the unexpected
     `{https://www.w3.org/2001/XMLSchema-instance}schemaLocation` attribute the
     Faces 4.1 descriptors declare on the nonstandard *https* XSI namespace, so
     the deployment fails to unmarshall. Only descriptors with child content
     hit the strict path: `faces41/headAndBodyRenderer/Spec1760IT` (its
     `faces-config.xml` carries a `<faces-config-extension>`) is the single
     class affected — the sibling `uiRepeat/Spec1263IT` and
     `uuidConverter/Spec1819IT` modules carry the same https namespace on an
     empty `<faces-config>` and deploy and pass. A TomEE fix and/or TCK
     challenge candidate.
   - *Faces CDI implicit-object injection:* `@Inject` with a Faces qualifier
     (`@RequestCookieMap`/`@SessionMap`/`@ViewMap`) yields a `Map` that renders
     non-empty where the TCK asserts the page contains `{}` — Mojarra's
     implicit-object CDI producers do not resolve to the empty map under
     OpenWebBeans (`Issue3729IT`/`Issue3730IT`/`Issue3731IT`,
     `Spec1582RequestCookieMap2IT`/`Spec1582ViewMap2IT`).
   - *Whole-bean / cross-field validation:* `<f:validateWholeBean>` does not
     emit the expected "Password fields must match" message
     (`multiFieldValidation/Spec1IT`, `validateWholeBean/Issue4083IT`).
   - *CDI method-level validation:* a `@FooConstraint` method constraint does
     not surface its violation message on the rendered page under TomEE's
     BVal/OpenWebBeans method-validation integration (`MethodValidationIT`).
   - *Mojarra ajax re-init under Chrome:* the ajax response re-executes an
     inline "Init called" script a different number of times than expected; the
     TCK source itself notes this appears under Chrome and passes under HtmlUnit
     (`Issue2162IT`).
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
12. **WebSocket 2.2 extension advertising (Tomcat)** — the server-side
    configurator reports the extensions the client requested and negotiated.
    TomEE's client-side WebSocket container (Tomcat's `tomcat-websocket`)
    always advertises its built-in `permessage-deflate` extension in the
    opening handshake, so it appears in the requested and negotiated lists
    the configurator observes while the test expects only the extensions it
    declared (3 methods in one class). The failure reproduces when the class
    runs on its own, so it is a Tomcat/TomEE client-container behavior to
    triage upstream. The negative-deployment classes (an invalid server
    endpoint aborts the whole webapp deployment, as the spec requires) run
    and pass: the runner's Arquillian extension tolerates the deployment
    failure so each client probe still runs.
    [websocket.txt](runner-standalone/exclusions/websocket.txt).
13. **Bean Validation XML config broken on stock Plume** — the Plume
    distribution ships EclipseLink MOXy (`eclipselink-5.0.1.jar`) and the JAXB
    RI (`jaxb-runtime-4.0.4.jar`) side by side. EclipseLink registers a
    `jakarta.xml.bind.JAXBContextFactory` service and wins ServiceLoader
    discovery, but MOXy rejects the namespace rewriting Apache BVal performs
    while parsing `validation.xml`/constraint-mapping descriptors, so every
    Bean Validation XML-configuration path fails on an out-of-the-box Plume
    server. The validation runner works around it by pinning the JAXB RI as the
    `jakarta.xml.bind.JAXBContextFactory` system property in the server JVM;
    TomEE should pin the factory itself so applications relying on Bean
    Validation XML config work on stock Plume.

## Upstream provider gaps

Need triage/fixes in the upstream projects TomEE ships.

1. **Apache OpenWebBeans 4.1 build-compatible extensions** — OpenWebBeans
   4.1 ships no `jakarta.enterprise.inject.build.compatible.spi.BuildServices`
   provider, so CDI 4.1 build-compatible extensions do not run. The CDI 4.1
   Method Invokers API itself works through portable extensions; the TCK's
   invoker tests fail only because they register their invokers through a
   build-compatible extension, which hits the same missing `BuildServices`
   gap. Deployments that only register a build-compatible extension fail
   configuration (the whole-class exclusion entries); tests that also assert
   runtime behavior fail their single method. Assorted
   observer/specialization/interceptor edge cases round out the lists. Drives
   the [cdi.txt](runner-standalone/exclusions/cdi.txt) and most of the
   [cdi-ee.txt](runner-standalone/exclusions/cdi-ee.txt) lists. The runner
   ships the apache/tomee `tck/cdi-tomee` OpenWebBeans overlay
   (`strictDynamicValidation`, the spec-default
   `defaultBeanDiscoveryMode=ANNOTATED`, and the TomEE-aware `Beans` porting
   SPI) and runs the mandatory CDI API signature check (`cdi-api-jdk17.sig`)
   in the core runner, which passes.
2. **Jakarta Authentication SPI (`ServletProfileSPITest`)** — the runner
   registers the TCK's test `AuthConfigProvider` under Tomcat's JASPIC
   app-context naming (`Catalina/localhost /spitests_servlet_web`, the value
   `getVirtualServerName() + " " + contextPath` yields), so Tomcat's
   `AuthConfigFactory` hands the request to the test SAM and 56 of the 57
   servlet-profile SPI assertions pass against Tomcat's implementation. The
   patch rewrites the GlassFish-style `server /...` app-context-ids in
   `spi/common/ProviderConfiguration.xml` and sets the matching
   `logical.hostname.servlet` for the client-side assertions
   (jakartaee/authentication#220). The one exclusion,
   `ServletProfileSPITest#CheckMsgInfoKey`, is a TCK challenge
   (jakartaee/authentication#219): it hard-codes a Jakarta Authorization
   (JACC) requirement — it expects the HttpServlet `MessageInfo` to carry the
   `jakarta.security.jacc.PolicyContext` key — which the Web Profile does not
   mandate. [authentication.txt](runner-standalone/exclusions/authentication.txt).
3. **Johnzon/CXF integration** — CDI injection into `@JsonbTypeDeserializer`
   fields, JSON-P scalar writers, Bean Validation interceptors, and CDI
   resource-class handling in the REST stack
   ([TOMEE-4436](https://issues.apache.org/jira/browse/TOMEE-4436),
   [TOMEE-4166](https://issues.apache.org/jira/browse/TOMEE-4166)).
4. **Apache Johnzon 2.1.0 (JSON Binding 3.0)** — confirmed at provider level
   by the standalone JSON-B TCK: `JsonbDeserializer` instances are not
   resolved through CDI, leaving `@Inject` fields null (the adapter and
   serializer CDI tests pass — the deserializer half of TOMEE-4436);
   `@JsonbDateFormat` on a `@JsonbCreator` constructor parameter is ignored
   during polymorphic (`@JsonbTypeInfo`) deserialization. These are the 2
   excluded tests in
   [jsonb.txt](runner-standalone/exclusions/jsonb.txt). The runner passes
   `-Djava.locale.providers=COMPAT` (same as Apache Johnzon's jakartaee TCK
   run) so the JVM uses the pre-CLDR-34 locale data the TCK's French
   number-format tests expect (U+00A0 as the grouping separator rather than
   the U+202F CLDR 34+/JDK 13+ emit). It also runs Johnzon with the
   spec-compat switches `johnzon.use-bigdecimal-stringadapter=false` /
   `johnzon.use-biginteger-stringadapter=false` (same as apache/tomee
   `tck/jsonb-standalone`), so BigDecimal/BigInteger serialize as the JSON
   numbers §3.4.1 requires instead of Johnzon's precision-preserving string
   default. JSON-P (johnzon-core) passes its TCK completely.
6. **OpenJPA** (webprofile classifier only) — 249 persistence classes fail;
   none reproduce on Plume/EclipseLink, tracked partly as
   [OPENJPA-2940](https://issues.apache.org/jira/browse/OPENJPA-2940).
   Kept visible via the `persistence-javatest (webprofile)` CI branch.

## Harness work remaining

Not product bugs — gaps in this repository's coverage.

- **Modern faces reactor not in CI**: the `faces` runner (modern Arquillian
  modules plus the signature test) runs locally via
  `run-standalone-suite.sh`; it joins the Jenkins branch list once its
  baseline and exclusion wiring have a verified green run. The `faces-old`
  JavaTest half runs in CI.
- **Faces `old-tck-selenium` module uncovered**: the extracted TCK reactor
  ships an `old-tck-selenium` module (22 tests) that neither the `faces`
  runner (which drives faces22/23/40/41 + faces-signaturetest) nor the
  `faces-old` JavaTest runner executes; wire it into one of them.

## What CI runs

`Jenkinsfile` stages: environment validation, smoke (JDK 17/21), the full
Platform catalog on Plume plus `persistence-javatest` on the webprofile ZIP,
and the standalone suites `annotations`, `di`, `el`, `concurrency`, `data`,
`servlet`, `pages`, `rest`, `validation`, `websocket`, `jsonp`, `jsonb`,
`debugging`, `persistence`, `transactions`, `cdi`, `cdi-ee`, `security`,
`authentication`, `faces-old` — all with default exclusions, all expected
green (a red JavaTest run fails the `faces-old` and `transactions` builds
through their `verify-tck-result` step). The `junit`/archive globs also
ingest the surefire/failsafe reports inside the extracted TCK reactors that
the source-reactor runners drive through the Maven invoker, plus the
JavaTest report directories. The modern `faces` reactor joins once its
baseline has a verified green run.
