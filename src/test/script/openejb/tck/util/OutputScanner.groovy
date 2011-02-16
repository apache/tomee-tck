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
 * Provides a primitive progress output for JavaTest by scanning the log file
 * for test state.
 *
 * @version $Rev: 1883 $ $Date: 2007-02-10 17:37:01 -0800 (Sat, 10 Feb 2007) $
 */
class OutputScanner
    implements Runnable
{
    def File file

    def running = false

    def hasInput = true

    def Thread thread

    public OutputScanner(filename) {
        assert filename

        this.file = new File("${filename}")
    }

    def start() {
        running = true
        thread = new Thread(this, 'Output Scanner')
        thread.start()
    }

    def stop() {
        if (running) {
            running = false
            thread.interrupt()
            thread.join()
        }
    }

    def shutdown() {
        //
        // HACK: Might not have had time to create the file yet... so sleep a bit first
        //
        sleep(2000)

        while (hasInput) {
            sleep(1000)
        }

        stop()
    }

    def sleep(ms) {
        try {
            Thread.sleep(ms)
        }
        catch (InterruptedException e) {
            // ignore
        }
    }

    public void run() {
        Reader reader

        while (running) {
            if (reader == null) {
                if (!file.exists()) {
                    sleep(1000)
                }
                else {
                    reader = file.newReader()
                }
            }
            else {
                def line = reader.readLine()

                if (line != null) {
                    if (line.startsWith('Beginning Test:')) {
                        def testname = "${line.substring(17,line.length())}"

                        // Replace last "." with "#"
                        def tmp = testname.reverse()
                        tmp = tmp.replaceFirst(/\./, '#')
                        testname = tmp.reverse()

                        print("    ${testname} - ")
                    }
                    else if (line.startsWith('Finished Test:')) {
                        def passfail = 'UNKNOWN'

                        if (line.contains('PASSED.')) {
                            Messages.passed()
                            println()
                        }
                        else if (line.contains('FAILED.')) {
                            Messages.failed()
                            println()
                        }
                        else if (line.contains('ERROR.')) {
                            Messages.error()
                            println()
                        }
                        else {
                            println('????')
                        }
                    }
                }
                else {
                    hasInput = false

                    sleep(1000);
                }
            }
        }
    }
}