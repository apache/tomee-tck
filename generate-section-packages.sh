#!/bin/bash

TCK_HOME="$(perl -ne 'print if s/.*<javaee6.cts.home>(.*)<.*/$1/' $HOME/.m2/settings.xml)"

find $TCK_HOME/src/com/sun/ts/tests -name '*.java' -exec perl -ne 'print if s/^package +(.*);.*/$1/;' {} \; | egrep -v '\.common($|\.)'| sort | uniq
