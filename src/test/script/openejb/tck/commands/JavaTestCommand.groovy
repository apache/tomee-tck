/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package openejb.tck.commands

import org.apache.commons.lang.SystemUtils
import org.apache.commons.lang.time.StopWatch

import org.apache.geronimo.cts.mavenplugins.j2eetck.report.ReportFileLocator
import org.apache.geronimo.cts.mavenplugins.j2eetck.report.ReportTestCaseLoader
import openejb.tck.util.Messages
import openejb.tck.util.OutputScanner

/**
 * Executes JavaTest to run the TCK tests.
 *
 * @version $Revision$ $Date$
 */
class JavaTestCommand
    extends CommandSupport
{
    def JavaTestCommand(source) {
        super(source)
    }
    
    def execute() {
        if (selectTests().size() == 0) {
            throw new Exception("Must specify at least one test to execute")
        }

        def tests = require('tests').tokenize(",")
        
        def tsant = new TsAntCommand(this)
        
        //
        // NOTE: Special handling for some interop tests
        //
        
        if (tests.contains("interop-csiv2")) {
            if (tests.size() != 1) {
                throw new Exception("The 'interop-csiv2' tests must be run alone")
            }
            
            tsant.execute("enable.csiv2")
            
            javatest()
            
            tsant.execute("disable.csiv2")
        }
        else if (tests.contains("interop-tx")) {
            if (tests.size() != 1) {
                throw new Exception("The 'interop-tx' tests must be run alone")
            }
            
            def workingDir = "${project.build.directory}/tck-work"
            def txDir = "${workingDir}/com/sun/ts/tests/interop/tx"
            
            tsant.execute("disable.ri.tx.interop")
            
            javatest()
            
            ant.mkdir(dir: "${txDir}/interop")
            ant.move(todir: "${txDir}/interop", overwrite: true) {
                fileset(dir: txDir) {
                    exclude(name: "interop/**")
                    exclude(name: "nointerop/**")
                }
            }
            
            tsant.execute("enable.ri.tx.interop")
            
            javatest()
            
            ant.mkdir(dir: "${txDir}/nointerop")
            ant.move(todir: "${txDir}/nointerop", overwrite: true) {
                fileset(dir: txDir) {
                    exclude(name: "interop/**")
                    exclude(name: "nointerop/**")
                }
            }
        }
        else {
            javatest()
        }
    }
    
    def line() {
        println()
        println("=" * 79)
        println()
    }

    def javatest() {
        log.info("Executing JavaTest...")
        
        initPaths()
        
        def tests = selectTests()
        println(tests)


        def timeout = getInteger('timeout', -1)
        def logOutput = getBoolean('logOutput', false)
        def logOutputDirectory = get('logOutputDirectory', "${project.build.directory}/logs")
        def logFile = "${logOutputDirectory}/javatest.log"
        
        def openejbHome = require('openejb.home')
        def javaeeCtsHome = require('cts.home')
        def javaeeRiHome = require('ri.home')
        def workingDir = "${project.build.directory}/tck-work"
        
        // Define a list of options to enable
        def options = get('options', "").tokenize(',')
        
        // If we are logging output then start the scanner to show progress
        def outputScanner
        if (logOutput) {
            //
            // FIXME: Need to allow appending he log for special interop tests which run
            //        javatest() more than once
            //
            
            // Make sure we start with an empty log
            ant.delete(file: logFile)
            
            outputScanner = new OutputScanner(logFile);
            outputScanner.start()
        }
        
        //
        // HACK: For some reason, need to quote JAVA_HOME on Windows...
        //
        def javaHome = require('java.home')
        // JRG: I commented this out, as this appeared to append the content of the JAVA_HOME environment variable to the current working directory
        // on Windows 11 for me. 2023-08-07
        //if (SystemUtils.IS_OS_WINDOWS) {
        //    javaHome = "${javaHome}"
        //}
        
        ant.mkdir(dir: workingDir)
        
        // Make sure we start out with a fresh state
        ant.delete() {
            fileset(dir: workingDir) {
                include(name: "**")
            }
        }
        
        // Track how long the run takes
        def watch = new StopWatch()
        watch.start()

        try {
            ant.java(classname: "com.sun.javatest.tool.Main", failonerror: false, fork: "yes", maxmemory: "150m") {
                //
                // NOTE: Get a reference to the current node so we can conditionally set attributes
                //
                def node = current.wrapper

                // Force a higher stack size to avoid StackOverflowError for classpath building if run with Java 11 (default: 256kb)
                jvmarg(value: '-Xss4m');

                // Maybe set timeout
                if (timeout > 0) {
                    log.info("Timeout after: ${timeout} seconds");
                    def millis = timeout * 1000
                    node.setAttribute('timeout', "${millis}")
                }

                // Maybe enable output logging
                if (logOutput) {
                    log.info("Redirecting output to: ${logFile}")
                    redirector(output: logFile)
                }

                classpath(refid: "ts.harness.classpath")

                if (options.contains('javatest-debug')) {
                    log.info("Enabling debug options")

                    jvmarg(value: '-Xdebug')
                    jvmarg(value: '-Xnoagent')
                    jvmarg(value: '-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5004')
                    jvmarg(value: '-Djava.compiler=NONE')
                }

                if (options.contains('debug')) {
                    log.info("Enabling server debug options")
                    jvmarg(value: '-Dopenejb.server.debug=true')
                }

                def tckJavaHome = get('tck.java.home')
                if (tckJavaHome != null) {
                    log.info("Using java home (javatest) ${tckJavaHome}")
                    jvmarg(value: "-Dtck.java.home=${tckJavaHome}")
                } else {
                    tckJavaHome = javaHome
                }

                def tckJavaVersion = get('tck.java.version')
                if (tckJavaVersion != null) {
                    log.info("Using java version (javatest) ${tckJavaVersion}")
                    jvmarg(value: "-Dtck.java.version=${tckJavaVersion}")
                }

                def tckJavaOpts = get('tck.java.opts')
                if (tckJavaOpts != null) {
                    log.info("Using java home (javatest) ${tckJavaOpts}")
                    jvmarg(value: "-Dtck.java.opts=${tckJavaOpts}")
                }

                def containerJavaHome = get('container.java.home')
                if (containerJavaHome != null) {
                    log.info("Using java home (container) ${containerJavaHome}")
                    jvmarg(value: "-Dcontainer.java.home=${containerJavaHome}")
                } else {
                    containerJavaHome = javaHome
                }

                def containerJavaVersion = get('container.java.version')
                if (containerJavaVersion != null) {
                    log.info("Using java version (container) ${containerJavaVersion}")
                }

                def containerJavaOpts = get('container.java.opts', "")

                // http://openjdk.java.net/jeps/252
                // need to set back java locale to COMPAT,SPI for Java 9 and above
                // tomcat requires add-opens to function properly as well
                def matches = containerJavaVersion ==~ /1[0-9]/;
                if (matches || new File(containerJavaHome as String, 'jmods').exists()) {
                    containerJavaOpts += " -Djava.locale.providers=COMPAT,SPI"
                    containerJavaOpts += " --add-opens=java.base/java.lang=ALL-UNNAMED"
                    containerJavaOpts += " --add-opens=java.base/java.io=ALL-UNNAMED"
                    containerJavaOpts += " --add-opens=java.base/java.util=ALL-UNNAMED"
                    containerJavaOpts += " --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
                    containerJavaOpts += " --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED"
                }


                // not sure about this ....
//                if (tckJavaHome == null || !new File(tckJavaHome as String, 'jmods').exists()/*j9 doesnt support it*/) {
//                    sysproperty(key: "java.endorsed.dirs", file: "${javaeeRiHome}/lib/endorsed")
//                    sysproperty(key: "command.testExecute.endorsed.dir", value: "-Djava.endorsed.dirs=${javaeeCtsHome}/endorsedlib")
//                    sysproperty(key: "command.testExecuteEjbEmbed.endorsed.dir", value: "-Djava.endorsed.dirs=${openejbHome}/endorsed")
//
//                    containerJavaOpts += " -Djava.locale.providers=COMPAT"
//                }

                // force memory on tasks because with JDK 8 it's computed with a bit too much
                // containerJavaOpts += " -Xmx512m -Dtest.ejb.stateful.timeout.wait.seconds=60"

                if (options.contains('security')) {
                    log.info("Enabling server security manager")

                    // -Djava.security.properties=conf/security.properties
                    containerJavaOpts += " -Djava.security.manager -Dcts.home=${javaeeCtsHome} -Djava.security.debug=none " +
                            "-Djava.security.policy=${project.basedir}/${openejbHome}/conf/catalina.policy " +
                            "-Djava.security.properties=${project.basedir}/${openejbHome}/conf/security.properties"
                }
                if (options.contains('websocket')) {
                    log.info("Enabling Tomcat WebSockets configuration")
                    containerJavaOpts += " -Dorg.apache.tomcat.websocket.DISABLE_BUILTIN_EXTENSIONS=true " +
                            "-Dorg.apache.tomcat.websocket.ALLOW_UNSUPPORTED_EXTENSIONS=true " +
                            "-Dorg.apache.tomcat.websocket.DEFAULT_PROCESS_PERIOD=0"
                }
                if (containerJavaOpts != null) {
                    log.info("Using java opts (container) ${containerJavaOpts}")
                    jvmarg(value: "-Dcontainer.java.opts=${containerJavaOpts}")
                }

                /*
                if (containerJavaOpts != null &&
                        (containerJavaVersion.startsWith("9") || containerJavaVersion.startsWith("1.9")
                                || containerJavaVersion.startsWith("10")  || containerJavaVersion.startsWith("11") )) {

                    def modulesOptions = "-Dcontainer.java.opts=" +
                            "-Dopenejb.deployer.jndiname=openejb/DeployerBusinessRemote " +
                            "--add-opens java.base/java.net=ALL-UNNAMED " +
                            "--add-opens java.base/java.lang=ALL-UNNAMED " +
                            "--add-modules java.xml.bind,java.corba"

                    log.info("Java modules detected - overriding java options for container with ${modulesOptions}.")
                    jvmarg(value: modulesOptions)
                }
                */

                sysproperty(key: "user.language", value: 'en')
                sysproperty(key: "user.country", value: 'US')

                sysproperty(key: "openejb.server.uri", value: require('openejb.server.uri'))
                sysproperty(key: "openejb.servicemanager.enabled", value: true)

                sysproperty(key: "server.shutdown.port", value: require('webcontainer.default.shutdown.port'))
                sysproperty(key: "openejb.home", file: openejbHome)
                sysproperty(key: "com.sun.ejb.home", file: openejbHome)
                sysproperty(key: "TS_HOME", file: javaeeCtsHome)
                sysproperty(key: "ts.home", file: javaeeCtsHome)
                sysproperty(key: "J2EE_HOME", file: openejbHome)
                sysproperty(key: "JAVA_HOME", file: javaHome)
                sysproperty(key: "cts.jtroutput", value: true)
                sysproperty(key: "cts.harness.debug", value: require('cts.harness.debug'))
                sysproperty(key: "javatest.security.allowPropertiesAccess", value: true)
                sysproperty(key: "java.security.policy", file: "${javaeeRiHome}/bin/harness.policy")
                sysproperty(key: "J2EE_HOME_RI", file: javaeeRiHome)
                sysproperty(key: "deliverable.class", value: require('deliverable.class'))
                sysproperty(key: "com.sun.enterprise.home", file: javaeeRiHome)
                sysproperty(key: "com.sun.aas.installRoot", file: javaeeRiHome)
                sysproperty(key: "DEPLOY_DELAY_IN_MINUTES", value: require('deploy_delay_in_minutes'))

                sysproperty(key: "java.protocol.handler.pkgs", value: 'javax.net.ssl')
                sysproperty(key: "javax.net.ssl.keyStorePassword", value: 'changeit')
                sysproperty(key: "javax.net.ssl.keyStore", file: "${project.basedir}/src/test/keystores/clientcert.jks")
                sysproperty(key: "javax.net.ssl.trustStore", file: "${project.basedir}/src/test/keystores/ssl-truststore")
                sysproperty(key: "javax.net.ssl.trustStorePassword", file: "changeit")

                if (options.contains('appclient-debug')) {
                    log.info("Enabling appclient-debug options")

                    // See testsuite.properties command.testExecuteAppClient for usage
                    sysproperty(key: 'command.testExecuteAppClient.debugopts', value: '-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5003')
                }

                //
                // TODO: Add support for setting command.testExecute.debugopts
                //

                if (options.contains('harness-debug')) {
                    log.info("Enabling harness-debug options")

                    // See testsuite.properties command.testExecute for usage
                    sysproperty(key: 'command.testExecute.debugopts', value: '-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5002')
                }

                if (options.contains('embedded-debug')) {
                    log.info("Enabling embedded-debug options")

                    // See testsuite.properties command.testExecute for usage
                    sysproperty(key: 'command.testExecuteEmbedded.debugopts', value: '-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5001')
                }

                if (SystemUtils.IS_OS_WINDOWS) {
                    sysproperty(key: "windir", value: System.getenv('windir'))
                    sysproperty(key: "SYSTEMROOT", value: System.getenv('SystemRoot'))
                }

                sysproperty(key: "bin.dir", value: "${javaeeCtsHome}/bin")

                // Include system properties
                arg(value: "-EsysProps")

                arg(value: "-batch")

                arg(value: "-testsuite")
                arg(file: "${javaeeCtsHome}/src")

                arg(value: "-workDir")
                arg(file: workingDir)

                arg(value: "-envFiles")
                arg(file: "${project.build.directory}/ts.jte")

                arg(value: "-env")
                if (SystemUtils.IS_OS_WINDOWS) {
                    arg(value: "ts_win32")
                }
                else {
                    arg(value: "ts_unix")
                }

                arg(value: "-excludeList")
                arg(file: "${project.build.directory}/ts.jtx")

                arg(value: "-timeoutFactor")
                arg(value: "10.0")

                arg(value: "-priorStatus")
                arg(value: "pass,fail,error,notRun")

                arg(value: "-tests")

                tests.each {
                    log.info("Including tests: ${it}")
                    arg(value: it)
                }

                arg(value: "-runtests")

                //
                // HACK: Some pre-running feedback (have to include this in the java closure)
                //
                log.info("Running tests...")
                log.info("> JavaTest Java Version: ${tckJavaVersion}")
                log.info("> JavaTest Java Home: ${tckJavaHome}")
                log.info("> Container Java Version: ${containerJavaVersion}")
                log.info("> Container Java Home: ${containerJavaHome}")

                line()
            }
        } catch (Exception e) {
            log.warn("JavaTest returned non-zero status (see logs/javatest.log) for details: $e")
        }

        watch.stop()
        if (logOutput) {
            outputScanner.shutdown()
        }

        // Display the test summary
        line()
        
        def dir = new File(workingDir)
        def locator = new ReportFileLocator(dir);
        def loader = new ReportTestCaseLoader(dir, true, locator);
        def testcases = loader.loadTestCases();
        def count = testcases.size()
        def passed = 0
        def failed = 0
        def errors = 0

        testcases.each {
            if (it.failed) {
                failed++
            }
            else if (it.error) {
                errors++
            }
            else {
                passed++
            }

            // Only show the summary when we are *not* logging
            if (!logOutput) {
                print('    ')

                if (it.error) {
                    Messages.error()
                }
                else if (it.failed) {
                    Messages.failed()
                }
                else {
                    Messages.passed()
                }

                println(" - ${it.className}#${it.name}")
            }
        }

        // Complain if nothing was run
        if (passed + failed + errors == 0) {
            log.warn('No tests were ran')
        }

        if (!logOutput) {
            println()
        }

        println("Completed running ${count} tests (${watch}):")
        println()
        println("    Passed: $passed")
        println("    Failed: $failed")
        println("    Errors: $errors")

        // Dump out a summary file so we can use that to act upon the results easily in the automated muck
        def props = new Properties()
        //props.setProperty('server', "$webcontainer")
        props.setProperty('tests', "$tests")
        props.setProperty('count', "$count")
        props.setProperty('passCount', "$passed")
        props.setProperty('failureCount', "$failed")
        props.setProperty('errorCount', "$errors")
        props.setProperty('passed', "${failed + errors == 0}")

        def output = new File("${project.build.directory}/summary.properties").newOutputStream()
        try {
            props.store(output, null)
        }
        finally {
            output.close()
        }

        line()
    }
}
