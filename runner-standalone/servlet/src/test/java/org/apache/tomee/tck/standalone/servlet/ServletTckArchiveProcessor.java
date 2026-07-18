/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.servlet;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Adds the vendor security-role mapping for the TCK's bundled client
 * certificate to the client-cert test archives, mirroring the reference
 * runner's archive updater.
 */
public class ServletTckArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        final String name = testClass.getJavaClass().getName();
        if (applicationArchive instanceof WebArchive webArchive
                && (name.endsWith("clientcert.ClientCertTests") || name.endsWith("clientcertanno.ClientCertAnnoTests"))) {
            webArchive.addAsWebInfResource("sun-web.xml", "sun-web.xml");
        }
    }
}
