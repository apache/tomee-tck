#!groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */

def platformPartitions = []

// The version of the TomEE built by the 'Build TomEE' stage, read out of its
// reactor rather than derived from the ref: a tomee-11.0.0 tag builds 11.0.0
// and a branch builds whatever -SNAPSHOT it carries. Every downstream branch
// passes it back in as -Dtomee.version so the whole pipeline tests one build.
def tomeeVersion = ''

// Every test branch runs its suite inside a container so each TomEE, Derby,
// LDAP and JavaTest port binds a container-private network namespace: nothing
// else on the host can collide with a chosen port, and no foreign server can
// answer on one (the Arquillian adapter silently attaches to whatever already
// listens on its port). The images are pinned by digest like the rest of the
// repo's inputs. A plain JDK is all an image must provide: the checked-in
// Maven wrapper bootstraps Maven itself, and the temurin images ship bash for
// the port scripts' /dev/tcp probe. The in-script free-port selection and
// require-*-free guards stay as defense in depth and for workstation runs
// outside a container.
def jdk21Image =
  'eclipse-temurin@sha256:35685c7e23352983a48882d97cd9875f5284c228db71d1e2476e5e6c1bab1080' // 21-jdk-noble
def smokeImages = [
  jdk17: 'eclipse-temurin@sha256:0386aaf49d6756b4856119f8e037f40cc865c7c8fbdda7c81733cc806f462daf', // 17-jdk-noble
  jdk21: jdk21Image,
]

pipeline {
  agent none

  parameters {
    string(
      name: 'TOMEE_REF',
      defaultValue: 'main',
      description: 'Git tag, branch, or commit of apache/tomee to build and test. ' +
                   'The build stage clones this ref, builds the distributions and the ' +
                   'Arquillian adapter, and feeds them to every branch below.'
    )
    string(
      name: 'TOMEE_REPO_URL',
      defaultValue: 'https://github.com/apache/tomee.git',
      description: 'Repository to clone TOMEE_REF from. Point at a fork to test an ' +
                   'unmerged branch.'
    )
    choice(
      name: 'TOMEE_CLASSIFIER',
      choices: ['plume', 'webprofile', 'microprofile', 'plus'],
      description: 'TomEE distribution to test. The Platform catalog manifest and ' +
                   'the reviewed exclusion lists describe plume and webprofile; a ' +
                   'microprofile or plus run falls back to the plume expectations, ' +
                   'so read its failures against that distribution\'s scope.'
    )
  }

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
          sh -n environment/tomee/build-tomee.sh
          sh -n environment/ports/select-free-port.sh
          sh -n environment/certificates/generate-test-certificates.sh
          sh -n runner-webprofile/run-platform-suite.sh
          sh -n runner-standalone/run-standalone-suite.sh
          sh -n runner-standalone/verify-invoker-result.sh
          sh -n runner-standalone/javatest-report-to-junit.sh
          sh -n runner-smoke/run-smoke-suite.sh
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

    // Build the TomEE under test once, from the requested ref, and hand the
    // result to every branch below. The harness needs more than the
    // distribution ZIP -- arquillian-tomee-remote and its TomEE dependencies,
    // jakartaee-api for the signature tests -- so the stage installs the whole
    // reactor into a workspace-local repository and stashes the
    // org/apache/tomee slice of it. Each branch unstashes that slice into its
    // own workspace repository before running, which is why every branch sets
    // HOME to its workspace: the Maven wrapper's ${HOME}/.m2/repository is
    // then the directory the slice lands in. Nothing is deployed anywhere, so
    // concurrent jobs and workstation repositories are untouched.
    //
    // The slice is ~1.6 GB, so the stash is the pipeline's one large artifact
    // transfer; it is deliberately scoped to org/apache/tomee rather than the
    // whole repository, since every other dependency each branch needs is
    // resolvable from Maven Central and Apache snapshots.
    //
    // The git clone and the TomEE build run on the node rather than in a
    // container: the build needs no fixed ports and produces artifacts the
    // node must stash afterwards.
    stage('Build TomEE') {
      agent { label 'ubuntu' }
      tools { jdk 'jdk_21_latest' }
      options { timeout(time: 90, unit: 'MINUTES') }
      steps {
        deleteDir()
        checkout scm

        script {
          def output = sh(
            returnStdout: true,
            script: """
              TOMEE_REPO_URL='${params.TOMEE_REPO_URL}' \
              sh environment/tomee/build-tomee.sh '${params.TOMEE_REF}' "\${WORKSPACE}/.m2/repository"
            """
          ).trim()

          // The script prints diagnostics to stderr and the version to
          // stdout, so this reads the one line the pipeline consumes. Plain
          // string work rather than a java.util.regex.Matcher, which is not
          // serializable and would break the CPS transform.
          tomeeVersion = output.readLines()
            .findAll { it.startsWith('TOMEE_VERSION=') }
            .collect { it.substring('TOMEE_VERSION='.length()).trim() }
            .find { it }
          if (!tomeeVersion) {
            error("build-tomee.sh did not report a TOMEE_VERSION; output was:\n${output}")
          }
          echo "Built TomEE ${tomeeVersion} from ${params.TOMEE_REF} (${params.TOMEE_REPO_URL})"
          currentBuild.description = "TomEE ${tomeeVersion} @ ${params.TOMEE_REF} (${params.TOMEE_CLASSIFIER})"
        }

        stash(name: 'tomee-repo', includes: '.m2/repository/org/apache/tomee/**')
      }
      post {
        always { deleteDir() }
      }
    }

    // Each branch requests a single-executor ephemeral agent and runs its
    // suite inside a pinned JDK container (see the digests above) whose
    // private network namespace rules out port collisions with anything else
    // on the host. Inside the container the image's own JDK is used
    // (JAVA_HOME=/opt/java/openjdk), never a host tool(...) install, and
    // checkout/archive/junit/deleteDir stay on the node, around the
    // container. HOME points at the workspace: .inside() runs the container
    // as the host uid, which has no passwd entry in the image, so HOME
    // otherwise resolves to / and the Maven wrapper's ${HOME}/.m2 default
    // lands on an unwritable //.m2. With the override, the wrapper dist and
    // local repo live under the workspace, populated from scratch each build
    // and removed by deleteDir().
    stage('Smoke') {
      matrix {
        axes {
          axis {
            name 'SMOKE_JDK'
            values 'jdk17', 'jdk21'
          }
        }
        stages {
          stage('smoke') {
            agent { label 'ubuntu && ephemeral' }
            options { timeout(time: 30, unit: 'MINUTES') }
            steps {
              deleteDir()
              checkout scm
              unstash 'tomee-repo'
              script {
                docker.image(smokeImages[SMOKE_JDK]).inside {
                  withEnv(["HOME=${env.WORKSPACE}",
                           'JAVA_HOME=/opt/java/openjdk',
                           'PATH+JDK=/opt/java/openjdk/bin',
                           "TOMEE_VERSION=${tomeeVersion}",
                           "TOMEE_CLASSIFIER=${params.TOMEE_CLASSIFIER}"]) {
                    sh 'runner-smoke/run-smoke-suite.sh'
                  }
                }
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
                  checkout scm
                  unstash 'tomee-repo'

                  try {
                    timeout(time: 360, unit: 'MINUTES') {
                      docker.image(jdk21Image).inside {
                        withEnv(["HOME=${env.WORKSPACE}",
                                 'JAVA_HOME=/opt/java/openjdk',
                                 'PATH+JDK=/opt/java/openjdk/bin',
                                 "TOMEE_VERSION=${tomeeVersion}",
                                 "TOMEE_CLASSIFIER=${classifier}"]) {
                          sh "runner-webprofile/run-platform-suite.sh ${protocol} ${partition}"
                        }
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

          // Every partition runs against the distribution TOMEE_CLASSIFIER
          // selects (the EclipseLink-based Plume by default).
          def branches = platformPartitions.collectEntries { entry ->
            def branchName = "${entry.protocol} - ${entry.partition}"
            [(branchName): catalogBranch(branchName, entry.protocol, entry.partition, params.TOMEE_CLASSIFIER)]
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
          // Chrome-bundling container (see facesBranch below) instead of the
          // plain JDK container the other standalone suites use.
          // See runner-standalone/README.md and KNOWN_ISSUES.md.
          def standaloneBranch = { String id, int timeoutMinutes ->
            return { ->
              stage("standalone - ${id}") {
                node('ubuntu && ephemeral') {
                  deleteDir()
                  checkout scm
                  unstash 'tomee-repo'

                  try {
                    timeout(time: timeoutMinutes, unit: 'MINUTES') {
                      docker.image(jdk21Image).inside {
                        withEnv(["HOME=${env.WORKSPACE}",
                                 'JAVA_HOME=/opt/java/openjdk',
                                 'PATH+JDK=/opt/java/openjdk/bin',
                                 "TOMEE_VERSION=${tomeeVersion}",
                                 "TOMEE_CLASSIFIER=${params.TOMEE_CLASSIFIER}"]) {
                          sh "runner-standalone/run-standalone-suite.sh ${id}"
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
          // The modern faces suite (298 tests) needs a Chrome binary its
          // old-tck-selenium modules drive through Selenium, so its branch runs
          // run-standalone-suite.sh inside a container that bundles JDK 21,
          // Maven, and a matching Chrome/chromedriver pair. Pinned by digest
          // like the rest of the repo's inputs: markhobson/maven-chrome:jdk-21
          // ships Temurin JDK 21, Maven 3.9.15, and Chrome + chromedriver
          // 147 on Ubuntu 24.04 (so Selenium Manager resolves the driver
          // offline). checkout/archive/junit/deleteDir stay on the node, around
          // the container. Inside the container we use the image's JDK (its own
          // JAVA_HOME=/opt/java/openjdk), never the host tool(...) install.
          // HOME points at the workspace (see the Smoke comment), so Maven's
          // local repo lives at ${WORKSPACE}/.m2: populated from scratch each
          // build, removed by deleteDir(), and shared by the faces runner's
          // nested install-tck-util/invoker steps, which must all resolve
          // against one repo. --shm-size=2g gives headless
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
                  checkout scm
                  unstash 'tomee-repo'

                  try {
                    timeout(time: timeoutMinutes, unit: 'MINUTES') {
                      docker.image(facesImage).inside('--shm-size=2g') {
                        withEnv(["HOME=${env.WORKSPACE}",
                                 'JAVA_HOME=/opt/java/openjdk',
                                 'PATH+JDK=/opt/java/openjdk/bin',
                                 "TOMEE_VERSION=${tomeeVersion}",
                                 "TOMEE_CLASSIFIER=${params.TOMEE_CLASSIFIER}"]) {
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
