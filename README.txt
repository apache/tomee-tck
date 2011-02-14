GETTING SETUP

 This document and the OpenEJB TCK setup can be checked out from here:

  svn co https://svn.apache.org/repos/tck/openejb-tck

 The Java EE 6 TCK and RI can be downloaded from here:

  svn export https://svn.apache.org/repos/tck/sun-tcks/javaee/6/javaeetck-6.0_25-Oct-2010.zip
  svn export https://svn.apache.org/repos/tck/sun-tcks/javaee/6/javaee6_ri-3.0.1-b22.zip

 Both are required to run the TCK.  The TCK is 813M, beware.

 Once unpacked, they can be "hooked" up via the maven settings.xml
 file like so:

    <settings>
      <profiles>
        <profile>
          <id>javaee-tck-environment</id>
          
          <activation>
    	    <activeByDefault>true</activeByDefault>
          </activation>
          
          <properties>
    	  <javaee6.cts.home>/Users/dblevins/work/javaeetck</javaee6.cts.home>
    	  <javaee6.ri.home>/Users/dblevins/work/javaeetck/glassfishv3</javaee6.ri.home>
          </properties>
        </profile>
      </profiles>
    </settings>

 The TCK will unzip into a directory called 'javaeetck/'.  The RI will
 unzip into a directory called 'glassfishv3'.  As you can see above
 we've unzipped the RI inside the javaeetck directory just to keep
 things tidy.  This is not required for the TCK to function properly.

TEST RUN

 From inside the openejb-tck directory, a command like this will get
 you a little taste of running the TCK:

  ./runtests com.sun.ts.tests.ejb30.bb.localaccess.statelessclient

 We don't yet pass all of the TCK, but the above tests should be among
 the passing sections and are a good way to validate all is setup
 properly.

TOMCAT

 By default the `runtests` command will execute the tests against an
 OpenEJB standalone server.

 To run in Tomcat, a command like the following will work:

  ./runtests --tomcat-version 6.0.32 com.sun.ts.tests.ejb30.bb.localaccess.statelessclient

 Any Tomcat version can be specfified and it will be downloaded prior
 to running the tck.  The are stored in the $HOME/.m2/repository, but
 with the group id org.apache.openejb.tck as to discourage accidental
 dependence in other projects.

TOMEE

 To run in Tomee, specify the webcontainer is "tomee".

  ./runtests --web tomee com.sun.ts.tests.ejb30.bb.localaccess.statelessclient

MISC

 The target directory is not cleaned out at the beginning of a test
 run.  There are a few thousand tests and sometimes multiple
 executions are required to get complete results.  It's also nice to
 be able to look back on older log files when tracking down and fixing
 bugs that the tests uncover.

 Bottom line is you have to clear out the target directory manually.
 On occasion some bad state will get into the server install in the
 target/ directory.  If you start getting weirld maven or groovy
 errors, clean out the target dir and try again.

TAB COMPLETION

 There is a nice little script in the root directory called
 runtests.completer which, when sourced, can give be a great
 time-saver when trying to navigate to run a specific test.

 In bash just source the file like so:

  source runtests.completer

LOGS

 The TCK for the most part runs as a client in a separate vm.  The
 test results are sent to this vm and then logged here:

   target/logs/javatest.log

 When looking at exceptions in that log file often come from the
 remote deployer tool -- the same tool we use on the command line for
 deployment.  Most of the deployment related exceptions were generated
 on the server and sent to the client and that's why the show up in
 that log.

 The server logs are in the usual place:

   target/openejb-3.2-SNAPSHOT/logs/openejb.log
   target/apache-tomcat-6.0.32/logs/openejb.log

