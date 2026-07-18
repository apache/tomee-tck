/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.IAnnotationTransformer;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.annotations.ITestAnnotation;

/**
 * Disables the tests listed in the reviewed exclusion file named by the
 * {@code tck.exclusions.file} system property. The TestNG-based TCKs run
 * through suite XML files, which surefire's excludesFile does not filter,
 * so the runners register this transformer as a TestNG listener instead.
 *
 * File format: one entry per line. A fully qualified class name excludes
 * every test method the class declares or inherits (required where the
 * class's deployment or configuration phase itself fails);
 * {@code Class#method} excludes a single method. Blank lines and lines
 * starting with {@code #} are ignored. Entries match both the class that
 * declares a test method (annotation transform) and the concrete class an
 * instance runs as (method interceptor) — the TCKs' abstract base classes
 * contribute inherited test methods that only the concrete subclass name
 * identifies. Set {@code -Dtck.exclusions.file=none} to run without
 * exclusions.
 */
public class ExclusionsAnnotationTransformer implements IAnnotationTransformer, IMethodInterceptor {

    private static final String PROPERTY = "tck.exclusions.file";

    private final Set<String> excludedClasses = new HashSet<>();
    private final Map<String, Set<String>> excludedMethods = new HashMap<>();

    public ExclusionsAnnotationTransformer() {
        final String location = System.getProperty(PROPERTY);
        if (location == null || location.isBlank() || "none".equals(location)) {
            return;
        }
        final Path file = Path.of(location);
        if (!Files.isReadable(file)) {
            // Fail loudly: silently running without the reviewed exclusions
            // would report every known gap as a fresh failure.
            throw new IllegalStateException("Exclusions file not readable: " + file
                    + " (set -D" + PROPERTY + "=none to run without exclusions)");
        }
        try {
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                final int separator = line.indexOf('#');
                if (separator < 0) {
                    excludedClasses.add(line);
                } else {
                    excludedMethods
                            .computeIfAbsent(line.substring(0, separator), key -> new HashSet<>())
                            .add(line.substring(separator + 1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read exclusions file " + file, e);
        }
    }

    @Override
    public void transform(final ITestAnnotation annotation, final Class testClass,
                          final Constructor testConstructor, final Method testMethod) {
        final Class<?> declaringClass = testClass != null ? testClass
                : testMethod != null ? testMethod.getDeclaringClass() : null;
        if (declaringClass == null) {
            return;
        }
        final String className = declaringClass.getName();
        if (excludedClasses.contains(className)) {
            annotation.setEnabled(false);
            return;
        }
        if (testMethod != null) {
            final Set<String> methods = excludedMethods.get(className);
            if (methods != null && methods.contains(testMethod.getName())) {
                annotation.setEnabled(false);
            }
        }
    }

    @Override
    public List<IMethodInstance> intercept(final List<IMethodInstance> methods, final ITestContext context) {
        if (excludedClasses.isEmpty() && excludedMethods.isEmpty()) {
            return methods;
        }
        final List<IMethodInstance> kept = new ArrayList<>(methods.size());
        for (final IMethodInstance instance : methods) {
            final String realClass = instance.getMethod().getTestClass().getRealClass().getName();
            if (excludedClasses.contains(realClass)) {
                continue;
            }
            final Set<String> excluded = excludedMethods.get(realClass);
            if (excluded != null && excluded.contains(instance.getMethod().getMethodName())) {
                continue;
            }
            kept.add(instance);
        }
        return kept;
    }
}
