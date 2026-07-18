/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.tck.cdi.tomee;

import org.apache.openejb.core.ivm.IntraVmCopyMonitor;
import org.apache.openejb.core.ivm.IntraVmProxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * TomEE-aware implementation of the CDI TCK {@code org.jboss.cdi.tck.spi.Beans}
 * porting SPI, ported verbatim from the apache/tomee tck/cdi-tomee runner.
 *
 * <p>It extends the plain OpenWebBeans {@code BeansImpl} so that
 * {@link #isProxy(Object)} also recognizes TomEE's {@code IntraVmProxy}, and
 * so passivation drives {@code IntraVmCopyMonitor} and deserializes with a
 * thread-context-classloader-aware {@link ObjectInputStream}. The plain OWB
 * implementation does not know about the intra-VM proxies TomEE injects, so
 * passivation-capability probes (e.g. {@code ImplicitEventTest}) misbehave
 * without it.
 *
 * <p>This class ships inside every deployed TCK archive through
 * {@code org.jboss.cdi.tck.libraryDirectory}; the {@code org.apache.openejb.*}
 * classes it references are resolved from the TomEE server class loader at
 * runtime.
 */
public class BeansImpl extends org.apache.webbeans.test.tck.BeansImpl {
    @Override
    public boolean isProxy(final Object instance) {
        return instance instanceof IntraVmProxy || super.isProxy(instance);
    }

    @Override
    public byte[] passivate(Object instance) throws IOException {
        IntraVmCopyMonitor.prePassivationOperation();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(instance);
            os.flush();
            return baos.toByteArray();
        } finally {
            IntraVmCopyMonitor.postPassivationOperation();
        }
    }

    @Override
    public Object activate(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream is = new BeanObjectInputStream(bais);
        return is.readObject();
    }


    public static final class BeanObjectInputStream extends ObjectInputStream {
        public BeanObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        protected Class resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(classDesc.getName(), false, getClassloader());
            } catch (ClassNotFoundException e) {
                String n = classDesc.getName();
                if (n.equals("boolean")) return boolean.class;
                if (n.equals("byte")) return byte.class;
                if (n.equals("char")) return char.class;
                if (n.equals("short")) return short.class;
                if (n.equals("int")) return int.class;
                if (n.equals("long")) return long.class;
                if (n.equals("float")) return float.class;
                if (n.equals("double")) return double.class;

                throw e;
            }
        }

        protected Class resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
            Class[] cinterfaces = new Class[interfaces.length];
            for (int i = 0; i < interfaces.length; i++)
                cinterfaces[i] = getClassloader().loadClass(interfaces[i]);

            try {
                return Proxy.getProxyClass(getClassloader(), cinterfaces);
            } catch (IllegalArgumentException e) {
                throw new ClassNotFoundException(null, e);
            }
        }

        ClassLoader getClassloader() {
            return Thread.currentThread().getContextClassLoader();
        }
    }

}
