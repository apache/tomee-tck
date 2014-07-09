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

import com.sun.ts.lib.porting.TSHttpsURLConnectionInterface;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class HTTPSURLConnectionImpl implements TSHttpsURLConnectionInterface {
    private HttpsURLConnection httpsURLConnection = null;

    public void init(final URL url) throws IOException {
        httpsURLConnection = (HttpsURLConnection) url.openConnection();
        httpsURLConnection.setHostnameVerifier(new MyHostNameVerifier());
    }

    public void setDoInput(final boolean doInput) {
        httpsURLConnection.setDoInput(doInput);
    }

    public void setDoOutput(final boolean doOutput) {
        httpsURLConnection.setDoOutput(doOutput);
    }

    public void setRequestProperty(final String key, final String value) {
        httpsURLConnection.setRequestProperty(key, value);
    }

    public void setUseCaches(final boolean usecaches) {
        httpsURLConnection.setUseCaches(usecaches);
    }

    public InputStream getInputStream() throws IOException {
        return httpsURLConnection.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return httpsURLConnection.getOutputStream();
    }

    public String getHeaderField(final String name) {
        return httpsURLConnection.getHeaderField(name);
    }

    public String getHeaderField(final int num) {
        return httpsURLConnection.getHeaderField(num);
    }

    public void disconnect() {
        httpsURLConnection.disconnect();
    }

    public static class MyHostNameVerifier implements HostnameVerifier {
        public boolean verify(final String urlhostname, final String certHostName) {
            return true;
        }

        public boolean verify(final String urlhostName, final SSLSession sslSession) {
            return true;
        }
    }
}
