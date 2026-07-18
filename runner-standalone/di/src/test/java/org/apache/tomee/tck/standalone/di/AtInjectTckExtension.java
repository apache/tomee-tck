/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
package org.apache.tomee.tck.standalone.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.accessories.SpareTire;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Adapts the atinject TCK's bean archive to CDI resolution rules, following
 * the TCK distribution's reference bootstrap: the TCK types predate CDI and
 * need explicit qualifiers so that exactly one bean matches each of the
 * injection points of the {@link org.atinject.tck.auto.Car} object graph.
 */
public class AtInjectTckExtension implements Extension {

    public void convertible(@Observes final ProcessAnnotatedType<Convertible> pat) {
        pat.configureAnnotatedType()
                .filterFields(field -> "spareTire".equals(field.getJavaMember().getName()))
                .forEach(field -> field.add(SpareLiteral.INSTANCE));
    }

    public void driversSeat(@Observes final ProcessAnnotatedType<DriversSeat> pat) {
        pat.configureAnnotatedType().add(DriversLiteral.INSTANCE);
    }

    public void spareTire(@Observes final ProcessAnnotatedType<SpareTire> pat) {
        pat.configureAnnotatedType()
                .add(NamedLiteral.of("spare"))
                .add(SpareLiteral.INSTANCE);
    }

    static final class DriversLiteral extends AnnotationLiteral<Drivers> implements Drivers {
        static final DriversLiteral INSTANCE = new DriversLiteral();

        private DriversLiteral() {
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Spare {
    }

    static final class SpareLiteral extends AnnotationLiteral<Spare> implements Spare {
        static final SpareLiteral INSTANCE = new SpareLiteral();

        private SpareLiteral() {
        }
    }
}
