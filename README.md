# Jakarta EE 11 Web Profile TCK runner

This is the replacement path for running the Jakarta EE 11 Web Profile TCK
against TomEE 11. It uses the TCK's published Maven artifacts, JUnit 5 and
Arquillian; it does not use the old Ant launcher, `TSDeployment`, or a
checked-in TCK distribution. A small smoke module checks the TomEE adapter,
while the partitioned Platform runner executes the Web Profile-tagged tests and
keeps a reviewed list of known compatibility gaps.

## Reproducible inputs

The build uses these centrally managed inputs:

| Input | Coordinate/build | Integrity evidence |
| --- | --- | --- |
| Jakarta EE API | `jakarta.platform:jakarta.jakartaee-api:11.0.0` | Maven Central transport and repository checksums |
| Platform TCK artifacts | `jakarta.tck:artifacts-bom:11.0.3` | SHA-256 in `environment/versions.env` |
| TCK Arquillian porting library | `jakarta.tck.arquillian:tck-porting-lib:11.1.3` | version selected by the 11.0.3 BOM |
| TomEE Plume (default target under test) | `org.apache.tomee:apache-tomee:11.0.0-SNAPSHOT:plume:zip` | mutable development snapshot; not locked yet |
| TomEE Web Profile (opt-in via `TOMEE_CLASSIFIER=webprofile`) | `org.apache.tomee:apache-tomee:11.0.0-SNAPSHOT:webprofile:zip` | mutable development snapshot; not locked yet |
| TomEE remote adapter | `org.apache.tomee:arquillian-tomee-remote:11.0.0-SNAPSHOT` | mutable development snapshot; not locked yet |
| Derby runtime | `derbyclient`, `derbynet`, `derbyshared`, and `derbytools` `10.15.2.0` | per-jar SHA-256 values in `environment/versions.env` |

TomEE is temporarily sourced from the snapshot repository because this work
targets active TomEE 11 development after the 11.0.0-M1 milestone. Maven always
uses the current `11.0.0-SNAPSHOT`; CI does not pin or checksum those changing
bytes. A timestamped version and checksum should only be added when the harness
needs a stable qualification candidate.

Verify the immutable TCK BOM metadata without downloading TomEE:

```sh
sh environment/verify-inputs.sh --metadata-only
```

## Run the smoke test

Requirements are JDK 17 or 21, `curl`, and a working Docker-free local network
stack. Maven itself is supplied by the wrapper.

```sh
./mvnw -B -ntp -pl runner-smoke -am verify
```

Run the default reactor (porting-extension tests and the smoke runner; catalog
TCK partitions remain opt-in):

```sh
./mvnw -B -ntp verify
```

Run one catalog partition, for example:

```sh
runner-webprofile/run-platform-suite.sh servlet rest
```

The EclipseLink-based TomEE Plume distribution is the default target under
test; the Jakarta Persistence catalog passes almost completely there, unlike
on OpenJPA. Test the OpenJPA-based `webprofile` ZIP instead with:

```sh
TOMEE_CLASSIFIER=webprofile runner-webprofile/run-platform-suite.sh javatest persistence-javatest
```

## ASF Jenkins pipeline

The root `Jenkinsfile` is the authoritative CI definition. It targets the ASF
Jenkins `ubuntu` agents and their managed `jdk_17_latest` and `jdk_21_latest`
tools. The pipeline validates the environment, runs the smoke gate on both
JDKs in parallel, and then fans out every manifest partition as an independent
JDK 21 branch against the default TomEE Plume distribution. One additional
branch runs the Jakarta Persistence javatest partition against the
OpenJPA-based webprofile distribution to track its reviewed exclusion list.
Test reports and TomEE logs are archived for 14 days.

Parallel branches request `ubuntu && ephemeral` agents. The current ASF cloud
workers advertise one executor per host, which isolates the fixed localhost
ports used by TomEE and Derby. Jenkins queues branches when fewer workers are
available, so the pipeline uses available capacity without imposing a fixed
partition count.
Configure an ASF Jenkins multibranch Pipeline job to use `Jenkinsfile` from
SCM; Jenkins supplies the checkout and managed JDKs, while the checked-in Maven
wrapper supplies Maven 3.9.9.

See `runner-webprofile/README.md` for the available artifact profiles and the
coverage gaps that remain before this can produce a certification result.

For a locally built TomEE snapshot, install the distribution under test (Plume
by default) and the remote Arquillian adapter into the same Maven repository
first. Development runs intentionally consume the mutable snapshot without a
checksum lock.

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

The JavaTest-protocol adapter reads the deliberately small
`environment/tck/ts.jte`. It contains only properties still consumed by the
published EE 11 tests, including the JDBC names and sizing values. Maven
supplies modern runner properties directly. Deployment is handled by
`org.apache.tomee.tck.porting.TomEETestArchiveProcessor`, registered as an
Arquillian extension.

## Scope

The catalog covers every class published in the Jakarta EE 11 Platform TCK
artifacts and selected by the official `web` JUnit group. It includes signature
validation and all servlet- and JavaTest-protocol partitions represented by
those artifacts. Known TomEE compatibility gaps remain visible as reviewed
class or method exclusions; they are not compatibility passes.

This repository replaces the old Jakarta EE *Platform TCK* harness. It does not
bundle or claim results for each specification project's independently
published standalone TCK. Those independent suites are additional inputs to a
formal Jakarta EE compatibility certification, not missing Platform artifact
partitions in this runner.

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
