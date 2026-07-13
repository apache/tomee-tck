# Minimal Jakarta EE 11 Web Profile TCK environment.
# Add properties only when a selected TCK artifact demonstrably reads them.

webServerHost=localhost
webServerPort=8080
webServerSecuredPort=8443

harness.log.port=2000
harness.log.traceflag=false

# TCK-prescribed test-only server identities.
user.vi=j2ee_vi
password.vi=j2ee_vi
user.ri=j2ee_ri
password.ri=j2ee_ri
authuser=javajoe
authpassword=javajoe

# TCK-prescribed database defaults and logical resources.
user1=cts1
password1=cts1
user2=cts1
password2=cts1
user3=cts1
password3=cts1
db1=jdbc/DB1
db2=jdbc/DB2
dbTimer=jdbc/DBTimer
Driver=org.apache.derby.jdbc.ClientDriver
DriverManager=java.sql.DriverManager

# EE 11 deployment is handled by the Arquillian LoadableExtension and
# TomEETestArchiveProcessor, not by the removed JavaTest deployment adapter.
