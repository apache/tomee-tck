/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.websocket;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.container.spi.event.DeployDeployment;
import org.jboss.arquillian.container.spi.event.UnDeployDeployment;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;

/**
 * The WebSocket TCK's negative-deployment tests (the {@code negdep} package)
 * deploy a WAR that contains an intentionally invalid server endpoint. The
 * specification requires such an endpoint to abort the whole application
 * deployment, so TomEE/Tomcat correctly fails the deploy. The
 * {@code @Deployment} methods carry no {@code @ShouldThrowException}: each test
 * instead expects to deploy, then probe over the network that the WAR's other,
 * valid echo endpoint is unreachable (because the deployment was aborted). The
 * clients read the target host and port from the {@code webServerHost} and
 * {@code webServerPort} system properties.
 *
 * <p>Left alone, Arquillian's {@code DeploymentExceptionHandler} finds a
 * deployment error with no expected exception and surfaces it as a class-level
 * error before the client test method can run. This observer makes the harness
 * tolerate the spec-required failure for {@code negdep} archives, in three
 * pieces:
 * <ul>
 *   <li>a {@link BeforeDeploy} observer (fired inside the still-open
 *       deployment-scoped context, before the container attempts the deploy)
 *       publishes a {@link ProtocolMetaData} pointing at the running TomEE, so
 *       the {@code @ArquillianResource URL} the client base class declares
 *       resolves even though the deploy will fail;</li>
 *   <li>a {@link DeployDeployment} interceptor tolerates the
 *       {@link DeploymentException} the failed deploy raises and clears the
 *       error recorded on the deployment, so the client test method runs;</li>
 *   <li>an {@link UnDeployDeployment} interceptor tolerates the matching
 *       undeploy failure, since the archive never actually deployed.</li>
 * </ul>
 * Every other deployment is left untouched, so a genuine deployment failure in
 * any non-negdep test still fails that test.
 */
public class NegativeDeploymentToleranceObserver {

    private static final String HOST = System.getProperty("webServerHost", "localhost");
    private static final int PORT = Integer.getInteger("webServerPort", 8080);

    @Inject
    @DeploymentScoped
    private InstanceProducer<ProtocolMetaData> protocolMetaData;

    /**
     * Runs inside the deployment-scoped context before the container attempts
     * the deploy. Pre-seed server metadata for negdep archives so the client's
     * injected URL resolves after the deploy is (expectedly) aborted.
     */
    public void seedNegativeDeploymentMetaData(@Observes final BeforeDeploy event) {
        final DeploymentDescription description = event.getDeployment();
        if (!isNegativeDeployment(description)) {
            return;
        }
        final HTTPContext http = new HTTPContext(HOST, PORT);
        http.add(new Servlet("default", contextRoot(description)));
        protocolMetaData.set(new ProtocolMetaData().addContext(http));
    }

    public void tolerateNegativeDeployment(@Observes final EventContext<DeployDeployment> context) {
        final Deployment deployment = context.getEvent().getDeployment();
        if (!isNegativeDeployment(deployment.getDescription())) {
            context.proceed();
            return;
        }
        try {
            context.proceed();
        } catch (final Throwable thrown) {
            // The failed deploy raises the container's checked
            // DeploymentException (sneaky-thrown); tolerate it and clear the
            // error recorded on the deployment so the client test runs its
            // probe. Re-raise anything that is not a deployment failure.
            if (!isDeploymentFailure(thrown)) {
                rethrow(thrown);
            }
        }
        clearDeploymentError(deployment);
    }

    public void tolerateNegativeUndeployment(@Observes final EventContext<UnDeployDeployment> context) {
        if (!isNegativeDeployment(context.getEvent().getDeployment().getDescription())) {
            context.proceed();
            return;
        }
        // The negdep archive never actually deployed, so the undeploy has
        // nothing to remove and fails; swallow that failure only.
        try {
            context.proceed();
        } catch (final Throwable thrown) {
            if (!isDeploymentFailure(thrown)) {
                rethrow(thrown);
            }
        }
    }

    private static String contextRoot(final DeploymentDescription description) {
        String name = description.getArchive().getName();
        if (name.endsWith(".war")) {
            name = name.substring(0, name.length() - ".war".length());
        }
        return name;
    }

    private static boolean isNegativeDeployment(final DeploymentDescription description) {
        if (description == null) {
            return false;
        }
        final String name = description.getName();
        if (name != null && name.contains("negdep")) {
            return true;
        }
        if (description.getArchive() != null) {
            final String archiveName = description.getArchive().getName();
            return archiveName != null && archiveName.contains("negdep");
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(final Throwable throwable) throws T {
        throw (T) throwable;
    }

    private static boolean isDeploymentFailure(final Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof DeploymentException) {
                return true;
            }
        }
        return false;
    }

    private static void clearDeploymentError(final Deployment deployment) {
        if (deployment.hasDeploymentError()) {
            // Deployment exposes no setter to clear the recorded error;
            // marking it deployed leaves the error in place, so reset it
            // reflectively. The field name is stable across the Arquillian
            // 1.x container SPI.
            try {
                final java.lang.reflect.Field field = Deployment.class.getDeclaredField("deploymentError");
                field.setAccessible(true);
                field.set(deployment, null);
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Unable to clear the recorded deployment error for a WebSocket negdep archive", e);
            }
        }
        deployment.deployed();
    }
}
