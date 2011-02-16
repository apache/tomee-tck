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

/**
 * Helper to handle Ansi or non-ansi messages.
 *
 * @version $Rev: 1772 $ $Date: 2007-01-10 11:04:11 -0800 (Wed, 10 Jan 2007) $
 */
class Messages
{
    static def passed() {
        if (AnsiColors.SUPPORTED) {
            print(AnsiColors.SPECIAL['bright'])
            print(AnsiColors.BACKGROUND['black'])
            print(AnsiColors.FOREGROUND['green'])
        }

        print("PASSED")

        if (AnsiColors.SUPPORTED) {
            print(AnsiColors.CLEAR)
        }
    }

    static def failed() {
        if (AnsiColors.SUPPORTED) {
            print(AnsiColors.SPECIAL['bright'])
            print(AnsiColors.BACKGROUND['black'])
            print(AnsiColors.FOREGROUND['red'])
        }

        print("FAILED")

        if (AnsiColors.SUPPORTED) {
            print(AnsiColors.CLEAR)
        }
    }

    static def error() {
        if (AnsiColors.SUPPORTED) {
            print(AnsiColors.SPECIAL['bright'])
            print(AnsiColors.BACKGROUND['black'])
            print(AnsiColors.FOREGROUND['red'])
        }

        print("ERROR")

        if (AnsiColors.SUPPORTED) {
            print(AnsiColors.CLEAR)
        }
    }
}
