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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Completes the Data TCK's deployments for TomEE. Three gaps are filled
 * (the first is TCK-generic, the other two mirror apache/tomee
 * tck/data-standalone, whose runtime is expected to provide them):
 *
 * <ul>
 * <li>The TCK's own {@code TCKArchiveProcessor} appends the read-only and
 * servlet framework packages, but several {@code @Deployment} methods (for
 * example {@code standalone.entity.EntityTests}) reference sibling classes
 * such as {@code MultipleEntityRepo} that are neither added explicitly nor
 * covered by those framework packages. Append the test class's own package so
 * every class the test injects is resolvable inside the container.</li>
 *
 * <li>The TCK ships no {@code persistence.xml} — providing the persistence
 * unit is the Jakarta Data runtime's responsibility. Generate one on
 * {@code java:comp/DefaultDataSource} with auto-discovery so the TCK's JPA
 * entities are found by the container's default provider.</li>
 *
 * <li>Without a {@code beans.xml} with {@code bean-discovery-mode="all"} the
 * {@code @Repository} interfaces are never scanned, so TomEE's
 * openejb-jakarta-data CDI extension never materializes them and every
 * repository injection stays {@code null}.</li>
 * </ul>
 */
public class DataTckArchiveProcessor implements ApplicationArchiveProcessor {

    private static final String PERSISTENCE_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\"\n" +
        "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "             xsi:schemaLocation=\"https://jakarta.ee/xml/ns/persistence\n" +
        "               https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd\"\n" +
        "             version=\"3.2\">\n" +
        "    <persistence-unit name=\"tck-pu\" transaction-type=\"JTA\">\n" +
        "        <jta-data-source>java:comp/DefaultDataSource</jta-data-source>\n" +
        "        <exclude-unlisted-classes>false</exclude-unlisted-classes>\n" +
        "        <properties>\n" +
        "            <property name=\"jakarta.persistence.schema-generation.database.action\"\n" +
        "                      value=\"drop-and-create\"/>\n" +
        "            <!-- OpenJPA-specific schema generation -->\n" +
        "            <property name=\"openjpa.jdbc.SynchronizeMappings\"\n" +
        "                      value=\"buildSchema(ForeignKeys=true)\"/>\n" +
        "            <property name=\"openjpa.Log\" value=\"DefaultLevel=WARN\"/>\n" +
        "            <!-- EclipseLink-specific schema generation -->\n" +
        "            <property name=\"eclipselink.ddl-generation\"\n" +
        "                      value=\"drop-and-create-tables\"/>\n" +
        "            <property name=\"eclipselink.ddl-generation.output-mode\"\n" +
        "                      value=\"database\"/>\n" +
        "            <property name=\"eclipselink.logging.level\" value=\"WARNING\"/>\n" +
        "        </properties>\n" +
        "    </persistence-unit>\n" +
        "</persistence>\n";

    private static final String BEANS_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
        "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee\n" +
        "         https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd\"\n" +
        "       version=\"4.0\"\n" +
        "       bean-discovery-mode=\"all\">\n" +
        "</beans>\n";

    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        if (applicationArchive instanceof ClassContainer<?> classContainer) {
            classContainer.addPackage(testClass.getJavaClass().getPackage());
        }
        if (applicationArchive instanceof WebArchive war) {
            if (!war.contains("WEB-INF/classes/META-INF/persistence.xml")) {
                war.addAsResource(new StringAsset(PERSISTENCE_XML), "META-INF/persistence.xml");
            }
            if (!war.contains("WEB-INF/beans.xml")) {
                war.addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
            }
        }
    }
}
