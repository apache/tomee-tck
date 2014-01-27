GETTING SETUP

 This document and the OpenEJB TCK setup can be checked out from here:

  svn co https://svn.apache.org/repos/tck/openejb-tck

 The Java EE 6 TCK and RI can be downloaded from here:

  svn export https://svn.apache.org/repos/tck/sun-tcks/javaee/6/2012-01-24/javaeetck-6.0_24-Jan-2012.zip
  svn export https://svn.apache.org/repos/tck/sun-tcks/javaee/6/2012-01-24/javaee6u4_ri-3.1.2-b21.zip

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

 From inside the openejb-tck/trunk/ directory, a command like this
 will get you a little taste of running the TCK:

  ./runtests --web tomee com.sun.ts.tests.ejb30.bb.localaccess.statelessclient

 We don't yet pass all of the TCK, but the above tests should be among
 the passing sections and are a good way to validate all is setup
 properly.

TOMCAT

 By default the `runtests` command will execute the tests against an
 OpenEJB standalone server.

 To run in Tomcat, a command like the following will work:

  ./runtests --tomcat-version 7.0.8 com.sun.ts.tests.ejb30.bb.localaccess.statelessclient

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

   target/openejb-4.0.0-SNAPSHOT/logs/openejb.log
   target/apache-tomcat-7.0.8/logs/openejb.log

SELECTING TESTS

 It is possible to select whole groups of tests or even individual
 tests.  The following are all valid ways to select which tests you'd
 like to run.

   ./runtests -tv 7.0.8 -c com.sun.ts.tests.ejb30 com.sun.ts.tests.ejb
   ./runtests -tv 7.0.8 -c com.sun.ts.tests.ejb30.lite.stateful.concurrency.accesstimeout
   ./runtests -tv 7.0.8 -c com.sun.ts.tests.ejb30.lite.stateful.concurrency.accesstimeout.annotated
   ./runtests -tv 7.0.8 -c com.sun.ts.tests.ejb30.lite.stateful.concurrency.accesstimeout.annotated.Client#beanClassLevel_from_ejbembed

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

 For the most part, you can copy and past that test name as-is and use
 it to run a test that failed... with one slight adjustment.  You need
 to delete the "#java" part and then it will work.

 BAD

   ./runtests -tv 7.0.8 com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#java#beanClassLevel_from_ejbembed

 GOOD

   ./runtests -tv 7.0.8 com/sun/ts/tests/ejb30/lite/stateful/concurrency/accesstimeout/annotated/Client#beanClassLevel_from_ejbembed

THE ROAD TO CERTIFICATION

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
 -Email:Send an email to tck-subscribe@openejb.apache.org  to subscribe to the tck discussion list
 -IRC: You can also jump on #tck channel at freenode

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

LEGAL CONSTRAINTS

 We are not allowed to publicly give specific information about the
 TCK and our status.  When referring publicly to the TCK we can only
 give essentially "binary" information, do we pass or not.  Publicly
 saying which sections we pass and how much is generally not allowed.

 Talking publicly about the specific details of a compliance issue is
 fine as long as the TCK and any TCK identifying information is not
 mentioned.  Publicly mentioning TCK test names, class names or
 sharing stack traces is not allowed.  That information should be
 posted to the TCK list.  All other information can be discussed
 publicly.

 For example, saying "I noticed we have an issue with X part of the
 specification under Y conditions" is OK.  Saying "Test X fails" is
 not.  When in doubt, be a little generic.

 All that said, we are an open source project and we should strive to
 be as public as possible and communicate as much as we can on the dev
 list, in our JIRA and in our commit messages.  It is an uncomfortable
 reality that the driver of much development (the TCK) is private yet
 the development itself is public.  The more we share publicly, the
 less the project seems driven by an invisible force.

