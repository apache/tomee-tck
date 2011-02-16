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

/**
 * Executes the TCK's Ant script (tsant).
 */
class TsAntCommand
    extends CommandSupport
{
    def buildFile

    def workingDirectory

    def props = [:]

    def TsAntCommand(source) {
        super(source)
    }

    def execute() {
        def goal = require('goal')

        execute(goal)
    }

    def execute(String goal) {
        execute([ goal ])
    }

    def execute(List goals) {
        def javaeeCtsHome = requireDirectory('javaee.cts.home')

        log.info("Executing 'tsant' goals: $goals")

        def script = "${javaeeCtsHome}/tools/ant/bin/ant"

        if (SystemUtils.IS_OS_WINDOWS) {
            script = "${script}.bat"
        }
        else if (SystemUtils.IS_OS_UNIX) {
            // Make sure the file is executable
            ant.chmod(file: script, perm: 'a+rx')

            // Make sure the underlying ant script is also executable
//            ant.chmod(file: "$javaeeCtsHome/tools/ant/bin/ant", perm: 'a+rx')
        }

        if (!workingDirectory) {
            workingDirectory = javaeeCtsHome
        }
        ensureDirectory(workingDirectory)

        // Hack for Windows hanging on 'tsant start.appserver' calls
        def antSpawn = false
        if (SystemUtils.IS_OS_WINDOWS) {
            antSpawn = true
        }

        ant.exec(executable: script, dir: workingDirectory, spawn: antSpawn) {
            env(key: 'TS_HOME', file: javaeeCtsHome)

            if (buildFile) {
                log.debug("Using build file: $buildFile")
                arg(value: '-f')
                arg(file: buildFile)
            }

            if (props) {
                props.each { key, value ->
                    arg(value: "-D${key}=${value}")
                }
            }

            goals.each { goal ->
                arg(value: goal)
            }
        }

        // Hack for Windows hanging on 'tsant start.appserver' calls
        if (SystemUtils.IS_OS_WINDOWS) {
            log.info("Sleeping 30 secs. for spawned 'tsant' goals: $goals")
            ant.sleep(seconds: 30)
        }
    }
}
