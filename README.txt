GETTING SETUP

 This document and the OpenEJB TCK setup can be cloned from Git:

  git clone git clone https://gitbox.apache.org/repos/asf/tomee-tck

In order to run the TCK, you will need both the TCK binary itself, and the Eclipse Glassfish RI.

At present, we are building the TCK binary from source, following these steps:

  git clone https://github.com/eclipse-ee4j/jakartaee-tck
  cd jakaratee-tck
  export WORKSPACE=$(pwd)
  export GF_BUNDLE_URL=https://jenkins.eclipse.org/glassfish/job/glassfish/job/EE4J_8/85/artifact/bundles/glassfish.zip
  export GF_HOME=$WORKSPACE
  export ANT_HOME=/home/jgallimore/Apps/apache-ant-1.10.5
  export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
  export PATH=$JAVA_HOME/bin:$ANT_HOME/bin/:$PATH
  $WORKSPACE/docker/build_jakartaeetck.sh

Substitute in your path for JAVA_HOME and ANT_HOME as appropriate. The TCK takes around an hour to build.

Once that is complete, unzip the TCK zip file somewhere on your file system. Where and how you set this up is all down to personal preference, but I like to create a ee8tck folder under ~/dev and have both the TCK
and Glassfish in this folder:

TCK_HOME => /Users/jgallimore/ee8tck/javaeetck
RI_HOME  => /Users/jgallimore/ee8tck/glassfish5

You'll then need to add Apache Ant to the TCK:

mkdir -p $TCK_HOME/tools/ant
cp -R $ANT_HOME $TCK_HOME/tools/ant

NOTE: I'm hoping we can eliminate this step (copying Ant) in the coming days.

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
             <javaee8.cts.home>/Users/jgallimore/dev/ee8tck/javaeetck</javaee8.cts.home>
             <javaee8.ri.home>/Users/jgallimore/dev/ee8tck/glassfish5/glassfish</javaee8.ri.home>
          </properties>
        </profile>
      </profiles>
    </settings>

TEST RUN

To complete a test run against the latest TomEE 8.0.0-SNAPSHOT, from the tomee-tck folder, run

  ./runtests --web tomee-plume com.sun.ts.tests.ejb30.bb.localaccess.statelessclient

MISC

 The target directory is not cleaned out at the beginning of a test
 run.  There are a few thousand tests and sometimes multiple
 executions are required to get complete results.  It's also nice to
 be able to look back on older log files when tracking down and fixing
 bugs that the tests uncover.

 Bottom line is you have to clear out the target directory manually.
 On occasion some bad state will get into the server install in the
 target/ directory.  If you start getting weird maven or groovy
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

   target/apache-tomee-plume-8.0.0-SNAPSHOT/logs
   target/apache-tomee-plume-8.0.0-SNAPSHOT/logs

SELECTING TESTS

 It is possible to select whole groups of tests or even individual
 tests.  The following are all valid ways to select which tests you'd
 like to run.

   ./runtests --web tomee-plume -c com.sun.ts.tests.ejb30 com.sun.ts.tests.ejb
   ./runtests --web tomee-plume -c com.sun.ts.tests.ejb30.lite.stateful.concurrency.accesstimeout
   ./runtests --web tomee-plume -c com.sun.ts.tests.ejb30.lite.stateful.concurrency.accesstimeout.annotated
   ./runtests --web tomee-plume -c com.sun.ts.tests.ejb30.lite.stateful.concurrency.accesstimeout.annotated.Client#beanClassLevel_from_ejbembed

 The first command runs of the ejb30 and ejb sections of the TCK
 illustrating that it is possble to run many sections or tests at
 once.  The very last line shows the syntax for running just one
 specific test.

 Note that the output of the tck shows which exact tests are being
 run.  For example:

   ...[tck output]...
    com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejbembed - FAILED
    com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejblitejsf - PASSED
    com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejblitejsp - PASSED
    com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejbliteservlet - PASSED
    com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejbliteservlet2 - PASSED
    com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel2_from_ejbembed - FAILED
   ....

 For the most part, you can copy and paste that test name as-is and use
 it to run a test that failed... with one slight adjustment.  You need
 to delete the "#java" part and then it will work.

 BAD

   ./runtests --web tomee-plume com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejbembed

 GOOD

   ./runtests --web tomee-plume com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#beanClassLevel_from_ejbembed

WHAT NEXT

 Getting from zero to passing is a long road.  Failures and the
 overall progress tends to go in three stages:

    1. setup issues -- the right things are not where they need to be

    2. missing features -- a key feature is missing causing failures
       in unrelated tests.
    
    3. compliance issues -- legitimate failures.

 During phase 1 there will be big jumps in numbers. It is best to
 clear out as much of phase 1 as possible before moving on to any
 issues of phase 2 or 3.

 During phase 2 it becomes apparent that many tests fail simply
 because of an unrelated feature that many tests use, such as global
 jndi support.  As these features are added, the tests that still fail
 are usually failing for more legitimate reasons -- actual compliance
 issues -- this is phase 3.

 Phase 3 takes the longest and is often the hardest.  Unlike phase 1
 or 2, the time spent debugging and fixing a test usually only results
 in one or two more passing tests.  It is also common that fixing a
 specific test requires reworking part of the code.  This inevitably
 results in "two steps forward, one step backward" and other tests
 might fail because of the change.  This is normal. It is also the
 reason why there should be no more phase 1 or 2 style issues, so that
 it is possible to see the regressions.  Working on phase 3 style
 issues while there are still phase 1 and 2 style issues is a little
 bit like working blind.  You don't really know how many steps
 backward you might be taking as a result of a change.  It can be
 done, but it is risky.

WORKING TOGETHER
 Communication:- 
 -Email:Make use of dev@tomee.apache.org

 We want to divide and conquer on each phase and clear it out as much
 as possible before moving to the next one.  We could possibly get up
 to 80% passing before reaching phase 3.

 So the name of the game is "call your shot" or "name it and claim
 it."  Find an issue that affects as many tests as possible and post
 that you are working on it so others know not to look into it as
 well.

 If you get busy or stuck, no problem, just post again to let others
 know the issue is up for grabs.  This is also normal.  Taking a quick
 peak and then realizing that the issue involves someone else's area
 of expertise is common.  Even if you aren't able to fix something,
 taking a look and reporting as much info as you can is incredibly
 valuable.  It's all part of the certification dance and will ideally
 happen very often -- the right people working on the right things
 gets you certified much faster.

 There are usually so many issues that finding the right one for you
 is somewhat like sifting through a pile of legos looking for that
 perfect piece.  It doesn't always fit -- chuck it back and look for
 another one.
