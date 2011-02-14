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

package org.apache.openejb.cts;

import com.sun.ts.lib.porting.TSLoginContextInterface;
import org.apache.openejb.client.ClientSecurity;

public class LoginContextImpl implements TSLoginContextInterface {
    // This method is used for login with username and password.
    //
    // @param usr - string username
    // @param pwd - string password
    public void login(String usr, String pwd) throws Exception {
        System.err.println("login(String usr, String pwd) " + usr + ", " + pwd);

        ClientSecurity.login(usr, pwd);
    }

    // This login method is used for Certificate based login
    //
    // Note: This method also uses keystore and keystore password from
    //       the TS configuration file
    //
    // @param alias - alias is used to pick up the certificate from keystore
    public void login(String alias) throws Exception {
        System.err.println("login(String alias) " + alias);
    }

    // This login method is used for Certificate based login
    //
    // @param alias - alias is used to pick up the certificate from keystore
    // @param keystore - keystore file
    // @param keyPass - keystore password
    public void login(String alias, String keystore, String keyPass) throws Exception {
        System.err.println("login(String alias, String keystore, String keyPass) " + alias + ", " + keystore + ", " + keyPass);
    }

    public Boolean logout() {
        System.err.println("logout()");
        ClientSecurity.logout();
        return true;
    }
}
