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

package util

import commands.CommandSupport

/**
 * Builds a list of testnames suitable for passing to JavaTest.
 *
 * @version $Revision$ $Date$
 */
class TestListBuilder
    extends CommandSupport
{
    def testList = []

    def testSections

    public TestListBuilder(source) {
        super(source)

        // Load the optional test sections properties
        testSections = loadTestSections()
    }

    def loadTestSections() {
        def testSections = new Properties()

        def testSectionsFile = get('testSectionsFile')

        if (testSectionsFile != null) {
            log.debug("Using test sections from file: ${testSectionsFile}")

            def file = new File(testSectionsFile)
            if (!file.exists()) {
                throw new Exception("Missing test sections file: ${file}")
            }

            def input = file.newInputStream()
            testSections.load(input)
            input.close()

            if (log.isDebugEnabled()) {
                log.debug("Using test sections:")
                testSections.each {
                    log.debug("    ${it.key}: ${it.value}")
                }
            }
        }
        else {
            log.warn("Test sections were not configured")
        }

        return testSections
    }

    def addTest(testname) {
        assert testname != null

        testname = testname.trim()

        // Resolve test sections to test classes
        if (testSections.containsKey(testname)) {
            def section = testSections[testname]
            log.debug("Resolved test section '${testname}' to: ${section}")

            // Sections could contain a list
            def tests = section.tokenize(",")
            tests.each {
                addTest(it)
            }
        }
        else {
            // Prefix package if the test is not fully qualified
            if (testname.startsWith('/')) {
                testname = "com/sun/ts/tests${testname}"
            }

            // Translate "." to "/"
            testname = testname.replace((char)'.', (char)'/')

            testList.add(testname)
        }
    }

    def getTests() {
        // Define a list of tests to execute
        def tests = require('tests').tokenize(',')

        tests.each {
            addTest(it)
        }

        if (log.isDebugEnabled()) {
            log.debug("Selected tests:")
            testList.each {
                log.debug("    ${it}")
            }
        }

        return testList
    }
}
