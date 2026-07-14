# Minimal Jakarta EE 11 JavaTest adapter configuration for TomEE.
platform.mode=jakartaEE
javaee.level=web

webServerHost=localhost
webServerPort=8080
logical.hostname.servlet=localhost

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
db.supports.sequence=false

harness.log.port=2000
harness.log.traceflag=false
harness.socket.retry.count=10
harness.temp.directory=${java.io.tmpdir}
work.dir=${java.io.tmpdir}
ts_home=${java.io.tmpdir}
