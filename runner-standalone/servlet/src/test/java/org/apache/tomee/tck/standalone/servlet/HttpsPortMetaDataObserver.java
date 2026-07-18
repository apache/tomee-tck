/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.servlet;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;

/**
 * The TCK's client-cert tests take the TLS port from the URL injected for the
 * deployment that targets the {@code https} container (the reference runner's
 * second container reports its TLS port as the default). The TomEE adapter
 * always reports the plain HTTP port, so rewrite the metadata of
 * https-targeted deployments to the TLS connector port.
 */
public class HttpsPortMetaDataObserver {

    static final int HTTPS_PORT = Integer.getInteger("servlet.tck.httpsPort", 8443);

    @Inject
    @DeploymentScoped
    private InstanceProducer<ProtocolMetaData> metaDataProducer;

    @Inject
    private Instance<ProtocolMetaData> metaData;

    public void rewrite(@Observes final AfterDeploy event) {
        if (!"https".equals(event.getDeployment().getTarget().getName())) {
            return;
        }
        final ProtocolMetaData current = metaData.get();
        if (current == null) {
            return;
        }
        final ProtocolMetaData rewritten = new ProtocolMetaData();
        for (final HTTPContext context : current.getContexts(HTTPContext.class)) {
            final HTTPContext secure = new HTTPContext(context.getName(), context.getHost(), HTTPS_PORT);
            for (final Servlet servlet : context.getServlets()) {
                secure.add(new Servlet(servlet.getName(), servlet.getContextRoot()));
            }
            rewritten.addContext(secure);
        }
        metaDataProducer.set(rewritten);
    }
}
