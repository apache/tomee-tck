/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.websocket;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class WebSocketTckExtension implements LoadableExtension {

    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(NegativeDeploymentToleranceObserver.class);
    }
}
