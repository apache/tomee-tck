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

/**
 * Generates the reverse tests for both jaxws and jws.
 *
 * @version $Rev: 3415 $ $Date: 2011-07-24 21:14:49 -0700 (Sun, 24 Jul 2011) $
 */
class ReverseSetupCommand
    extends CommandSupport
{
    def ReverseSetupCommand(source) {
        super(source)
    }

    def execute() {
        def javaeeCtsHome = requireDirectory('jakartaee.cts.home')

        def tsant = new TsAntCommand(this)
        tsant.props['build.vi'] = true

        def setupReverseTests = { section ->

            tsant.workingDirectory = "$javaeeCtsHome/src/com/sun/ts/tests/$section"

            // Generate the reverse tests for this section just once they haven't already been generated
            def reverseGeneratedFile = new File("${javaeeCtsHome}/reverse_${section}_generated")
            if (!reverseGeneratedFile.exists()) {

                log.info("Setting up reverse tests for: $section")
                tsant.execute(['clean', 'build'])

                // Create flag to indicate processing is complete
                ant.touch(file: reverseGeneratedFile)
            }
            else {
                log.info("Reverse tests already created for: $section")
            }
        }

        // Default to both jaxws and jws generation
        def sections = [ 'jaxws', 'jws' ]

        // If -Dsection was given, then use that instead
        def section = get('section')
        if (section) {
            sections = [ section ]
        }

        // Setup the rerverse tests for each section
        sections.each {
            setupReverseTests(it)
        }
    }
}
