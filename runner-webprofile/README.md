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
Platform TCK `11.0.3` artifacts. The script verifies those expected counts
against the generated reports, making an upstream scope or selection change a
hard failure.

Source and post-exclusion counts are separate manifest columns. Every reduction
must be explained in [`KNOWN_FAILURES.md`](KNOWN_FAILURES.md) and in the matching
file under `exclusions/`.

The manifest contains 1,132 Platform TCK classes. After the reviewed TomEE
compatibility exclusions, 673 classes remain enabled.

## Certification boundary

This runner replaces the old Jakarta EE Platform TCK harness and covers the
Platform integration artifacts above. Passing its retained catalog is not by
itself a Jakarta EE compatibility result: the independently published TCK for
each required Web Profile specification, plus the formal challenge/exclusion
review, must also pass for certification. Those standalone TCKs deliberately
remain outside this repository's Platform artifact catalog.

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
