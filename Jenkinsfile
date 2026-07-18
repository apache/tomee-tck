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
      parallel {
        stage('JDK 17') {
          agent { label 'ubuntu && ephemeral' }
          tools { jdk 'jdk_17_latest' }
          options { timeout(time: 30, unit: 'MINUTES') }
          steps {
            deleteDir()
            checkout scm
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

        stage('JDK 21') {
          agent { label 'ubuntu && ephemeral' }
          tools { jdk 'jdk_21_latest' }
          options { timeout(time: 30, unit: 'MINUTES') }
          steps {
            deleteDir()
            checkout scm
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

    stage('Platform catalog') {
      steps {
        script {
          def catalogBranch = { String branchName, String protocol, String partition, String classifier ->
            {
              stage(branchName) {
                node('ubuntu && ephemeral') {
                  deleteDir()
                  checkout scm
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

          // The EclipseLink-based Plume distribution is the default target
          // under test; OpenJPA blocks most of the persistence catalog on the
          // webprofile ZIP.
          def branches = platformPartitions.collectEntries { entry ->
            def branchName = "${entry.protocol} - ${entry.partition}"
            [(branchName): catalogBranch(branchName, entry.protocol, entry.partition, 'plume')]
          }

          // Track the OpenJPA-based webprofile distribution's persistence
          // compatibility gaps against their reviewed exclusion list.
          branches['javatest - persistence-javatest (webprofile)'] =
            catalogBranch('javatest - persistence-javatest (webprofile)', 'javatest', 'persistence-javatest', 'webprofile')

          // Standalone specification TCK runners. Every suite runs with its
          // reviewed exclusion list from runner-standalone/exclusions/, so a
          // red branch is a regression, not a known gap. The source-reactor
          // runners (security, authentication, faces) drive the downloaded
          // TCK reactors through the Maven invoker; their surefire/failsafe
          // reports live inside the extracted TCK module targets (one level
          // deep for security/authentication, two for the faces reactor's
          // per-submodule layout), which the recursive tck/** globs below
          // ingest. The faces-old runner drives the legacy
          // JavaTest half of the Faces TCK; its report lands in the
          // target/*report glob and a red JavaTest run fails the Maven build.
          // The modern faces reactor joins once its baseline is confirmed;
          // see runner-standalone/README.md and KNOWN_ISSUES.md.
          def standaloneBranch = { String id, int timeoutMinutes ->
            {
              stage("standalone - ${id}") {
                node('ubuntu && ephemeral') {
                  deleteDir()
                  checkout scm
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
          for (id in ['annotations', 'di', 'el', 'concurrency', 'data', 'servlet', 'pages', 'rest', 'validation', 'websocket', 'jsonp', 'jsonb', 'debugging', 'persistence', 'transactions', 'cdi', 'cdi-ee', 'security', 'authentication']) {
            branches["standalone - ${id}"] = standaloneBranch(id, 240)
          }
          // ~5,400 JavaTest tests plus an old-tck source build and a
          // 245-webapp deployment: a full run takes ~2.5 h on a warm
          // workstation, so give it more headroom than the other suites.
          branches['standalone - faces-old'] = standaloneBranch('faces-old', 420)

          parallel branches
        }
      }
    }
  }
}
