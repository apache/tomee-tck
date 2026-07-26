# Apache TomEE Jakarta EE TCK harness

This repository is the harness for running the Jakarta EE TCKs against Apache
TomEE — currently the Jakarta EE 11 Web Profile TCK against TomEE 11. Runs
target the TomEE Plume distribution by default; set `TOMEE_CLASSIFIER` to
`webprofile`, `microprofile`, or `plus` to test another distribution.

A run can test either a TomEE already published to a Maven repository or one
built from a specific tag, branch, or commit of apache/tomee — see
[Test a specific TomEE ref](#test-a-specific-tomee-ref).

The harness uses the TCK's published Maven artifacts, JUnit 5 and Arquillian;
there is no Ant launcher, `TSDeployment`, or checked-in TCK distribution. A
small smoke module checks the TomEE adapter, the partitioned Platform runner
executes the Web Profile-tagged tests and keeps a reviewed list of known
compatibility gaps, and `runner-standalone/` drives the independently
published per-specification TCKs.

[KNOWN_ISSUES.md](KNOWN_ISSUES.md) is the central triage list of everything
that is currently broken — TomEE product gaps, upstream provider gaps, and
remaining harness work — with pointers to the reviewed per-suite exclusion
lists that keep the default runs green.

## Reproducible inputs

The build uses these centrally managed inputs:

| Input | Coordinate/build | Integrity evidence |
| --- | --- | --- |
| Jakarta EE API | `jakarta.platform:jakarta.jakartaee-api:11.0.0` | Maven Central transport and repository checksums |
| Platform TCK artifacts | `jakarta.tck:artifacts-bom:11.0.3` | SHA-256 in `environment/versions.env` |
| TCK Arquillian porting library | `jakarta.tck.arquillian:tck-porting-lib:11.1.3` | version selected by the 11.0.3 BOM |
| TomEE distribution under test | `org.apache.tomee:apache-tomee:${tomee.version}:${tomee.classifier}:zip` | git commit when built from a ref; otherwise a mutable development snapshot |
| TomEE remote adapter | `org.apache.tomee:arquillian-tomee-remote:${tomee.version}` | git commit when built from a ref; otherwise a mutable development snapshot |
| Derby runtime | `derbyclient`, `derbynet`, `derbyshared`, and `derbytools` `10.15.2.0` | per-jar SHA-256 values in `environment/versions.env` |

TomEE comes from one of two places. Built from a git ref, the exact commit
identifies the bytes and no checksum is needed. Resolved from the snapshot
repository instead — the default when `TOMEE_VERSION` is unset — Maven uses
the current `11.0.0-SNAPSHOT` and neither pins nor checksums those changing
bytes. Build from a tag when the harness needs a stable qualification
candidate.

Verify the immutable TCK BOM metadata only (a plain
`sh environment/verify-inputs.sh` also downloads and verifies the pinned
Derby jars):

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

`TOMEE_CLASSIFIER` accepts `plume`, `webprofile`, `microprofile`, and `plus`,
and every runner script rejects anything else. The Platform catalog manifest
and the reviewed exclusion lists describe `plume` and `webprofile`; a
`microprofile` or `plus` run reuses the `plume` expectations, so read its
failures against that distribution's own scope rather than as regressions.

## Test a specific TomEE ref

`environment/tomee/build-tomee.sh` builds Apache TomEE from any tag, branch,
or commit and installs every distribution ZIP and the `arquillian-tomee-remote`
adapter into a Maven repository, so the suites can run against a build that
was never deployed:

```sh
environment/tomee/build-tomee.sh tomee-project-9.1.3
```

It prints the built version as a single `TOMEE_VERSION=<version>` line — read
from the reactor, so a tag builds its release version and a branch builds
whatever `-SNAPSHOT` it carries. Pass that version and the flavour back to any
runner:

```sh
TOMEE_VERSION=11.0.0-SNAPSHOT TOMEE_CLASSIFIER=plume \
  runner-webprofile/run-platform-suite.sh servlet rest
```

Both variables are optional: unset, `TOMEE_VERSION` leaves the pom's snapshot
version in place and `TOMEE_CLASSIFIER` defaults to `plume`. Set
`TOMEE_REPO_URL` to build from a fork, and `TOMEE_SRC_DIR` to place the clone
somewhere other than `target/tomee-src`. The build skips TomEE's own tests and
uses its `quick` profile, which drops the examples, itests, and TomEE's own
tck modules while still assembling all four distributions.

## ASF Jenkins pipeline

The root `Jenkinsfile` is the authoritative CI definition. It targets the ASF
Jenkins `ubuntu` agents. The pipeline validates the environment, builds the
TomEE under test, runs the smoke gate on JDK 17 and JDK 21 in parallel, and
then fans out every manifest partition and every standalone suite as an
independent JDK 21 branch. Test reports and TomEE logs are archived for 14
days.

Three build parameters select what is tested:

| Parameter | Default | Meaning |
| --- | --- | --- |
| `TOMEE_REF` | `main` | Tag, branch, or commit of apache/tomee to build and test |
| `TOMEE_REPO_URL` | `https://github.com/apache/tomee.git` | Clone source; point at a fork for an unmerged branch |
| `TOMEE_CLASSIFIER` | `plume` | Distribution to test: `plume`, `webprofile`, `microprofile`, or `plus` |

The `Build TomEE` stage runs `environment/tomee/build-tomee.sh` for the chosen
ref, installs the result into a workspace-local Maven repository, and stashes
the `org/apache/tomee` slice of it. Every downstream branch unstashes that
slice into its own workspace repository and passes the built version and the
selected flavour to its runner, so the whole pipeline reports on one TomEE
build. Nothing is deployed to a shared repository, so concurrent jobs and
workstation repositories are untouched. The build description records the ref,
version, and flavour each run tested.

Parallel branches request `ubuntu && ephemeral` agents and run their suites
inside digest-pinned containers (`eclipse-temurin` JDK images; a
Chrome-bundling image for the modern `faces` suite), so every TomEE, Derby,
LDAP and JavaTest port binds a container-private network namespace and cannot
collide with other jobs on the same host. The runner scripts' free-port
selection and require-free guards remain as defense in depth and for
workstation runs outside a container. Jenkins queues branches when fewer
workers are available, so the pipeline uses available capacity without
imposing a fixed partition count.
Configure an ASF Jenkins multibranch Pipeline job to use `Jenkinsfile` from
SCM; Jenkins supplies the checkout, the container images supply the JDKs, and
the checked-in Maven wrapper supplies Maven 3.9.9 inside each container. Each
branch sets `HOME` to its Jenkins workspace inside the container, so the Maven
wrapper distribution and local repository live under the workspace and are
populated from scratch every build.

See `runner-webprofile/README.md` for the available artifact profiles and the
coverage gaps that remain before this can produce a certification result.

To test a TomEE checkout you already have locally, install the distribution
under test and the remote Arquillian adapter into the same Maven repository
first, or let `environment/tomee/build-tomee.sh` do it from a git ref.
Development runs that resolve the snapshot instead intentionally consume the
mutable bytes without a checksum lock.

## Standalone specification TCKs

`runner-standalone/` gathers the independently published TCKs that Jakarta EE
11 Web Profile certification requires in addition to the Platform catalog:
Servlet, Pages, RESTful Web Services, WebSocket, Faces, CDI (with
Interceptors), Dependency Injection, Annotations, Validation, Security,
Authentication, Concurrency, Data, JSON Processing, JSON Binding, and
Debugging Support. Run one at a time with:

```sh
runner-standalone/run-standalone-suite.sh concurrency
```

See [runner-standalone/README.md](runner-standalone/README.md) for the
per-specification status: Annotations, Dependency Injection, Pages, JSON
Processing (197/197 on Apache Johnzon), and Debugging Support pass, and every
other suite — including the source-reactor TCKs (Security, Authentication,
Faces) — executes end to end with dated result counts and the remaining TomEE
or provider gaps recorded for triage.

## Environment templates

`environment/tomee/conf` contains a minimal TomEE overlay carrying only what
the TCK requires:

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

The Platform catalog does not bundle or claim results for each specification
project's independently published standalone TCK. Those independent suites are
additional inputs to a formal Jakarta EE compatibility certification, not
missing Platform artifact partitions; they run from `runner-standalone/` as
separate suites with their own status tracking.

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
