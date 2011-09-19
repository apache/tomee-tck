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
import java.sql.DriverManager

/**
 * Setup the environment for running the TCK.
 *
 * @version $Revision$ $Date$
 */
class SetupCommand
    extends CommandSupport
{
    def SetupCommand(source) {
        super(source)
    }
    
    def generateTsJte() {
        def javaeeCtsHome = requireDirectory('javaee.cts.home')

        // Install the Javatest testsuite environment configuration, first filter our custom properties
        // then merge those properties with the javaee ts.jte
        ant.copy(todir: "${project.build.directory}", filtering: true, overwrite: true) {
            fileset(dir: "${project.basedir}/src/test/resources") {
                include(name: 'testsuite.properties')
            }

            filterset(begintoken: '%', endtoken: '%') {
                def map = [:]

                def paths = [
                    'ts.run.classpath',
                    'ts.run.classpath.ri.suffix',
                    'ts.harness.classpath',
                    'openejb.embedded.classpath',
                    'geronimo.specs.classpath',
                    'geronimo.porting.classes'
                ]

                paths.each{ name ->
                    map[name] = getReferenceAsString(name)
                }

                map.putAll(System.properties)
                map.putAll(project.properties)


        		// Set the profile to test. Options are:  web, full
                def options = get('options', '').tokenize(',')
                boolean javaeeFullProfile = true
                if (options.contains('web-profile')) {
                    javaeeFullProfile = false
                }
                if (javaeeFullProfile) {
                    log.info("Setting javaee.level=full")
                    map['javaee.level'] = 'full'
                }
                if (!javaeeFullProfile) {
                    log.info("Setting javaee.level=web")
                    map['javaee.level'] = 'web'
                }

                // map['servlet_adaptor'] = 'org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet'
                map['servlet_adaptor'] = 'org.apache.openejb.server.rest.OpenEJBRestServlet'
                map['jaxrs_impl_name'] = 'cxf'

                map['basedir'] = project.basedir

                // Need to sanatize some properties which reference files or paths when running Windows... :-(
                if (SystemUtils.IS_OS_WINDOWS) {
                    def vars = [
                        'basedir',
                        'openejb.home',
                        'openejb.embedded.classpath',
                        'javaee.home',
                        'javaee.ri.home',
                        'javaee.home.ri',
                        'ts.run.classpath',
                        'ts.run.classpath.ri.suffix',
                        'ts.harness.classpath',
                    ]

                    vars.each {
                        def value = map[it]
                        if (value) {
                            map[it] = "$value".replace('\\', '/')
                        }
                    }
                }

                map.each {
                    filter(token: it.key, value: it.value)
                }
            }
        }

        // Make a backup of the original ts.jte file
        def originalFile = createOriginalFile("${javaeeCtsHome}/bin/ts.jte.orig", "${javaeeCtsHome}/bin/ts.jte")

        // Load the original props
        def props = loadProps(originalFile)

        // Load our custom props
        def customProps = loadProps("${project.build.directory}/testsuite.properties")

        // Merge custom w/original, custom taking precedence
        props.putAll(customProps)

        // Save the "real" ts.jte used to execute tests
        storeProps(props, "${project.build.directory}/ts.jte")

        // Dump the props as rendered above
        if (log.debugEnabled) {
            log.debug 'TS environment:'
            file.eachLine {
                log.debug "    $it"
            }
        }
    }

    def generateSigMap() {

        // Modify the sig-test_se6.map file to use the proper signature files.
        // Create backups first.
        def javaeeCtsHome = requireDirectory('javaee.cts.home')

        // Backup the original sig-test_se6.map, load in the new props, and create
        // the modified file
        def originalSe6File = createOriginalFile("${javaeeCtsHome}/bin/sig-test_se6.map.orig", "${javaeeCtsHome}/bin/sig-test_se6.map")

        // Load original properties
        def props = loadProps(originalSe6File)

        // Load custom properties
        def customProps = loadProps("${project.basedir}/src/test/resources/signature_se6.properties")

        // Merge the differences
        props.putAll(customProps)

        // Save the new properties file
        storeProps(props, "${javaeeCtsHome}/bin/sig-test_se6.map")
    }

    def createOriginalFile(newFileName, oldFileName) {

        def originalFile = new File(newFileName)

        if (!originalFile.exists()) {
            def file = new File(oldFileName)
            assert file.exists() : "Missing original file: $oldFileName"
            ant.copy(file: file, tofile: originalFile)

            // Make it harder to alter the original
            ant.chmod(perm: 'a-w', file: originalFile)
        }

        return originalFile
    }

    def loadProps(file) {
        file = new File("$file")

        def props = new Properties()
        def input = file.newInputStream()
        try {
            props.load(input)
        }
        finally {
            input.close()
        }

        return props
    }

    def storeProps(props, file) {
        file = new File("$file")
        def output = file.newOutputStream()
        try {
            props.store(output, null)
        }
        finally {
            output.close()
        }
    }

    def getReferenceAsString(name) {
        return ant.getAntProject().getReference(name).toString()
    }
    
    def execute() {
        log.info("Setting up TCK environment...")
        
        // Esnure that openejb.home is now set to a directory

        def openejbHome = requireDirectory('openejb.home')

        initPaths()

        def javaeeCtsHome = get('javaee.cts.home')

        ant.mkdir(dir: "${openejbHome}/logs")
        ant.mkdir(dir: "${openejbHome}/apps")

        generateTsJte()

        generateSigMap()

        // Install the latest excludes
        ant.copy(todir: "${project.build.directory}") {
            fileset(dir: "${project.basedir}/src/test/resources") {
                include(name: 'ts.jtx')
            }
        }

        //
        // NOTE: This file *must* be placed in ${javaeeCtsHome.home}/bin, bin/tsant
        //       loads this automagically, and uses it to replace values.
        //
        ant.copy(
            file: "${project.build.directory}/ts.jte",
            tofile: "$javaeeCtsHome/bin/ts.jte",
            overwrite: true
        )

        def jaxrs = get('jaxrs')

        if(jaxrs.equals('true')){

            ant.mkdir(dir: "$javaeeCtsHome/bin/xml/impl/openejb")
            ant.copy(todir: "$javaeeCtsHome/bin/xml/impl/openejb") {
            fileset(dir: "${project.basedir}/src/test/resources/jaxrs") {
                include(name: 'cxf.xml')
            }
          }

            log.info("generating jaxrs tck dist artifacts based on vi setting")
            TsAntCommand command = new TsAntCommand(this)
            command.workingDirectory = "${javaeeCtsHome}/bin"
            command.execute('update.jaxrs.wars')

        }


        //
        // Copy openejb configuration files into server
        //
        ant.copy(todir: "${openejbHome}", overwrite: true) {
            if ("tomcat".equals(System.getProperty("webcontainer"))) {
                fileset(dir: "${project.basedir}/src/test/tomcat")
            } else if ("tomee".equals(System.getProperty("webcontainer"))) {
                fileset(dir: "${project.basedir}/src/test/tomee")
            } else {
                fileset(dir: "${project.basedir}/src/test/openejb")
            }
        }

        //
        // Copy openejb-tck jar to server lib
        // this jar contains the DriverXADataSource needed for the xa tests
        //
        ant.copy(todir: "${openejbHome}/lib", overwrite: true) {
            fileset(dir: "${project.basedir}/target") {
                include(name: "openejb-tck-*.jar")
            }
        }

        //
        // Copy the clientcert.jks keystore to the conf dir
        //
        ant.copy(
            file: "${project.basedir}/src/test/keystores/clientcert.jks",
            todir: "${openejbHome}/conf"
        )
        ant.copy(
            file: "${project.basedir}/src/test/keystores/ssl-truststore",
            todir: "${openejbHome}/conf"
        )
		
        def connector = get('connector')

        if(connector.equals('true')){
            //
            // deploy the connectors
            //
            ant.copy(todir: "${openejbHome}/apps", overwrite: true) {
                fileset(dir: "${javaeeCtsHome}/dist/com/sun/ts/tests/common/connector/whitebox") {
                    include(name: "old-dd-whitebox*.rar")
                    include(name: "whitebox*.rar")
                }
                fileset(dir: "${javaeeCtsHome}/dist/com/sun/ts/tests/common/connector/whitebox/annotated") {
                    include(name: "whitebox*.rar")
                }
                fileset(dir: "${javaeeCtsHome}/dist/com/sun/ts/tests/common/connector/whitebox/ibanno") {
                    include(name: "whitebox*.rar")
                }
                fileset(dir: "${javaeeCtsHome}/dist/com/sun/ts/tests/common/connector/whitebox/mdcomplete") {
                    include(name: "whitebox*.rar")
                }
                fileset(dir: "${javaeeCtsHome}/dist/com/sun/ts/tests/common/connector/whitebox/mixedmode") {
                    include(name: "whitebox*.rar")
                }
                fileset(dir: "${javaeeCtsHome}/dist/com/sun/ts/tests/common/connector/whitebox/multianno") {
                    include(name: "whitebox*.rar")
                }
            }
        }

        //
        // NOTE: This tsant script only needs to be run once with each new CTS image or
        //       when the DB is changed from Derby to something else. It copies tssql.stmt
        //       containing the correct DML to ${j2eetckHome}/bin where it must reside.
        //       It also updates the jstl wars as necessary for the DB selected.
        //
        // Process the dml for derby just once if it hasn't already been done
        def dmlProcessedFile = new File("${javaeeCtsHome}/dml_processed")
        if (!dmlProcessedFile.exists()) {
            log.info("Processing DML for Derby")

            // Ant task only works if sql file is in the correct place
            ant.mkdir(dir: "${javaeeCtsHome}/sql/derby")
            ant.copy( toDir: "${javaeeCtsHome}/sql/derby", overwrite: true ) {
                fileset(dir: "${project.basedir}/src/test/sql/derby") {
                    include(name: "derby.dml*.sql")
                }
            }

            // Configure CTS using Derby dml
            TsAntCommand command = new TsAntCommand(this)
            command.props['target.dml.file'] = 'tssql.stmt'
            command.props['dml.file'] = 'derby/derby.dml.sql'
            command.workingDirectory = "${javaeeCtsHome}/bin"
            command.execute('copy.dml.file')

            // Create flag to indicate processing is complete
            ant.touch(file: dmlProcessedFile)

        } else {
            log.info("DML already processed for Derby")
        }

        //
        // Backup previous logs (maybe)
        //
        
        def logOutputDirectory = get('logOutputDirectory', "${project.build.directory}/logs")
        def backupLogs = get('backupLogs', true)
        if (backupLogs) {
            def dir = new File("${logOutputDirectory}")
            if (dir.exists()) {
                def timestamp = System.currentTimeMillis()
                def logBackupDir = "${logOutputDirectory}-${timestamp}"
                log.info("Backing up previous logs to: ${logBackupDir}")
                
                ant.mkdir(dir: logBackupDir)
                ant.move(todir: logBackupDir) {
                    fileset(dir: logOutputDirectory) {
                        include(name: '**')
                    }
                }
            }
        }
        ant.mkdir(dir: logOutputDirectory)
		
		//
		// Stop any previous running copy of Derby, from SQL setup
		//
		//log.info("Attempting to stop Derby embedded database...")
		//Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
		//DriverManager.getConnection("jdbc:derby:${openejbHome}/data/derbydb;user=cts;password=cts;shutdown=true")
		//log.info("Derby embedded database stopped.")
    }
}
