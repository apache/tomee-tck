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

import com.sun.ts.lib.deliverable.DeliverableFactory;
import com.sun.ts.lib.deliverable.PropertyManagerInterface;
import com.sun.ts.lib.deliverable.PropertyNotSetException;
import com.sun.ts.lib.implementation.sun.javaee.runtime.web.SunWebApp;
import com.sun.ts.lib.porting.DeploymentInfo;
import com.sun.ts.lib.porting.TSDeploymentException;
import org.apache.openejb.config.RemoteServer;
import org.apache.openejb.cts.deploy.TSDeploymentInterface2;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.apache.openejb.assembler.Deployer.ALT_DD;

@SuppressWarnings("unchecked")
public class DeploymentImpl implements TSDeploymentInterface2 {
    private static final String HEAD = "OpenEJB - ";
    private static final String FILENAME = "filename";
    private static final String PATH_SEP = System.getProperty("path.separator", ":");
    private static final String CLIENT_MAIN = "org.apache.openejb.client.Main";

    static {

        Properties overrides = new Properties();
        String containerJavaHome = System.getProperty("container.java.home");
        String containerJavaVersion = System.getProperty("container.java.version");
        String containerJavaOpts = System.getProperty("container.java.opts", "-Dopenejb.deployer.jndiname=openejb/DeployerBusinessRemote");
        // String containerJavaOpts = System.getProperty("container.java.opts", "-Djava.security.properties=conf/security.properties -Dopenejb.deployer.jndiname=openejb/DeployerBusinessRemote");
        if (containerJavaVersion != null) {
            overrides.put("java.version", containerJavaVersion);
        }

        if (containerJavaHome != null) {
            overrides.put("java.home", containerJavaHome);
        }

        if (containerJavaOpts != null) {
            overrides.put("java.opts", containerJavaOpts);
        }

        final RemoteServer remoteServer = new RemoteServer(overrides, 250, true);
        remoteServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                remoteServer.destroy();
            }
        });
    }

    private PrintWriter log;
    private File libDir;
    private File appsDir;
    private StringBuilder classpathBuilder = new StringBuilder();

    public void init(final PrintWriter log) {
        this.log = log;

        final PropertyManagerInterface propMgr;
        try {
            propMgr = DeliverableFactory.getDeliverableInstance().getPropertyManager();

            final String openejbHomeName = propMgr.getProperty("openejb.home");
            if (openejbHomeName == null) {
                throw new IllegalStateException("Not initialized; missing property: geronimo.porting.planDir");
            }
            final File openejbHome = new File(openejbHomeName).getCanonicalFile();
            System.setProperty("openejb.home", openejbHome.getAbsolutePath());
            appsDir = new File(openejbHome, "apps");
            libDir = new File(openejbHome, "lib");

            try {
                final String openejbUri = propMgr.getProperty("openejb.server.uri");
                System.setProperty("openejb.uri", openejbUri);
            } catch (final PropertyNotSetException e) {
                //Ignore
            }

            try {
                final String value = propMgr.getProperty("ts.run.classpath");
                System.setProperty("ts.run.classpath", value);
            } catch (final PropertyNotSetException e) {
                //Ignore
            }

            this.log.println(HEAD + "Initialized Deployment helper");
        } catch (final Exception e) {
            this.log.println(HEAD + "ERROR initializing DeploymentImpl");
            e.printStackTrace(this.log);
            throw new AssertionError(e);
        }
    }

    public Hashtable getDependentValues(final DeploymentInfo[] infoArray) {
        return new Hashtable();
    }

    public InputStream getDeploymentPlan(final DeploymentInfo info) throws TSDeploymentException {
        classpathBuilder = new StringBuilder();
        final String earPath = info.getEarFile();
        final String earDir = earPath.substring(0, earPath.lastIndexOf('.'));
        try {
            JarFile jarFile = new JarFile(earPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().startsWith("lib") && jarEntry.getName().endsWith(".jar")) {
                    if (classpathBuilder.toString().isEmpty()) {
                        classpathBuilder.append(String.format("%s%s", earDir, File.separator + jarEntry.getName()));
                        continue;
                    }

                    if (!classpathBuilder.toString().contains(jarEntry.getName())) {
                        classpathBuilder.append(String.format("%s%s%s", PATH_SEP, earDir, jarEntry.getName()));
                    }
                }
            }
        } catch (IOException e) {
            // do nop
        }
        if (earPath == null) {
            throw new TSDeploymentException("EarFile is null");
        }
        log.println(HEAD + "module: " + earPath);

        final Properties properties = new Properties();
        properties.put(FILENAME, earPath);

        final Set<String> moduleIds = new TreeSet<String>();
        moduleIds.addAll(info.getWebRuntimeData().keySet());
        moduleIds.addAll(info.getEjbRuntimeData().keySet());
        moduleIds.addAll(info.getAppRuntimeData().keySet());
        moduleIds.addAll(info.getAppClientRuntimeData().keySet());

        for (final String path : info.getRuntimeFiles()) {
            final String earName = earName(path, earPath);
            if (earName != null && path.contains(earName)) {
                final String name = path.substring(path.indexOf(earName) + earName.length() + 1);
                properties.put(ALT_DD + "/" + name, path);
            } else {
                final String fileName = new File(path).getName();
                for (final String moduleId : moduleIds) {
                    if (fileName.startsWith(moduleId)) {
                        final String name = fileName.substring(moduleId.length() + 1);
                        properties.put(ALT_DD + "/" + moduleId + "/" + name, path);

                        // hack when deployer is used to deploy war in a standalone manner and not within an ear file
                        // in that case, the altDD above are not read and webapp context is not overridden by the sun web.xml
                        final Object webAppObject = info.getWebRuntimeData().get(moduleId);
                        if (webAppObject != null) {
                            final SunWebApp sunWebApp = SunWebApp.class.cast(webAppObject);
                            properties.put("webapp." + moduleId + ".context-root", sunWebApp.getContextRoot());
                        }
                    }
                }
            }
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            properties.store(out, "Auto Generated Deployment Plan");
        } catch (final IOException e) {
            throw new TSDeploymentException("Unable to create deployment plan", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String earName(final String path, final String earPath) {
        String earName = earPath;
        int end = earName.indexOf(".ear");
        if (end > 0) {
            end += 4; // .ear

            int start;
            int currentIdx = 0;
            do {
                start = currentIdx;
                currentIdx = earName.indexOf("/", currentIdx + 1);
            } while (currentIdx < end - 1 && currentIdx > start);

            earName = earName.substring(start + 1, end);
            return earName;
        }
        return null;
    }

    public Target[] getTargetsToUse(final Target[] targets, final DeploymentInfo info) {
        return new Target[]{targets[0]};
    }

    public String getAppClientArgs(final Properties p) {
        final String executeArgs = p.getProperty("executeArgs");
        final String clientname = p.getProperty("client_name");
        String earFile = p.getProperty("ear_file");
        earFile = new File(earFile).getName();
        earFile = earFile.substring(0, earFile.lastIndexOf('.'));
        final File appClientJar = new File(new File(appsDir, earFile), clientname + ".jar");

        if (!classpathBuilder.toString().contains(appClientJar.getAbsolutePath())) {
            classpathBuilder.append(String.format("%s%s", PATH_SEP, appClientJar.getAbsolutePath()));
        }

        final String property = System.getProperty("ts.run.classpath");
        if (!classpathBuilder.toString().contains(property)) {
            classpathBuilder.append(String.format("%s%s", PATH_SEP, property));
        }
//        for (int i = 0; i < libDir.listFiles().length; i++) {
//            File file = libDir.listFiles()[i];
//            if (file.getName().endsWith(".jar")) {
//                classPath += PATH_SEP + file.getAbsolutePath();
//            }
//        }

        // lib/ directory if exists
        /*File file = new File(p.getProperty("ear_file"));
        if (file.getName().endsWith("ar") && !file.isDirectory() && file.exists()) {
            JarInputStream jarFile = null;
            try {
                jarFile = new JarInputStream(new FileInputStream(file));
                ZipEntry entry;
                byte[] buf = new byte[1024];
                int n;
                while ((entry = jarFile.getNextEntry()) != null) {
                    if (entry.getName().startsWith("lib/") && entry.getName().endsWith(".jar")) {
                        File extracted = File.createTempFile("ext", ".jar");
                        extracted.deleteOnExit();

                        FileOutputStream fos = new FileOutputStream(extracted);
                        while ((n = jarFile.read(buf, 0, 1024)) > -1) {
                            fos.write(buf, 0, n);
                        }
                        fos.close();

                        classPath += PATH_SEP + extracted.getPath();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        // no-op
                    }
                }
            }
        } else if (file.isDirectory()) {
            File libDir = new File(file,  "lib");
            if (libDir.exists()) {
                for (File lib : libDir.listFiles()) {
                    if (lib.getName().endsWith(".jar")) {
                        classPath += PATH_SEP + lib.getPath();
                    }
                }
            }
        }*/

        return "-cp " + classpathBuilder.toString() + " -Dopenejb.client.moduleId=" + clientname + " " + CLIENT_MAIN + " " + executeArgs;
    }

    public String getClientClassPath(final TargetModuleID[] targetIDs, final DeploymentInfo info, final DeploymentManager manager) throws TSDeploymentException {
        return "";
    }

    public void createConnectionFactory(final TargetModuleID[] targetIDs, final Properties p) throws TSDeploymentException {
    }

    public void removeConnectionFactory(final TargetModuleID[] targetIDs, final Properties p) throws TSDeploymentException {
    }

    public void postDistribute(final ProgressObject po) {
        final TargetModuleID moduleID = po.getResultTargetModuleIDs()[0];
        log.println(HEAD + "Distribute returned moduleID " + moduleID.getModuleID());
    }

    public void postStart(final ProgressObject po) {
    }

    public void postStop(final ProgressObject po) {
    }

    public void postUndeploy(final ProgressObject po) {
    }
}
