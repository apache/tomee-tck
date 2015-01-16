#!/bin/bash

cat target/logs/javatest.log target/apache-tomee-*/logs/* | less
