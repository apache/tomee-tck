/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.porting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tck.arquillian.porting.lib.spi.AbstractTestArchiveProcessor;

/**
 * Minimal TomEE porting hook for Jakarta EE 11 TCK deployment archives.
 *
 * <p>Portable descriptors and archive contents are deliberately left unchanged. TomEE's deployment
 * pipeline already understands a legacy GlassFish {@code sun-web.xml} context root through its
 * {@code SunConversion} stage. A minimal descriptor containing only that value is supplied because
 * Web Profile tests can expect an endpoint different from the WAR name.</p>
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
            final String contextRoot = readContextRoot(sunXmlUrl);
            if (contextRoot != null) {
                webArchive.addAsWebInfResource(new StringAsset("<sun-web-app><context-root>"
                        + escapeXml(contextRoot) + "</context-root></sun-web-app>"), "sun-web.xml");
            }
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

    private static String readContextRoot(final URL descriptor) {
        try (InputStream input = descriptor.openStream()) {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            NodeList roots = factory.newDocumentBuilder().parse(input)
                    .getElementsByTagNameNS("*", "context-root");
            if (roots.getLength() == 0) {
                return null;
            }
            final String value = roots.item(0).getTextContent().trim();
            return value.isEmpty() ? null : value;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalArgumentException("Cannot read GlassFish web descriptor " + descriptor, e);
        }
    }

    private static String escapeXml(final String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
