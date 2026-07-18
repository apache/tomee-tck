/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.debugging;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jakarta Debugging Support for Other Languages 2.0 TCK.
 *
 * <p>The TCK ships a JSP test application ({@code testclient.war}) and the
 * {@code VerifySMAP} tool. A compatible implementation must produce a valid
 * SMAP (JSR-45 source map) for every translated JSP. This runner compiles
 * the test application offline with the Jasper JSP compiler bundled in the
 * TomEE distribution under test — SMAP generation and SMAP dumping enabled,
 * exactly what the container does for a deployed page — and then validates
 * both the dumped {@code .smap} files and the {@code SourceDebugExtension}
 * attribute embedded in the generated class files with {@code VerifySMAP}.
 */
class DebuggingTckTest {

    private static Path outputDir;

    @BeforeAll
    static void compileTestClientJsps() throws Exception {
        final Path tomeeDir = Path.of(System.getProperty("debugging.tomee.dir"));
        final Path testClientDir = Path.of(System.getProperty("debugging.testclient.dir"));
        outputDir = Path.of(System.getProperty("debugging.output.dir"));
        Files.createDirectories(outputDir);

        final List<URL> serverJars;
        try (Stream<Path> tree = Files.walk(tomeeDir)) {
            serverJars = tree.filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .map(DebuggingTckTest::toUrl)
                    .toList();
        }
        assertFalse(serverJars.isEmpty(), "no jars staged from the TomEE distribution under " + tomeeDir);

        final ClassLoader serverLoader = new URLClassLoader(
                serverJars.toArray(new URL[0]), DebuggingTckTest.class.getClassLoader());
        final Thread thread = Thread.currentThread();
        final ClassLoader previousLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(serverLoader);
        try {
            final Class<?> jspcClass = serverLoader.loadClass("org.apache.jasper.JspC");
            final Object jspc = jspcClass.getConstructor().newInstance();
            jspcClass.getMethod("setUriroot", String.class).invoke(jspc, testClientDir.toString());
            jspcClass.getMethod("setOutputDir", String.class).invoke(jspc, outputDir.toString());
            jspcClass.getMethod("setCompile", boolean.class).invoke(jspc, true);
            jspcClass.getMethod("setFailOnError", boolean.class).invoke(jspc, true);
            // What the container does for debuggable pages: generate the
            // SMAP, install it into the class file, and dump it to disk.
            jspcClass.getMethod("setSmapSuppressed", boolean.class).invoke(jspc, false);
            jspcClass.getMethod("setSmapDumped", boolean.class).invoke(jspc, true);
            jspcClass.getMethod("execute").invoke(jspc);
        } finally {
            thread.setContextClassLoader(previousLoader);
        }
    }

    @Test
    void helloJspClassFileContainsValidSmap() throws Exception {
        verifySmap(outputDir.resolve("org/apache/jsp/Hello_jsp.class"));
    }

    @Test
    void greetingJspClassFileContainsValidSmap() throws Exception {
        verifySmap(outputDir.resolve("org/apache/jsp/greeting_jsp.class"));
    }

    @Test
    void dumpedSmapFilesAreValid() throws Exception {
        final List<Path> smapFiles;
        try (Stream<Path> tree = Files.walk(outputDir)) {
            smapFiles = tree.filter(p -> p.getFileName().toString().endsWith(".smap")).toList();
        }
        assertTrue(smapFiles.size() >= 2,
                "expected a dumped SMAP per translated JSP under " + outputDir + ", found " + smapFiles);
        for (final Path smap : smapFiles) {
            verifySmap(smap);
        }
    }

    /**
     * Runs the TCK's own validator; it exits normally for a correct SMAP
     * and throws for a missing or malformed one. The tool keeps its parser
     * state in static fields and is designed for one run per JVM, so every
     * invocation loads it in a fresh classloader.
     */
    private static void verifySmap(final Path file) throws Exception {
        assertTrue(Files.isRegularFile(file), "missing JSP compiler output " + file);
        final URL tckJar = Path.of(DebuggingTckTest.class.getClassLoader()
                .loadClass("VerifySMAP").getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toUri().toURL();
        try (URLClassLoader verifierLoader = new URLClassLoader(new URL[]{tckJar}, null)) {
            // The TCK ships VerifySMAP as a package-private class in the
            // default package, so it is only reachable reflectively.
            final Class<?> verifier = verifierLoader.loadClass("VerifySMAP");
            final Method main = verifier.getMethod("main", String[].class);
            main.setAccessible(true);
            try {
                main.invoke(null, (Object) new String[]{file.toString()});
            } catch (final InvocationTargetException e) {
                if (e.getCause() instanceof Exception cause) {
                    throw cause;
                }
                throw e;
            }
        }
    }

    private static URL toUrl(final Path path) {
        try {
            return path.toUri().toURL();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
