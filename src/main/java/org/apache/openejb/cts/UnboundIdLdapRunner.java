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

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import org.apache.openejb.util.Join;

public class UnboundIdLdapRunner {

    private static class LdapThread extends Thread {
        private static final int SLEEP_INTERVAL = 60000;
        private final String ldifFile;
        private final int port;

        public LdapThread(final String ldifFile, final int port) {
            this.ldifFile = ldifFile;
            this.port = port;
        }

        public void run() {
            System.out.println(String.format("Starting LDAP server with file %s and port %s", ldifFile, port));

            InMemoryDirectoryServer ldapServer = null;
            try {
                final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=net");
                final InMemoryListenerConfig listenerConfig = new InMemoryListenerConfig(
                    "LdapForSecurityAPI",
                    null,
                    port,
                    null,
                    null,
                    null);

                config.setListenerConfigs(listenerConfig);
                ldapServer = new InMemoryDirectoryServer(config);

                ldapServer.importFromLDIF(true, new LDIFReader(ldifFile));
                ldapServer.startListening();

            } catch (final Exception ex) {
                throw new IllegalStateException(ex);
            }

            while (true) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (final InterruptedException e) {
                    break;
                }
            }

            System.out.println("Embedded ldap thread stopping");
        }

    }

    public static void main(String[] args) {
        int port = 11389;
        String ldifFile = null;
        if (args.length == 1) { // it's the file
            ldifFile = args[0];
        }
        if (args.length == 2) { // first is URL and second is port
            try {
                port = Integer.parseInt(args[1]);

            } catch (final NumberFormatException e) {
                System.out.println(String.format("Could not convert ports %s. Using the default port %s",
                                                 Join.join(", ", args), port));
            }
        }

        if (ldifFile == null) {
            throw new IllegalArgumentException("LDIF file is required as first argument");
        }

        final UnboundIdLdapRunner.LdapThread thread = new UnboundIdLdapRunner.LdapThread(ldifFile, port);
        thread.setDaemon(true);
        thread.setName("LdaplServerDaemon");
        thread.start();
    }
}
