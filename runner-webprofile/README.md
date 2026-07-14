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
Platform TCK `11.0.2` source tag. Runtime artifacts come from the compatible
`11.0.3` publication because that release has no matching source tag. The
script verifies those expected counts against the generated reports, making an
upstream scope or selection change a hard failure.

Source and post-exclusion counts are separate manifest columns. Every reduction
must be explained in [`KNOWN_FAILURES.md`](KNOWN_FAILURES.md) and in the matching
file under `exclusions/`.

The manifest contains 1,132 Platform TCK classes. The eight other `web`-tagged
classes in the tagged repository are part of the standalone WebSocket TCK and
belong in the independent-component suite required by EE-WP21.

## Web Profile 11 coverage matrix

The authoritative required-component list is in the
[Jakarta EE Web Profile 11 specification](https://jakarta.ee/specifications/webprofile/11/jakarta-webprofile-spec-11.0.html#required-components).
The distinction below matters: Platform TCK artifacts have the common `11.0.3`
release version, while independent specification TCKs have their own coordinates
and versions and need dedicated runner/configuration work.

| Required technology | Version | Platform artifact here | Independent TCK runner status |
|---|---:|---|---|
| Annotations | 3.0 | `javaee-tck` integration | TODO: add standalone Annotations TCK |
| Authentication | 3.1 | `javaee-tck` integration | TODO: add standalone Authentication TCK and realm setup |
| Concurrency | 3.1 | `javaee-tck` integration | TODO: add `jakarta.enterprise.concurrent-tck` runner |
| CDI | 4.1 | `javaee-tck` integration | TODO: add a dedicated TestNG runner for `cdi-tck-ee-impl` and the CDI standalone suite |
| Data | 1.0 | `javaee-tck` integration | TODO: add `jakarta.data-tck` web-profile runner |
| Debugging Support | 2.0 | `pages-platform-tck` | Covered through Pages/Platform artifacts; verify upstream class manifest |
| Dependency Injection | 2.0 | `javaee-tck` integration | TODO: add standalone DI TCK |
| Enterprise Beans Lite | 4.0 | `javaee-tck` | Platform artifact runner available; class manifest still required |
| Expression Language | 6.0 | `el-platform-tck`, `javaee-tck` | TODO: add standalone EL TCK |
| Faces | 4.1 | `javaee-tck` integration | TODO: add standalone Faces TCK |
| Interceptors | 2.2 | `javaee-tck` integration | TODO: add standalone Interceptors TCK |
| JSON Binding | 3.0 | `jsonb-platform-tck` | TODO: add standalone JSON-B TCK |
| JSON Processing | 2.1 | `jsonp-platform-tck` | TODO: add standalone JSON-P TCK |
| Pages | 4.0 | `pages-platform-tck`, `javaee-tck` | TODO: add standalone Pages TCK |
| Persistence | 3.2 | `persistence-platform-tck-tests` | TODO: add standalone Persistence TCK and database matrix |
| RESTful Web Services | 4.0 | `rest-platform-tck` | TODO: add standalone REST TCK |
| Security | 4.0 | `javaee-tck` integration | TODO: add standalone Security TCK and realm/identity-store setup |
| Servlet | 6.1 | `javaee-tck` integration | TODO: add standalone Servlet TCK |
| Standard Tag Library | 3.0 | `tags-tck` | Platform artifact runner available; class manifest still required |
| Transactions | 2.0 | `transactions-tck`, `javaee-tck` | Platform artifact runner available; class manifest still required |
| Validation | 3.1 | `javaee-tck` integration | TODO: add standalone Validation TCK |
| WebSocket | 2.2 | `websocket-tck-platform-tests` | TODO: add standalone WebSocket TCK |

Passing every profile in this directory is therefore **not** by itself a Web
Profile compatibility result. The standalone component TCKs, Platform
signature tests, exact test manifests, allowed exclusions, and required
environment services must all be included before a certification run.

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
