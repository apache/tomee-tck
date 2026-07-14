# TomEE Jakarta EE 11 Web Profile smoke runner

This module is the first migration gate for the Jakarta EE 11 Platform TCK. It
uses JUnit 5, Arquillian, the TomEE 11 remote adapter, and the TomEE 11 Web
Profile distribution. Versions are controlled by the root Maven reactor and
the Jakarta TCK artifacts BOM.

The selected test is
`com.sun.ts.tests.jaxrs.platform.servletconfig.JAXRSClientIT` from
`jakarta.tck:rest-platform-tck:11.0.3`. It is tagged `web` and `platform` and
creates one non-testable JAX-RS WAR. A successful run therefore proves that the
published EE 11 TCK test artifact can start TomEE, deploy a WAR, make HTTP
requests to it, undeploy it, and stop TomEE.

## Prerequisites

- JDK 17 or newer.
- The current `11.0.0-SNAPSHOT` TomEE remote adapter and Web Profile
  distribution, available from the Apache snapshot repository or the local
  Maven repository.
- Network access to Maven Central, which publishes
  `jakarta.tck:rest-platform-tck:11.0.3`, or that artifact already cached in
  the local Maven repository.

## Run

From the repository root:

```shell
./mvnw -pl runner-smoke -am verify
```

To substitute another test class from the same published TCK artifact:

```shell
./mvnw -pl runner-smoke -am \
  -Dsmoke.test=com.sun.ts.tests.somepackage.AnotherClientIT verify
```

Arquillian exports the generated archive to `target/deployments`, while
Failsafe writes its reports under `target/failsafe-reports`. The TomEE remote
adapter resolves and unpacks the Web Profile distribution in the module's
build directory.

The shared archive processor is on the test class path. Database, security,
TLS, mail, LDAP, and messaging services are deliberately outside this smoke
gate; the chosen test does not require them.
