#!groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */

def platformPartitions = []

pipeline {
  agent none

  options {
    buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '10'))
    disableConcurrentBuilds()
    skipDefaultCheckout()
  }

  environment {
    MAVEN_OPTS = '-Xmx2g -Djava.awt.headless=true'
  }

  stages {
    stage('Validate environment') {
      agent { label 'ubuntu' }
      tools { jdk 'jdk_21_latest' }
      options { timeout(time: 15, unit: 'MINUTES') }
      steps {
        deleteDir()

        checkout scm
        stash name: 'source'

        sh 'sh environment/verify-inputs.sh --metadata-only'
        sh '''
          sh -n environment/verify-inputs.sh
          sh -n environment/reset-generated-test-environment.sh
          sh -n environment/database/require-derby-port-free.sh
          sh -n environment/database/wait-for-derby.sh
          sh -n environment/tomee/require-tomee-ports-free.sh
          sh -n environment/certificates/generate-test-certificates.sh
          sh -n runner-webprofile/run-platform-suite.sh
          sh -n runner-standalone/run-standalone-suite.sh
          sh -n runner-standalone/verify-invoker-result.sh
        '''
        sh '''python3 -c '
from pathlib import Path
from xml.etree import ElementTree
[(ElementTree.parse(source), print(f"valid XML: {source}")) for source in sorted(Path("environment/tomee/conf").glob("*.xml"))]
' '''
        script {
          platformPartitions = readFile('runner-webprofile/platform-suite.tsv')
            .readLines()
            .findAll { line -> line && !line.startsWith('#') }
            .collect { line ->
              def columns = line.split('\\t')
              [partition: columns[0], protocol: columns[2]]
            }
        }
      }
      post {
        always { deleteDir() }
      }
    }

    // Each branch requests a single-executor ephemeral agent. TomEE and Derby
    // use fixed localhost ports, so two partitions must never share a host.
    stage('Smoke') {
      matrix {
        axes {
          axis {
            name 'SMOKE_JDK'
            values 'jdk_17_latest', 'jdk_21_latest'
          }
        }
        stages {
          stage('smoke') {
            agent { label 'ubuntu && ephemeral' }
            tools { jdk "${SMOKE_JDK}" }
            options { timeout(time: 30, unit: 'MINUTES') }
            steps {
              deleteDir()
              unstash 'source'
              sh './mvnw -B -ntp -pl runner-smoke -am verify'
            }
            post {
              always {
                archiveArtifacts(
                  artifacts: 'runner-smoke/target/surefire-reports/**/*,runner-smoke/target/failsafe-reports/**/*,runner-smoke/target/**/logs/**/*,runner-smoke/target/*.log',
                  allowEmptyArchive: true
                )
                junit(testResults: 'runner-smoke/target/**/TEST-*.xml', allowEmptyResults: true)
                deleteDir()
              }
            }
          }
        }
      }
    }

    stage('Platform catalog') {
      steps {
        script {
          def catalogBranch = { String branchName, String protocol, String partition, String classifier ->
            return { ->
              stage(branchName) {
                node('ubuntu && ephemeral') {
                  deleteDir()
                  unstash 'source'
                  def javaHome = tool(name: 'jdk_21_latest', type: 'hudson.model.JDK')

                  try {
                    timeout(time: 360, unit: 'MINUTES') {
                      withEnv(["JAVA_HOME=${javaHome}", "PATH+JDK=${javaHome}/bin", "TOMEE_CLASSIFIER=${classifier}"]) {
                        sh "runner-webprofile/run-platform-suite.sh ${protocol} ${partition}"
                      }
                    }
                  } finally {
                    archiveArtifacts(
                      artifacts: 'runner-webprofile/run/target/surefire-reports/**/*,runner-webprofile/run/target/failsafe-reports/**/*,runner-webprofile/run/target/**/logs/**/*',
                      allowEmptyArchive: true
                    )
                    junit(testResults: 'runner-webprofile/run/target/**/TEST-*.xml', allowEmptyResults: true)
                    deleteDir()
                  }
                }
              }
            }
          }

          // The EclipseLink-based Plume distribution is the target under test.
          def branches = platformPartitions.collectEntries { entry ->
            def branchName = "${entry.protocol} - ${entry.partition}"
            [(branchName): catalogBranch(branchName, entry.protocol, entry.partition, 'plume')]
          }

          // Standalone specification TCK runners. Every suite runs with its
          // reviewed exclusion list from runner-standalone/exclusions/, so a
          // red branch is a regression, not a known gap. The source-reactor
          // runners (security, authentication, faces) drive the downloaded
          // TCK reactors through the Maven invoker; their surefire/failsafe
          // reports live inside the extracted TCK module targets (one level
          // deep for security/authentication, two for the faces reactor's
          // per-submodule layout), which the recursive tck/** globs below
          // ingest. The faces-old and security-old runners drive the legacy
          // JavaTest halves of the Faces and Security TCKs; each provisions and
          // starts its own TomEE (security-old also a Derby network server and
          // an in-process UnboundID LDAP server on 11389), builds its old-tck
          // bundle from source on the first run, and lands its JavaTest report
          // in the target/*report glob so a red run fails the Maven build.
          // The modern faces reactor's old-tck-selenium modules drive Chrome
          // through Selenium, and the ASF 'ubuntu && ephemeral' agents ship no
          // browser binary, so its branch runs the suite inside a
          // Chrome-bundling container (see facesBranch below) instead of on the
          // bare JDK-tools agent the other standalone suites use.
          // See runner-standalone/README.md and KNOWN_ISSUES.md.
          def standaloneBranch = { String id, int timeoutMinutes ->
            return { ->
              stage("standalone - ${id}") {
                node('ubuntu && ephemeral') {
                  deleteDir()
                  unstash 'source'
                  def javaHome = tool(name: 'jdk_21_latest', type: 'hudson.model.JDK')

                  try {
                    timeout(time: timeoutMinutes, unit: 'MINUTES') {
                      withEnv(["JAVA_HOME=${javaHome}", "PATH+JDK=${javaHome}/bin"]) {
                        sh "runner-standalone/run-standalone-suite.sh ${id}"
                      }
                    }
                  } finally {
                    archiveArtifacts(
                      artifacts: 'runner-standalone/*/target/surefire-reports/**/*,runner-standalone/*/target/failsafe-reports/**/*,runner-standalone/*/target/**/tck/**/surefire-reports/**/*,runner-standalone/*/target/**/tck/**/failsafe-reports/**/*,runner-standalone/*/target/**/logs/**/*,runner-standalone/*/target/*report/**/*',
                      allowEmptyArchive: true
                    )
                    junit(
                      testResults: 'runner-standalone/*/target/surefire-reports/TEST-*.xml,runner-standalone/*/target/failsafe-reports/TEST-*.xml,runner-standalone/*/target/**/tck/**/surefire-reports/TEST-*.xml,runner-standalone/*/target/**/tck/**/failsafe-reports/TEST-*.xml',
                      allowEmptyResults: true
                    )
                    deleteDir()
                  }
                }
              }
            }
          }
          // The modern faces suite (298 tests) needs a Chrome binary its
          // old-tck-selenium modules drive through Selenium, so its branch runs
          // run-standalone-suite.sh inside a container that bundles JDK 21,
          // Maven, and a matching Chrome/chromedriver pair. Pinned by digest
          // like the rest of the repo's inputs: markhobson/maven-chrome:jdk-21
          // ships Temurin JDK 21, Maven 3.9.15, and Chrome + chromedriver
          // 147 on Ubuntu 24.04 (so Selenium Manager resolves the driver
          // offline). checkout/archive/junit/deleteDir stay on the node, around
          // the container. Inside the container we use the image's JDK (its own
          // JAVA_HOME=/opt/java/openjdk), never the host tool(...) install. The
          // runner leaves Maven's local repo at the container default: docker
          // .inside() runs a fresh container per build, so ~/.m2 is populated
          // from scratch and discarded with the container - the faces runner's
          // nested install-tck-util/invoker steps must all share one repo, so an
          // override would only split them apart. --shm-size=2g gives headless
          // Chrome enough shared memory: the TCK's ChromeDevtoolsDriver sets
          // --headless=new --no-sandbox --disable-gpu but not
          // --disable-dev-shm-usage, so the default 64 MB /dev/shm would crash
          // the renderer.
          def facesImage =
            'markhobson/maven-chrome@sha256:90b0a104dd7236b5fcef71c342e4f6392fb204a4df030fad4bbf4c7990aaee00'
          def facesBranch = { int timeoutMinutes ->
            return { ->
              stage('standalone - faces') {
                node('ubuntu && ephemeral') {
                  deleteDir()
                  unstash 'source'

                  try {
                    timeout(time: timeoutMinutes, unit: 'MINUTES') {
                      docker.image(facesImage).inside('--shm-size=2g') {
                        withEnv(['JAVA_HOME=/opt/java/openjdk',
                                 'PATH+JDK=/opt/java/openjdk/bin']) {
                          sh 'runner-standalone/run-standalone-suite.sh faces'
                        }
                      }
                    }
                  } finally {
                    archiveArtifacts(
                      artifacts: 'runner-standalone/*/target/surefire-reports/**/*,runner-standalone/*/target/failsafe-reports/**/*,runner-standalone/*/target/**/tck/**/surefire-reports/**/*,runner-standalone/*/target/**/tck/**/failsafe-reports/**/*,runner-standalone/*/target/**/logs/**/*,runner-standalone/*/target/*report/**/*',
                      allowEmptyArchive: true
                    )
                    junit(
                      testResults: 'runner-standalone/*/target/surefire-reports/TEST-*.xml,runner-standalone/*/target/failsafe-reports/TEST-*.xml,runner-standalone/*/target/**/tck/**/surefire-reports/TEST-*.xml,runner-standalone/*/target/**/tck/**/failsafe-reports/TEST-*.xml',
                      allowEmptyResults: true
                    )
                    deleteDir()
                  }
                }
              }
            }
          }
          // security-old shares the plain 240-minute default: even with the
          // one-off old-tck source build it runs only ~83 JavaTest tests
          // (~65 client classes), far below the faces-old sizing that earns
          // the extended timeout below.
          for (id in ['annotations', 'di', 'el', 'concurrency', 'data', 'servlet', 'pages', 'rest', 'validation', 'websocket', 'jsonp', 'jsonb', 'debugging', 'persistence', 'transactions', 'cdi', 'cdi-ee', 'security', 'security-old', 'authentication']) {
            branches["standalone - ${id}"] = standaloneBranch(id, 240)
          }
          // ~5,400 JavaTest tests plus an old-tck source build and a
          // 245-webapp deployment: a full run takes ~2.5 h on a warm
          // workstation, so give it more headroom than the other suites.
          branches['standalone - faces-old'] = standaloneBranch('faces-old', 420)
          // The modern faces reactor takes ~45-60 min on a warm workstation
          // (TCK download, provision, 298 Selenium/Arquillian/sigtest tests).
          // A cold CI node also pulls the browser image and repopulates the
          // container-local Maven repo from scratch, so 300 minutes leaves
          // ample headroom over the observed runtime.
          branches['standalone - faces'] = facesBranch(300)

          parallel branches
        }
      }
    }
  }
}
