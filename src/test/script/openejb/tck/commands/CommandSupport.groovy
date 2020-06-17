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

import openejb.tck.util.PathBuilder
import openejb.tck.util.TestListBuilder

/**
 * Support for commands
 *
 * @version $Revision$ $Date$
 */
abstract class CommandSupport {
    def source

    def log

    def project

    def ant = new AntBuilder()

    protected CommandSupport(source) {
        assert source != null

        this.source = source

        // Extract components from the source script class
        this.log = source.log
        this.project = source.project

        this.project.properties.putAll(System.getProperties())

        // Drop the default listener and replace with a maven log adapter
        initAntLogging()
    }

    private initAntLogging() {
        def p = ant.antProject

        p.getBuildListeners().each {
            p.removeBuildListener(it)
        }

        def adapter = new MavenAntLoggerAdapter(log)
        adapter.emacsMode = true
        if (log.debugEnabled) {
            adapter.messageOutputLevel = p.MSG_VERBOSE
        } else {
            adapter.messageOutputLevel = p.MSG_INFO
        }
        p.addBuildListener(adapter)
    }

    // Can't use getProperty() as that messes up the GroovyObject
    def get(name) {
        assert name != null

        def value = project.properties.getProperty(name)

        log.info("Get property: $name=$value")

        return value
    }

    def get(name, defaultValue) {
        def value = get(name)

        if (value == null) {
            value = defaultValue
        }

        return value
    }

    def getBoolean(name, defaultValue) {
        def value = get(name, defaultValue)
        return Boolean.valueOf("$value")
    }

    def getInteger(name, defaultValue) {
        def value = get(name, defaultValue)
        return Integer.parseInt("$value")
    }

    def require(name) {
        assert name != null

        log.info("Require property: $name")

        //
        // NOTE: Need to check project and system properties, as when setting -Dprop=foo
        //       on the command-line m2 will set System properties not project properties.
        //
        if (!project.properties.containsKey(name) && !System.properties.containsKey(name)) {
            throw new Exception("Missing required property: $name")
        }

        //
        // NOTE: Use getProperty() so that defaults (system properties) will get applied
        //       for some reason properties[name] does not resolve defaults :-(
        //
        def value = get(name)

        //
        // Treat "null" the same as unset
        //
        if (value == 'null') {
            throw new Exception("Missing required property: $name (resolved to null)")
        }

        return value
    }

    File requireDirectory(name) {
        def dir = require(name)

        ensureDirectory(dir)

        return new File(dir).canonicalFile
    }

    def ensureDirectory(dirname) {
        assert dirname

        def dir = dirname
        if (!(dir instanceof File)) {
            dir = new File(dirname)
        }
        if (!dir.exists()) {
            throw new Exception("Required directory does not exist: $dir")
        }

        if (!dir.isDirectory()) {
            throw new Exception("File exists but directory was expected: $dir")
        }
    }

    def initPaths() {
        def openejbHome = get('openejb.home')
        def javaeetckHome = get('jakartaee.cts.home')
        def javaeeRiHome = get('jakartaee.ri.home')
        def webcontainer = get('webcontainer')
        def tomeeHttpPort = get('webcontainer.default.port')

        def openejbLib
        def openejbServerUri
        if (webcontainer.startsWith("tom")) {
            openejbLib = "${openejbHome}/lib"
            openejbServerUri = "http://localhost:${tomeeHttpPort}/tomee/ejb"
        } else {
            openejbLib = "${openejbHome}/lib"
            openejbServerUri = "ejbd://localhost:4201"
        }
        project.properties.setProperty("openejb.lib", openejbLib)
        project.properties.setProperty("openejb.server.uri", openejbServerUri)

        dumpLibs(new File(openejbLib))

        //
        // Setup Ant paths required to run the TCK and make sure they have the expected number
        // of elements so we can safley avoid using version numbers
        //

        def builder

        // openejb.porting.classes -- porting impl and deps
        builder = new PathBuilder(this)
        builder.directory = "${project.build.directory}"
        builder.append("openejb-tck-*.jar")
        builder.directory = openejbLib



        builder.appendAll("openejb-core-*.jar")
        builder.directory = "${project.build.directory}/lib"
        builder.appendAll("*.jar")
        builder.getPath("openejb.porting.classes")

        // openejb.jee.classes  -- spec classes used for
        // signature tests
        builder = new PathBuilder(this)
        builder.directory = openejbLib
        builder.appendAll("geronimo-*_spec-*.jar")
        builder.append("javaee-api-*.jar")
        builder.getPath("openejb.jee.classes")

        // ts.run.classpath - used to run the appclient
        builder = new PathBuilder(this)
        builder.reference("openejb.jee.classes")
        builder.reference("openejb.porting.classes")
        builder.directory = openejbLib
        builder.appendAll("commons-logging-*.jar")

        if (get("webcontainer").equals("tomee-plume")) {
            builder.appendAll("eclipselink-*.jar")
        } else {
            builder.appendAll("openjpa-*.jar")
        }

//        builder.append("hsqldb-*.jar")
        builder.append("derby-*.jar")
        builder.append("derbyclient-*.jar")
        builder.append("openejb-client*.jar")
        builder.directory = "${javaeetckHome}/lib"
        builder.append("javatest.jar")
        builder.append("tsharness.jar")
        builder.append("cts.jar")
        builder.append("dbprocedures.jar")
        builder.append("commons-httpclient*.jar")
        builder.append("jdom-1.1.3.jar")
        //builder.append("dom4j.jar")
        builder.append("jaxb-api.jar")
        builder.append("jaxb-impl.jar")
        builder.append("jaxb-xjc.jar")
        builder.directory = "${openejbHome}/lib"
        builder.append("jasper-el.jar")
        
        // for CXF JAX-RS client
        builder.append("cxf-rt-rs-client-*.jar")
        builder.append("cxf-rt-transports-http-*.jar")
        builder.append("cxf-core-*.jar")
        builder.append("woodstox-core-*.jar")
        builder.append("stax2-api-*.jar")
        builder.append("xmlschema-core-*.jar")
        builder.append("cxf-rt-frontend-jaxrs-*.jar")

        // for jonzon
        builder.appendAll("johnzon-*.jar")

        builder.getPath("ts.run.classpath")
        // ts.harness.classpath
        builder = new PathBuilder(this)
        builder.reference('ts.run.classpath')
        builder.directory = "$javaeetckHome/lib"
        builder.append("apiCheck.jar")
        builder.append("sigtest.jar")
        builder.directory = "$javaeetckHome/tools/ant/lib"
        builder.append("ant.jar")
        builder.append("ant-launcher.jar")
        // builder.append("ant-nodeps.jar") - this seems to have disappeared with EE7
        builder.directory = "$javaeeRiHome/lib"
        builder.append("appserv-rt.jar")
        builder.getPath('ts.harness.classpath')

        // ts.run.classpath - used to run the appclient
        builder = new PathBuilder(this)
        builder.reference("openejb.jee.classes")
        builder.directory = "${project.build.directory}"
        builder.append("openejb-tck-*.jar")
        builder.directory = "${project.build.directory}/lib"
        builder.append("openejb-lite*.jar")
        builder.directory = "${javaeetckHome}/lib"
        builder.append("javatest.jar")
        builder.append("tsharness.jar")
        builder.append("cts.jar")
        builder.append("commons-httpclient*.jar")
        builder.append("jdom-*.jar")
        //builder.append("dom4j.jar")
        builder.append("jaxb-api.jar")
        builder.append("jaxb-impl.jar")
        builder.append("jaxb-xjc.jar")
        builder.directory = openejbLib
        builder.append("derby-*.jar")
        builder.append("derbyclient-*.jar")
        if (get("webcontainer").equals("tomee-plume")) {
            builder.appendAll("eclipselink-*.jar")
        }

        builder.getPath("openejb.embedded.classpath")
    }

    def selectTests() {
        def builder = new TestListBuilder(this)
        return builder.getTests()
    }

    def dumpLibs(lib) {
        if (project.properties.containsKey("openejb.dump.libraries")) {
            return;
        }

        log.info("TCK pom version: " + require('jakartaee.tck.version'))
        log.info("Start - Container libraries")
        if (lib.exists() && lib.isDirectory()) {
            lib.listFiles().grep(~/.*.jar/).sort { it.name }.each {
                log.info(" - $it.name")
            }
        }
        log.info("End - Container libraries")

        project.properties.setProperty("openejb.dump.libraries", "done")
    }
}
