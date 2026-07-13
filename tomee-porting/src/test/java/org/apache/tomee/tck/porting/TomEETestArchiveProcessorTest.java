/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.porting;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TomEETestArchiveProcessorTest {
    private final TomEETestArchiveProcessor processor = new TomEETestArchiveProcessor();

    @Test
    void translatesOnlyTheContextRootForTomEEsExistingConversionStage() throws IOException {
        final URL descriptor = getClass().getResource("/sun-web.xml");
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "sample.war");

        processor.processWebArchive(archive, getClass(), descriptor);

        assertNotNull(archive.get(ArchivePaths.create(TomEETestArchiveProcessor.SUN_WEB_XML)));
        assertEquals("<sun-web-app><context-root>/expected</context-root></sun-web-app>",
                new String(archive.get(TomEETestArchiveProcessor.SUN_WEB_XML).getAsset()
                        .openStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void doesNothingWhenNoVendorDescriptorWasSupplied() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "portable.war");

        processor.processWebArchive(archive, getClass(), null);

        assertFalse(archive.contains(ArchivePaths.create(TomEETestArchiveProcessor.SUN_WEB_XML)));
    }

    @Test
    void doesNothingWhenTheVendorDescriptorHasNoContextRoot() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "portable.war");

        processor.processWebArchive(archive, getClass(), getClass().getResource("/sun-web-without-context.xml"));

        assertFalse(archive.contains(ArchivePaths.create(TomEETestArchiveProcessor.SUN_WEB_XML)));
    }

    @Test
    void neverOverwritesADescriptorAlreadyPresentInTheArchive() {
        final StringAsset existing = new StringAsset("<sun-web-app/>");
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "existing.war")
                .addAsWebInfResource(existing, "sun-web.xml");

        processor.processWebArchive(archive, getClass(), getClass().getResource("/sun-web.xml"));

        assertSame(existing, archive.get(TomEETestArchiveProcessor.SUN_WEB_XML).getAsset());
    }

    @Test
    void isDiscoverableAsAnArquillianExtension() {
        final LoadableExtension extension = java.util.ServiceLoader.load(LoadableExtension.class)
                .stream()
                .map(java.util.ServiceLoader.Provider::get)
                .filter(TomEETckExtension.class::isInstance)
                .findFirst()
                .orElse(null);

        assertNotNull(extension);
    }
}
