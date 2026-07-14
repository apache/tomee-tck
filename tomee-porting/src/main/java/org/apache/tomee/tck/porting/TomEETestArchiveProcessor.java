/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.porting;

import java.net.URL;

import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import tck.arquillian.porting.lib.spi.AbstractTestArchiveProcessor;

/**
 * Minimal TomEE porting hook for Jakarta EE 11 TCK deployment archives.
 *
 * <p>Portable descriptors and archive contents are deliberately left unchanged. TomEE's deployment
 * pipeline already understands the legacy GlassFish {@code sun-web.xml} descriptor through its
 * {@code SunConversion} stage. The descriptor must be preserved: in addition to context roots,
 * tests use it for required principal-to-role and resource mappings.</p>
 *
 * <p>The remaining callbacks are intentionally no-ops until a failing TCK test demonstrates that a
 * specific vendor descriptor is required. This avoids importing obsolete GlassFish configuration
 * wholesale.</p>
 */
public class TomEETestArchiveProcessor extends AbstractTestArchiveProcessor {
    static final String SUN_WEB_XML = "WEB-INF/sun-web.xml";

    @Override
    public void processClientArchive(final JavaArchive clientArchive, final Class<?> testClass,
                                     final URL sunXmlUrl) {
        // No Web Profile-specific client archive conversion is currently required.
    }

    @Override
    public void processEjbArchive(final JavaArchive ejbArchive, final Class<?> testClass,
                                  final URL sunXmlUrl) {
        // Preserve the portable EJB metadata.
    }

    @Override
    public void processWebArchive(final WebArchive webArchive, final Class<?> testClass,
                                  final URL sunXmlUrl) {
        if (sunXmlUrl != null && !webArchive.contains(ArchivePaths.create(SUN_WEB_XML))) {
            webArchive.addAsWebInfResource(new UrlAsset(sunXmlUrl), "sun-web.xml");
        }
    }

    @Override
    public void processRarArchive(final JavaArchive rarArchive, final Class<?> testClass,
                                  final URL sunXmlUrl) {
        // Connectors are outside the Web Profile scope.
    }

    @Override
    public void processParArchive(final JavaArchive parArchive, final Class<?> testClass,
                                  final URL persistenceXmlUrl) {
        // Preserve the portable persistence descriptor.
    }

    @Override
    public void processEarArchive(final EnterpriseArchive earArchive, final Class<?> testClass,
                                  final URL sunXmlUrl) {
        // No EAR-wide vendor descriptor conversion is currently required for the Web Profile.
    }

}
