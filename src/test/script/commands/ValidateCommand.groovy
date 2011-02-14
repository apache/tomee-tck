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

package commands

import org.apache.commons.lang.SystemUtils

/**
 * Validate the TCK environment.
 *
 * @version $Revision$ $Date$
 */
class ValidateCommand
    extends CommandSupport
{
    def ValidateCommand(source) {
        super(source)
    }
    
    def logProperties(props, header) {
        assert props
        assert header

        if (log.debugEnabled) {
            log.debug("${header}:")

            props.each {
                log.debug("    ${it.key}=${it.value}")
            }
        }
    }
    
    def execute() {
        log.info("Validating TCK environment...")
        
        if (SystemUtils.IS_OS_WINDOWS) {
            log.warn("Detected evil operating system; beware of failures caused by your operating system\'s stupidity")
        }
        
        // Spit out some debug information
        if (log.isDebugEnabled()) {
            logProperties(System.properties, "System")
            logProperties(project.properties, "Project")
        }
        
        // Esnure that javaee.cts.home and javaee.ri.home are set to valid directories
        ['javaee.cts.home', 'javaee.ri.home'].each {
            def dir = requireDirectory(it)

            //
            // TODO: Push these back into the project props as canonical paths
            //

            log.info("Using $it: $dir")
        }
        
    }
}


















