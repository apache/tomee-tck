# Environment integration contract

The TomEE remote Arquillian adapter supports the two overlay properties used
here, `conf` and `lib`. Before integration tests, `runner-webprofile/run` prepares
this layout beneath its own `target` directory:

```text
target/tomee-overlay/
├── conf/    # environment/tomee/conf plus generated TLS files
└── lib/     # Derby client runtime
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
   using `derbynet` only in the separate Derby process;
4. use fixed localhost ports 8005, 8080, 8443, and 1527. The catalog is a
   single-worker harness until port allocation is implemented consistently for
   Arquillian, TomEE, Derby, and TCK client properties.

The TLS generator also creates client-side files. They may stay outside the
server overlay when no selected test uses client-certificate authentication;
only `server.p12` and `server-truststore.p12` are read by `server.xml`.

The JavaTest Arquillian protocol reads `environment/tck/ts.jte`. This is a
small set of properties actually propagated by the Jakarta EE 11 adapter, not
the historical GlassFish-oriented `ts.jte`. Add a property only when a selected
test documents or demonstrates that it needs one.
