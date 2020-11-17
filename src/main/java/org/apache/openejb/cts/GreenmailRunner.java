/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.cts;

import com.icegreen.greenmail.standalone.GreenMailStandaloneRunner;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.log4j.Level;
import org.apache.openejb.testng.PropertiesBuilder;
import org.apache.openejb.util.Join;
import org.apache.openejb.util.Log4jPrintWriter;

import java.net.InetAddress;
import java.util.Properties;

public class GreenmailRunner {

    private static class GreenmailThread extends Thread {
        private static final int SLEEP_INTERVAL = 60000;
        private final int smtp;
        private final int imap;
        private GreenMailStandaloneRunner greenMailStandaloneRunner;

        public GreenmailThread(final int smtp, final int imap) {
            this.smtp = smtp;
            this.imap = imap;
        }

        public void run() {
            System.out.println(String.format("Starting greenmail with imap port %s and smtp port %s", imap, smtp));
            try {

                final Properties properties = new PropertiesBuilder()
                        .p("greenmail.verbose", "true")
                        .p("greenmail.setup.test.smtp", "true")
                        .p("greenmail.setup.test.imap", "true")
                        .p("greenmail.auth.disabled", "true")
                        .p("greenmail.users", "foo@foo.com")
                        .build();


                greenMailStandaloneRunner = new GreenMailStandaloneRunner();
                greenMailStandaloneRunner.doRun(properties);

            } catch (final Exception e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (final InterruptedException e) {
                    break;
                }
            }

            System.out.println("Embedded greenmail thread stopping");
        }

    }


    public static void main(final String[] args) {
        int smtp = 25;
        int imap = 143;
        if (args.length == 2) {
            try {
                smtp = Integer.parseInt(args[0]);
                imap = Integer.parseInt(args[1]);
            } catch (final NumberFormatException e) {
                System.out.println(String.format("Could not convert ports %s. Using the default smtp %s and imap %s",
                        Join.join(", ", args), smtp, imap));
            }
        }
        final GreenmailThread thread = new GreenmailThread(smtp, imap);
        thread.setDaemon(true);
        thread.setName("GreenmailServerDaemon");
        thread.start();
    }
}
