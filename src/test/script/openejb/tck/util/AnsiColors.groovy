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

package openejb.tck.util

import jline.Terminal

/**
 * Defines ANSI color codes.
 *
 * @version $Rev: 1772 $ $Date: 2007-01-10 11:04:11 -0800 (Wed, 10 Jan 2007) $
 */
class AnsiColors {
    // Ask Jline to see if ANSI is supported
    static def isSupported() {
        // Hack seems like setting jline.terminal (as documented) to unsupported is not
        // enought get isANSISupported() to return false

        //
        // NOTE: Maybe because -D for mvn is not actually setting a System property?
        //

        def ttype = System.getProperty("jline.terminal")
        if (ttype == "jline.UnsupportedTerminal") {
            return false
        } else {
            return Terminal.getTerminal().isANSISupported()
        }
    }

    static def SUPPORTED = isSupported()

    static def BACKGROUND = [
            black  : "\033[40m",
            red    : "\033[41m",
            green  : "\033[42m",
            yellow : "\033[43m",
            blue   : "\033[44m",
            magenta: "\033[45m",
            cyan   : "\033[46m",
            white  : "\033[47m"
    ]

    static def FOREGROUND = [
            black  : "\033[30m",
            red    : "\033[31m",
            green  : "\033[32m",
            yellow : "\033[33m",
            blue   : "\033[34m",
            magenta: "\033[35m",
            cyan   : "\033[36m",
            white  : "\033[37m"
    ]

    static def SPECIAL = [
            reset    : "\033[0m",
            bright   : "\033[1m",
            dim      : "\033[2m",
            underline: "\033[4m",
            blink    : "\033[5m",
            reverse  : "\033[7m",
            hidden   : "\033[8m"
    ]

    static def CLEAR = SPECIAL['reset']
}
