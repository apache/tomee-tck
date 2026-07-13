# Jakarta EE 11 Web Profile runner catalog

This module is the explicit entry point for Jakarta EE 11 Web Profile
certification work. The default build runs no TCK tests. Select one published
Platform TCK artifact and one test (or a deliberately narrow test pattern):

```shell
./mvnw -Ptck-rest-platform \
  -pl runner-webprofile/run -am \
  -Dtck.test=com.sun.ts.tests.jaxrs.platform.servletconfig.JAXRSClientIT \
  verify
```

The artifact profiles use the version inherited from
`jakarta.tck:artifacts-bom`, currently `11.0.3`. Do not activate multiple
`tck-*` profiles in one invocation: they share the `tck.scan` selector. Split
technologies into separate CI jobs instead; that also isolates TomEE and port
collisions and produces useful per-technology reports.

## Platform integration artifact profiles

| Profile | Published artifact scanned | Web Profile responsibility |
|---|---|---|
| `tck-platform` | `jakarta.tck:javaee-tck` | Cross-specification requirements, Servlet, security, naming, EJB Lite, and other classic Platform tests |
| `tck-rest-platform` | `jakarta.tck:rest-platform-tck` | REST integration beyond the standalone REST TCK |
| `tck-el-platform` | `jakarta.tck:el-platform-tck` | Expression Language Platform integration |
| `tck-jsonb-platform` | `jakarta.tck:jsonb-platform-tck` | JSON Binding Platform integration |
| `tck-jsonp-platform` | `jakarta.tck:jsonp-platform-tck` | JSON Processing Platform integration |
| `tck-pages-platform` | `jakarta.tck:pages-platform-tck` | Pages and debugging-language integration |
| `tck-persistence-platform` | `jakarta.tck:persistence-platform-tck-tests` | Persistence Platform integration (also supplies its common artifact) |
| `tck-tags` | `jakarta.tck:tags-tck` | Standard Tag Library tests published with the Platform TCK |
| `tck-transactions` | `jakarta.tck:transactions-tck` | Transactions Platform integration |
| `tck-websocket-platform` | `jakarta.tck:websocket-tck-platform-tests` | WebSocket Platform integration (also supplies its common artifact) |

These profiles are runners, not a suite definition. A checked-in suite manifest
or CI matrix should enumerate exact classes and exclusions. This avoids silently
changing the certification scope when an upstream artifact adds tests.

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

- A selected profile fails during `validate` unless `-Dtck.test` is set.
- `-Dtck.test` accepts a fully qualified class or a narrow Failsafe pattern.
- TomEE uses ports 8080, 8443, and 8005 and Derby uses 1527;
  run technology jobs sequentially unless each job receives distinct ports.
- On JDK 21, TomEE 11 currently logs that JACC authorization checks are
  skipped because method security is not supported there. The Java 21 gate
  validates deployment and invocation, but security coverage remains blocked
  on that TomEE limitation.
- Failsafe reports are in `runner-webprofile/run/target/failsafe-reports` and
  generated deployments are in `runner-webprofile/run/target/deployments`.
