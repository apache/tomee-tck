#!groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */

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
          sh -n environment/certificates/generate-test-certificates.sh
          sh -n runner-webprofile/run-platform-suite.sh
        '''
        sh '''python3 -c '
from pathlib import Path
from xml.etree import ElementTree
[(ElementTree.parse(source), print(f"valid XML: {source}")) for source in sorted(Path("environment/tomee/conf").glob("*.xml"))]
' '''
      }
      post {
        always { deleteDir() }
      }
    }

    // These stages are intentionally sequential. TomEE and Derby use fixed
    // localhost ports, and multiple Jenkins executors can share one host.
    stage('Smoke - JDK 17') {
      agent { label 'ubuntu' }
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

    stage('Smoke - JDK 21') {
      agent { label 'ubuntu' }
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

    stage('Platform catalog - Servlet') {
      agent { label 'ubuntu' }
      tools { jdk 'jdk_21_latest' }
      options { timeout(time: 360, unit: 'MINUTES') }
      steps {
        deleteDir()
        checkout scm
        sh 'runner-webprofile/run-platform-suite.sh servlet'
      }
      post {
        always {
          archiveArtifacts(
            artifacts: 'runner-webprofile/run/target/surefire-reports/**/*,runner-webprofile/run/target/failsafe-reports/**/*,runner-webprofile/run/target/**/logs/**/*',
            allowEmptyArchive: true
          )
          junit(testResults: 'runner-webprofile/run/target/**/TEST-*.xml', allowEmptyResults: true)
          deleteDir()
        }
      }
    }

    stage('Platform catalog - JavaTest') {
      agent { label 'ubuntu' }
      tools { jdk 'jdk_21_latest' }
      options { timeout(time: 360, unit: 'MINUTES') }
      steps {
        deleteDir()
        checkout scm
        sh 'runner-webprofile/run-platform-suite.sh javatest'
      }
      post {
        always {
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
