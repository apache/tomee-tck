# Minimal Jakarta EE 11 JavaTest adapter configuration for TomEE.
platform.mode=jakartaEE
javaee.level=web
# Packages of optional technologies that the TomEE distributions expose through
# the combined jakartaee-api jar without declaring the technology. The
# signature test otherwise fails their "undeclared optional technology"
# presence check. Jakarta Authentication (jakarta.security.auth.message.*) is
# NOT listed: EE 11 makes it a required Web Profile technology, so its
# signatures must be validated. jakarta.batch.* and jakarta.jms are also
# deliberately not listed; their leak through the webprofile distribution is
# recorded as a signature-partition compatibility gap in KNOWN_FAILURES.md.
optional.tech.packages.to.ignore=jakarta.resource,jakarta.resource.cci,jakarta.resource.spi,jakarta.resource.spi.work,jakarta.resource.spi.endpoint,jakarta.resource.spi.security,jakarta.mail,jakarta.mail.event,jakarta.mail.internet,jakarta.mail.search,jakarta.mail.util,jakarta.security.jacc
bin.dir=${signature.bin.dir}
sigTestClasspath=${signature.classpath}

webServerHost=localhost
webServerPort=${tomee.http.port}
logical.hostname.servlet=localhost

# Vendor VariableMapper implementation the expression-language TCK's
# VarMapperELContext instantiates from the harness properties bag (testProps);
# Tomcat's Jasper EL provides it. Reaches the JSP and servlet vehicle tests
# that build the context inside the container.
variable.mapper=org.apache.el.lang.VariableMapperImpl

user=j2ee
password=j2ee
authuser=javajoe
authpassword=javajoe
user1=cts1
password1=cts1

DriverManager=no
Driver=org.apache.derby.jdbc.ClientDriver
jdbc.db=derby
db1=jdbc/DB1
db2=jdbc/DB2
ptable=ctstable1
ftable=ctstable2
cofSize=5
cofTypeSize=5
binarySize=24
varbinarySize=48
longvarbinarySize=50
db.supports.sequence=false
db.dml.file=environment/tck/derby.dml.sql

harness.log.port=${tck.harness.log.port}
harness.log.traceflag=false
harness.socket.retry.count=10
harness.temp.directory=${java.io.tmpdir}
work.dir=${java.io.tmpdir}
ts_home=${tck.ts.home}
