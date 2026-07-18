/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.data;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;

/**
 * The Data TCK's own {@code TCKArchiveProcessor} appends the read-only and
 * servlet framework packages, but several {@code @Deployment} methods (for
 * example {@code standalone.entity.EntityTests}) reference sibling classes
 * such as {@code MultipleEntityRepo} that are neither added explicitly nor
 * covered by those framework packages. Append the test class's own package so
 * every class the test injects is resolvable inside the container.
 */
public class DataTckArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        if (applicationArchive instanceof ClassContainer<?> classContainer) {
            classContainer.addPackage(testClass.getJavaClass().getPackage());
        }
    }
}
