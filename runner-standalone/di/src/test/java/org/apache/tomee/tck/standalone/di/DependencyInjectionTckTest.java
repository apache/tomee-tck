/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.di;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Boots Apache OpenWebBeans through the standard CDI SE API and hands the
 * TCK a {@link Car} built by the injector under test. Static injection is
 * not supported by CDI ({@code supportsStatic=false}); private member
 * injection is ({@code supportsPrivate=true}).
 */
public class DependencyInjectionTckTest {

    public static synchronized Test suite() {
        final Car tckCar;
        try {
            final SeContainer container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addExtensions(AtInjectTckExtension.class)
                    .addPackages(true, Tck.class)
                    .initialize();
            Runtime.getRuntime().addShutdownHook(new Thread(container::close));
            tckCar = container.select(Car.class).get();
        } catch (final IllegalArgumentException alreadyRegistered) {
            // The JUnit runner loads this class once for discovery and once
            // for execution; OpenWebBeans allows a single container per class
            // loader, so reuse the running one.
            return suiteFromRunningContainer();
        }
        final TestSuite suite = new TestSuite("Jakarta Dependency Injection 2.0 TCK");
        suite.addTest(Tck.testsFor(tckCar, false, true));
        return suite;
    }

    private static Test suiteFromRunningContainer() {
        final Car tckCar = CDI.current().select(Car.class).get();
        final TestSuite suite = new TestSuite("Jakarta Dependency Injection 2.0 TCK");
        suite.addTest(Tck.testsFor(tckCar, false, true));
        return suite;
    }
}
