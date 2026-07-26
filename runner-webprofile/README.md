# Jakarta EE 11 Web Profile runner catalog

This module is the explicit entry point for Jakarta EE 11 Web Profile
certification work. The default build runs no TCK tests. Run every modern
Platform test selected for Web Profile with:

```shell
runner-webprofile/run-platform-suite.sh servlet
runner-webprofile/run-platform-suite.sh javatest
```

Pass a manifest ID as the second argument to run or resume one partition, for
example `runner-webprofile/run-platform-suite.sh servlet rest`.

The EclipseLink-based TomEE Plume distribution is the default target under
test; 448 of the 450 Jakarta Persistence javatest classes pass there. Set
`TOMEE_CLASSIFIER=webprofile` to test the OpenJPA-based `webprofile` ZIP
instead. The script then merges the per-partition overrides from
`platform-suite-webprofile.tsv` over the base manifest and prefers
partition exclusions from `exclusions/webprofile/` where they exist — the
OpenJPA provider blocks 249 of the 450 persistence classes there:

```shell
TOMEE_CLASSIFIER=webprofile runner-webprofile/run-platform-suite.sh javatest persistence-javatest
```

`TOMEE_CLASSIFIER` also accepts `microprofile` and `plus`; the script rejects
any other value. Those two carry no manifest overrides or exclusion
subdirectory, so they run against the `plume` expectations and their failures
have to be read against that distribution's own scope.

`TOMEE_VERSION` selects the TomEE build to test — typically one
`environment/tomee/build-tomee.sh` produced from a tag or branch. Unset, the
pom's snapshot version applies:

```shell
TOMEE_VERSION=11.0.0-SNAPSHOT runner-webprofile/run-platform-suite.sh servlet rest
```

For diagnosis, run one partition and optionally one class directly:

```shell
./mvnw -pl runner-webprofile/run -am \
  -Dtck.artifact=rest-platform-tck \
  -Dtck.partition=rest \
  -Dtck.groups='web & !tck-javatest' \
  -Dtck.test=com.sun.ts.tests.jaxrs.platform.servletconfig.JAXRSClientIT \
  verify
```

The artifact version is inherited from `jakarta.tck:artifacts-bom`, currently
`11.0.3`. Each manifest partition runs in a separate Maven invocation. This
isolates classpaths, TomEE instances, port use, exclusions, and reports.

## Platform integration artifacts

| Published artifact scanned | Web Profile responsibility |
|---|---|
| `jakarta.tck:jdbc-platform-tck` | JDBC integration |
| `jakarta.tck:ejb30`, `jakarta.tck:ejb32` | Enterprise Beans Lite integration |
| `jakarta.tck:rest-platform-tck` | REST integration beyond the standalone REST TCK |
| `jakarta.tck:el-platform-tck` | Expression Language Platform integration |
| `jakarta.tck:jsonb-platform-tck` | JSON Binding Platform integration |
| `jakarta.tck:jsonp-platform-tck` | JSON Processing Platform integration |
| `jakarta.tck:pages-platform-tck` | Pages and debugging-language integration |
| `jakarta.tck:persistence-platform-tck-tests` | Persistence Platform integration |
| `jakarta.tck:tags-tck` | Standard Tag Library tests published with the Platform TCK |
| `jakarta.tck:transactions-tck` | Transactions Platform integration |
| `jakarta.tck:websocket-tck-platform-tests` | WebSocket Platform integration |
| `jakarta.tck:signaturevalidation` | Web Profile API signatures |

The checked-in [`platform-suite.tsv`](platform-suite.tsv) is the suite
definition. It records every artifact/protocol partition selected by the
official `web` JUnit tag and the number of tagged test classes in the Jakarta
Platform TCK `11.0.3` artifacts. The script verifies those expected counts
against the generated reports, making an upstream scope or selection change a
hard failure.

Source and post-exclusion counts are separate manifest columns. Every reduction
must be explained in [`KNOWN_FAILURES.md`](KNOWN_FAILURES.md) and in the matching
file under `exclusions/`.

The manifest contains 1,132 Platform TCK classes. After the reviewed TomEE
compatibility exclusions, 920 classes remain enabled on the default Plume
distribution (673 on the OpenJPA-based `webprofile` distribution, whose
provider blocks most of the persistence catalog). The counts were re-derived independently from the published
11.0.3 jars by scanning class-level (and inherited) JUnit `@Tag` annotations;
every artifact in `jakarta.tck:artifacts-bom` carrying classes in the strict
`web` group is represented here. The only intentionally unlisted `web`-tagged
class is `com.sun.ts.tests.jta.ee.transactional.EJBLiteJSPTag`, a server-side
JSP `SimpleTag` helper that inherits the tag from its JSP-vehicle client and is
not a runnable client test. Artifacts such as `jms-platform-tck` and
`connector` carry only `web_optional` classes, which the Web Profile TCK does
not require.

## Certification boundary

This runner covers the Platform integration artifacts above. Passing its
retained catalog is not by itself a Jakarta EE compatibility result: the
independently published TCK for each required Web Profile specification, plus
the formal challenge/exclusion review, must also pass for certification. Those
standalone TCKs deliberately stay outside the Platform artifact catalog; they
run from `runner-standalone/`.

The Jakarta EE 11 Web Profile specification (section 2.1) requires 22
component specifications. Mapped against this catalog:

- Platform-artifact integration coverage here, standalone TCK still required
  separately: Expression Language 6.0, JSON Binding 3.0, JSON Processing 2.1,
  Pages 4.0, Persistence 3.2, RESTful Web Services 4.0, Transactions 2.0,
  WebSocket 2.2.
- Fully covered by Platform artifacts: Enterprise Beans 4.0 Lite (`ejb30`,
  `ejb32`), Standard Tag Library 3.0 (`tags-tck`), Debugging Support for
  Other Languages 2.0 (via the Pages debugging classes).
- No coverage in this repository, certified only through their standalone
  TCKs: Servlet 6.1, Faces 4.1, CDI 4.1, Dependency Injection 2.0,
  Annotations 3.0, Interceptors 2.2, Validation 3.1, Security 4.0,
  Authentication 3.1, Concurrency 3.1, and Data 1.0 (new in Web Profile 11).

Note that Jakarta Authentication became a *required* Web Profile technology in
EE 11; its API packages are therefore part of the signature validation scope
(`environment/tck/ts.jte` must not list `jakarta.security.auth.message.*`
among ignorable optional packages).

## Safety and output

- A selected artifact fails during `validate` unless `-Dtck.partition` is set.
- Each manifest row supplies the class pattern appropriate to that published
  artifact; the JUnit tag expression remains the authoritative Web Profile
  selector. Override `-Dtck.test` only for diagnosis.
- TomEE uses ports 8080, 8443, and 8005 and Derby uses 1527;
  run technology jobs sequentially unless each job receives distinct ports.
- On JDK 21, TomEE 11 currently logs that JACC authorization checks are
  skipped because method security is not supported there. The Java 21 gate
  validates deployment and invocation, but security coverage remains blocked
  on that TomEE limitation.
- Partitioned Failsafe reports are in `runner-webprofile/run/target/failsafe-reports` and
  generated deployments are in `runner-webprofile/run/target/deployments`.
