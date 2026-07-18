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

## Status per required Web Profile 11 specification

Dates refer to runs against the TomEE Plume snapshot on Java 21.

| Specification | TCK source | Runner | Status |
|---|---|---|---|
| Annotations 3.0 | EFTL zip (installed as `jakartatck:jakarta-annotations-tck:3.0.0`) | `annotations` | **Passes** (signature test, 2026-07-18) |
| Dependency Injection 2.0 | EFTL zip (installed as `jakarta.inject:jakarta.inject-tck:2.0.2`) | `di` | **Passes 50/50** on OpenWebBeans via the CDI SE API (2026-07-18) |
| CDI 4.1 (core) + Interceptors 2.2 (non-EJB half) | `jakarta.enterprise:cdi-tck-core-impl:4.1.0` | `cdi` | **Runs: 1,388 tests, 90 failures, 82 skipped** (2026-07-18). The failures cluster in the new CDI 4.1 Method Invokers API (30) and build-compatible extensions (~25) — OpenWebBeans 4.1 feature gaps to triage upstream |
| CDI 4.1 (EE integration) + Interceptors 2.2 (EJB half) | `jakarta.tck:cdi-tck-ee-impl:11.0.3` | `cdi-ee` | **Runs: 1,829 tests, 117 failures, 92 skipped** (2026-07-18, needs the 4 GB server heap configured in its arquillian.xml) |
| Concurrency 3.1 | `jakarta.enterprise:jakarta.enterprise.concurrent-tck:3.1.1` | `concurrency` | **Runs: 187 tests, 92 pass, 46 failures, 49 errors, 14 skipped** (2026-07-18). Signature and virtual-thread tests pass; the failures are TomEE Concurrency 3.1 implementation gaps to triage |
| Data 1.0 | `jakarta.data:jakarta.data-tck:1.0.0` | `data` | Harness runs (99 tests); 95 errors from one root cause: repository interfaces (e.g. `MultipleEntityRepo`) are not resolvable in the deployed application — TomEE's Jakarta Data provider integration needs triage |
| Servlet 6.1 | EFTL zip (installed as `jakarta.tck:servlet-tck-runtime:6.1.0`) | `servlet` | First run 2026-07-18: 294 tests reached, 186 errors — dominated by `@OperateOnDeployment` URL lookups for the missing dedicated https container definition, plus the client-cert WARs that need a TomEE vendor descriptor (the reference runner injects sun-web.xml through an Arquillian archive processor). Harness work remains before results are meaningful |
| Validation 3.1 | EFTL zip (installed as `jakarta.validation:validation-tck-tests:3.1.1`) | `validation` | First run 2026-07-18: 1,049 tests, 587 failures — largely harness issues: an AssertJ version conflict inside deployments (`NoSuchMethodError: ListAssert.containsExactlyInAnyOrder`, 22×) and `ValidationException: Unable to parse null` / `DOMException NAMESPACE_ERR` around `validation.xml` handling (128×) need triage before the genuine Apache BVal gaps are visible |
| Security 4.0 | Source reactor zip 4.0.1 | `security/run-security-tck.sh` | Download scaffold. apache/tomee `tck/security-standalone` already automates a TomEE profile for 4.0.0 and is the porting template |
| Authentication 3.1 | Source reactor zip 3.1.2 | `authentication/run-authentication-tck.sh` | Download scaffold; the TCK's own tomcat-remote profile is the template. No existing TomEE runner |
| Faces 4.1 | Source reactor zip 4.1.2 | `faces/run-faces-tck.sh` | Download scaffold. Modern Arquillian modules (~319 tests) portable via the shipped tomcat profiles; the ~5,500-test legacy JavaTest `old-tck` half is GlassFish-wired and is the largest remaining porting effort |
| Enterprise Beans 4.0 Lite | Covered by the Platform TCK catalog (`runner-webprofile`, `ejb30`/`ejb32`) | — | See `runner-webprofile/KNOWN_FAILURES.md` |
| Standard Tag Library 3.0 | Covered by the Platform TCK catalog (`tags-tck`) | — | Blocked by the Jakarta Tags TLD registration gap |
| Debugging Support 2.0 | Covered by the Platform TCK catalog (Pages debugging classes) | — | Passing |
| Expression Language 6.0 | Standalone EL TCK (Maven Central `jakarta.el:...`-tck) | not yet scaffolded | Platform EL integration already runs in `runner-webprofile` |
| JSON Processing 2.1 / JSON Binding 3.0 | Standalone TCKs on Maven Central | not yet scaffolded | Platform integration already runs in `runner-webprofile` |
| Pages 4.0 / WebSocket 2.2 / REST 4.0 / Transactions 2.0 / Persistence 3.2 | Standalone TCKs (zip or Maven Central) | not yet scaffolded | Platform integration already runs in `runner-webprofile` |

## Layout conventions

- `<id>-install` modules download an EFTL distribution zip (SHA-256 pinned)
  and install its artifacts into the local repository; run them once before
  the matching runner (`run-standalone-suite.sh` chains them).
- Container-based runners inherit the TomEE overlay, Derby, and certificate
  lifecycle from the shared parent; `src/tomee-conf/` files replace
  same-named files from `environment/tomee/conf`.
- Signature-only runners (annotations, di) set
  `tck.standalone.container.skip=true` and run in the local JVM.
- TestNG-based TCKs (cdi, cdi-ee, validation) force the surefire TestNG
  provider; JUnit-based ones use the JUnit Platform or JUnit 4 provider.
