/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.porting;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/** Registers the TomEE implementation of the Jakarta EE TCK archive porting SPI. */
public class TomEETckExtension implements LoadableExtension {
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.service(ResourceProvider.class, TomEETestArchiveProcessor.class);
        builder.observer(TomEETestArchiveProcessor.class);
    }
}
