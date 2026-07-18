/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.jsonb;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * Boots the CDI SE container (Apache OpenWebBeans, the CDI implementation
 * TomEE ships) once for the TCK run and closes it when the run finishes.
 *
 * <p>The TCK's CDI tests declare their own {@code SeContainerInitializer}
 * bootstrap in a private static {@code @BeforeAll} method, which JUnit
 * silently ignores, so the environment has to provide the running container
 * the tests assume — like the deployed EE container the specification
 * describes. Registered through the JUnit auto-detection service loader.
 */
public class CdiContainerExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(final ExtensionContext context) {
        context.getRoot().getStore(GLOBAL)
                .getOrComputeIfAbsent(ContainerResource.class);
    }

    static class ContainerResource implements ExtensionContext.Store.CloseableResource {

        private final SeContainer container;

        ContainerResource() {
            SeContainer started = null;
            if (!isCdiRunning()) {
                started = SeContainerInitializer.newInstance().initialize();
                System.out.println("[tck-harness] started CDI SE container "
                        + started.getBeanManager().getClass().getName());
            }
            container = started;
        }

        private static boolean isCdiRunning() {
            try {
                // OpenWebBeans returns a CDI facade even without a running
                // container; resolving a bean is the reliable liveness probe.
                CDI.current().getBeanManager().getBeans(Object.class);
                return true;
            } catch (final RuntimeException notRunning) {
                return false;
            }
        }

        @Override
        public void close() {
            if (container != null) {
                container.close();
            }
        }
    }
}
