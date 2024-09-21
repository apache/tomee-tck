#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

. variables.sh

# First clean up old TCK installations
rm -rf jakarta-security-tck*.zip security-tck-*

# Download TCK, unpack and cd into it
curl -O https://download.eclipse.org/jakartaee/security/3.0/jakarta-security-tck-$TCK_VERSION.zip
unzip jakarta-security-tck-$TCK_VERSION.zip
cd security-tck-$TCK_VERSION/tck

# Patch pom.xml with tomee profile
mv pom.xml pom.xml.original
xsltproc --output pom.xml ../../modifications/modify-pom.xslt pom.xml.original
