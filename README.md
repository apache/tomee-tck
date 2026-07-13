# Jakarta EE 11 Web Profile TCK runner

This is the replacement path for running the Jakarta EE 11 Web Profile TCK
against TomEE 11. It uses Maven, JUnit 5 and Arquillian; it does not use the
legacy JavaTest/Ant harness, `TSDeployment`, or a checked-in TCK distribution.

The current milestone is deliberately small: the reactor builds the TomEE
archive processor and runs one deploy/invoke/undeploy smoke test. A green smoke
test proves the container integration, not Web Profile compatibility.

## Reproducible inputs

The build uses these centrally managed inputs:

| Input | Coordinate/build | Integrity evidence |
| --- | --- | --- |
| Jakarta EE API | `jakarta.platform:jakarta.jakartaee-api:11.0.0` | Maven Central transport and repository checksums |
| Platform TCK artifacts | `jakarta.tck:artifacts-bom:11.0.3` | SHA-256 in `environment/versions.env` |
| TCK Arquillian porting library | `jakarta.tck.arquillian:tck-porting-lib:11.1.3` | version selected by the 11.0.3 BOM |
| TomEE Web Profile | `org.apache.tomee:apache-tomee:11.0.0-SNAPSHOT:webprofile:zip` | exact timestamped build and SHA-512 in `environment/versions.env` |
| TomEE remote adapter | `org.apache.tomee:arquillian-tomee-remote:11.0.0-SNAPSHOT` | exact timestamped build and SHA-512 in `environment/versions.env` |
| Derby runtime | `derbyclient`, `derbynet`, `derbyshared`, and `derbytools` `10.15.2.0` | per-jar SHA-256 values in `environment/versions.env` |

TomEE is temporarily a snapshot because this work targets development after
the 11.0.0-M1 milestone. A snapshot can change without its coordinate changing.
CI therefore fails if Maven resolves bytes different from the reviewed lock.
When intentionally advancing TomEE, update the timestamped build names and
SHA-512 values together with the root Maven version.

Verify the immutable TCK BOM metadata without downloading TomEE:

```sh
sh environment/verify-inputs.sh --metadata-only
```

After Maven has resolved TomEE, verify the exact distribution and adapter:

```sh
sh environment/verify-inputs.sh --require-tomee
```

## Run the smoke test

Requirements are JDK 17 or 21, `curl`, and a working Docker-free local network
stack. Maven itself is supplied by the wrapper.

```sh
./mvnw -B -ntp -pl runner-smoke -am verify
```

Run the complete reactor (currently the porting module and smoke runner):

```sh
./mvnw -B -ntp verify
```

The Web Profile runner catalog keeps broader work opt-in. Select exactly one
artifact profile and one test class, for example:

```sh
./mvnw -B -ntp -Ptck-rest-platform \
  -pl runner-webprofile/run -am \
  -Dtck.test=com.sun.ts.tests.jaxrs.platform.servletconfig.JAXRSClientIT \
  verify
```

See `runner-webprofile/README.md` for the available artifact profiles and the
coverage gaps that remain before this can produce a certification result.

For a locally built TomEE snapshot, install both the Web Profile distribution
and remote Arquillian adapter into the same Maven repository first. Then update
the lock file to the actual resolved checksums before treating the run as
reproducible.

## Environment templates

`environment/tomee/conf` contains a clean TomEE overlay rather than a copy of
the old harness configuration:

- `tomee.xml` declares `jdbc/DB1`, `jdbc/DB2`, and `jdbc/DBTimer` using the
  TCK-documented Derby test account `cts1`.
- `tomcat-users.xml` contains only the users, passwords and groups prescribed
  by the EE 11 TCK guide (`j2ee_vi`, `javajoe`, `j2ee`, and `j2ee_ri`). These
  credentials are strictly test-only.
- `server.xml` defines HTTP and HTTPS connectors and optionally accepts a
  client certificate, so ordinary HTTPS and mutual-authentication tests can
  share one connector.
- `system.properties` enables strict Servlet behavior and TomEE remote support.

The catalog runner stages these files under its `target` directory, starts a
temporary Derby network server, overlays the freshly unpacked TomEE, and stops
both services after Failsafe completes. It never modifies a user’s installed
TomEE or `~/.m2/settings.xml`. The server uses fixed localhost ports 8005, 8080,
8443, and 1527, so run one catalog worker at a time.

TomEE 11 currently reports on JDK 21 that JACC authorization checks are skipped
because method security is not supported there. The JDK 21 CI gate still checks
container startup, deployment, HTTP invocation and cleanup, but security tests
cannot be considered covered until that TomEE limitation is resolved.

Generate disposable TLS material with the `changeit` password documented by
the TCK guide:

```sh
sh environment/certificates/generate-test-certificates.sh
```

The output goes to `target/ee11-certificates`. Copy `server.p12` and
`server-truststore.p12` to the test server’s `conf` directory. Configure the
test JVM with `clientcert.p12` and `client-truststore.p12` when a test requires
client-certificate authentication. Generated keys are test-only and must not
be committed.

No `ts.jte` is staged: the current JUnit 5/Arquillian gate receives its client
properties directly from Maven. Add legacy TCK properties only when a selected
artifact demonstrably reads them. Deployment is handled by
`org.apache.tomee.tck.porting.TomEETestArchiveProcessor`, registered as an
Arquillian extension.

## Expanding to the Web Profile

Each TCK artifact needs a small runner or a well-isolated execution in a shared
runner. Configure Surefire/JUnit with the `web` group; that tag is the official
selection mechanism for required Web Profile tests. Add suites incrementally:

1. signature validation;
2. Annotations, EL, JSON-P and JSON-B;
3. Servlet, REST and WebSocket;
4. CDI and Validation;
5. Persistence and Transactions;
6. Faces, Pages and Tags;
7. Enterprise Beans Lite, Security and Authentication.

Every suite must preserve Surefire XML and the corresponding TomEE logs. A full
Web Profile result also needs a reviewed exclusion/challenge list and an exact
manifest of TCK, TomEE, JDK and operating-system inputs.

## CI

`.github/workflows/ee11-webprofile.yml` validates the environment, runs the
smoke runner, and executes the selected REST Platform catalog gate on Temurin
17 and 21. It archives JUnit reports and TomEE logs even when a test fails. The
workflow actions are pinned to full commit hashes.

## Authoritative references

- [Jakarta EE Platform 11 release page](https://jakarta.ee/specifications/platform/11/)
  (minimum Java 17, final API coordinate, and official TCK links)
- [Jakarta EE 11 Web Profile specification](https://jakarta.ee/specifications/webprofile/11/)
- [Platform TCK 11.0.2 release notes](https://github.com/jakartaee/platform-tck/blob/11.0.2/release/README.adoc)
  (JUnit 5/Arquillian architecture and Maven artifact split)
- [Tagged EE 11 Web Profile TCK setup guide](https://github.com/jakartaee/platform-tck/blob/11.0.2/tcks/profiles/platform/docs/userguide/platform/src/main/asciidoc/webprofileconfig.adoc)
  (`web` group selection)
- [Tagged EE 11 TCK configuration guide](https://github.com/jakartaee/platform-tck/blob/11.0.2/tcks/profiles/platform/docs/userguide/platform/src/main/asciidoc/config.adoc)
  (test identities, JDBC names, TLS defaults, and warning about unused
  `ts.jte` remnants)
