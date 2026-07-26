# Environment integration contract

The TomEE remote Arquillian adapter supports the two overlay properties used
here, `conf` and `lib`. Before integration tests, `runner-webprofile/run` prepares
this layout beneath its own `target` directory:

```text
target/tomee-overlay/
├── conf/    # environment/tomee/conf plus generated TLS files
└── lib/     # Derby client runtime and Jakarta Tags implementation
```

The `conf` and `lib` values are source directories. During container
startup `RemoteTomEEContainer` synchronizes them onto the corresponding
directories of the freshly unpacked TomEE distribution.

Wire the prepared absolute paths into the runner's `arquillian.xml`:

```xml
<property name="conf">${project.build.directory}/tomee-overlay/conf</property>
<property name="lib">${project.build.directory}/tomee-overlay/lib</property>
```

Have Maven filter only `arquillian.xml`; copy the TomEE templates byte-for-byte
so Maven never interprets Tomcat configuration syntax.

The runner's preparation phase:

1. copy `environment/tomee/conf` without modifying the source templates;
2. run `environment/certificates/generate-test-certificates.sh` with the
   overlay `conf` directory as its argument;
3. copy `org.apache.derby:derbyclient`, `derbyshared`, and `derbytools` at the
   version in `environment/versions.env` into the overlay `lib` directory (the
   configured `org.apache.derby.jdbc.ClientDriver` is in `derbytools`), while
   using `derbynet` only in the separate Derby process, and replace TomEE's
   legacy JSTL jar with the Jakarta Tags 3.0.1 implementation (the Tags TCK
   still records TomEE's missing short-URI registration as a product gap);
4. create the `CTS1` schema and shared Platform tables for every partition
   before TomEE starts. The JDBC and Persistence partitions additionally
   create their technology-specific tables and stored procedures from the
   Platform TCK Derby definitions. The checked-in scripts contain the
   authoritative `CREATE` statements but omit `DROP` statements because every
   invocation starts with a freshly deleted Derby home;
5. use fixed localhost ports 8005, 8080, 8443, and 1527. The catalog is a
   single-worker harness until port allocation is implemented consistently for
   Arquillian, TomEE, Derby, and TCK client properties.

The TLS generator also creates client-side files. They may stay outside the
server overlay when no selected test uses client-certificate authentication;
only `server.p12` and `server-truststore.p12` are read by `server.xml`.

The JavaTest Arquillian protocol reads `environment/tck/ts.jte`. This is a
small set of properties actually propagated by the Jakarta EE 11 adapter, not
the historical GlassFish-oriented `ts.jte`. Add a property only when a selected
test documents or demonstrates that it needs one.
The companion `derby.dml.sql` is copied verbatim from the Platform TCK 11.0.2
release tree and is passed as the adapter's additional-property statement file.
`persistence-derby.ddl.sql` is derived from the Jakarta EE 11 Platform TCK
`jakartaeetck-11.0.1` GlassFish runner, with only clean-database `DROP`
statements and the runner-specific procedure-jar installation removed.

## Building the TomEE under test

`tomee/build-tomee.sh <git-ref> [repository-directory]` builds Apache TomEE
from any tag, branch, or commit and installs every distribution ZIP plus the
`arquillian-tomee-remote` adapter into a Maven repository, so a suite can test
a build that was never deployed. The clone lives in `target/tomee-src`
(override with `TOMEE_SRC_DIR`) and the source is `apache/tomee` (override
with `TOMEE_REPO_URL`).

apache/tomee ships no Maven wrapper, so the build runs through this
repository's wrapper pointed at the TomEE reactor. It skips TomEE's own tests
— the TCK suites are what validate the build — and uses TomEE's `quick`
profile, the narrowest one that still assembles all four distributions and
builds the adapter.

The script writes diagnostics to stderr and a single `TOMEE_VERSION=<version>`
line to stdout, read from the reactor rather than derived from the ref, so a
caller can capture it with a plain command substitution and pass it to the
runners as `TOMEE_VERSION`. It fails if the build did not install all four
distribution ZIPs and the adapter, rather than letting each suite fail later
on an unresolvable artifact.
