# Environment integration contract

The TomEE remote Arquillian adapter supports three overlay properties:
`conf`, `bin`, and `lib`. Before integration tests, a runner should prepare
this layout beneath its own `target` directory:

```text
target/tomee-overlay/
├── bin/     # environment/tomee/bin
├── conf/    # environment/tomee/conf plus generated TLS files
└── lib/     # Derby client runtime
```

The `conf`, `bin`, and `lib` values are source directories. During container
startup `RemoteTomEEContainer` synchronizes them onto the corresponding
directories of the freshly unpacked TomEE distribution.

Wire the prepared absolute paths into the runner's `arquillian.xml`:

```xml
<property name="conf">@project.build.directory@/tomee-overlay/conf</property>
<property name="bin">@project.build.directory@/tomee-overlay/bin</property>
<property name="lib">@project.build.directory@/tomee-overlay/lib</property>
```

Have Maven filter only `arquillian.xml` with `@...@` delimiters. Do not use
the default `${...}` delimiter for the overlay: `server.xml` intentionally
contains Tomcat system-property placeholders such as `${tck.http.port}`.

The runner's preparation phase must:

1. copy `environment/tomee/conf` and `environment/tomee/bin` without modifying
   the source templates;
2. run `environment/certificates/generate-test-certificates.sh` with the
   overlay `conf` directory as its argument;
3. copy `org.apache.derby:derbyclient`, `derbyshared`, and `derbytools` at the
   version in `environment/versions.env` into the overlay `lib` directory;
4. keep the Arquillian `httpPort` and `stopPort` values aligned with
   `TCK_HTTP_PORT` (8080) and `TCK_SHUTDOWN_PORT` (8005), or override all of
   them together for a parallel worker.

The TLS generator also creates client-side files. They may stay outside the
server overlay when no selected test uses client-certificate authentication;
only `server.p12` and `server-truststore.p12` are read by `server.xml`.
