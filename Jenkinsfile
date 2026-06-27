// ─────────────────────────────────────────────────────────────────────────────
//  Drone OS – Yocto Multibranch Pipeline
// ─────────────────────────────────────────────────────────────────────────────

def MACHINES  = ['rpi5', 'cm5']
def FEATURES  = ['dev']

def SSTATE_DIR    = '/var/cache/yocto/sstate-cache'
def DL_DIR        = '/var/cache/yocto/downloads'
def ARTEFACTS_DIR = '/var/jenkins_home/artefacts'

def ARTEFACT_GLOBS = [
    '*.rootfs.wic.bz2',
    '*.raucb',
]

pipeline {

    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        disableConcurrentBuilds()
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        KAS_BUILD_SYSTEM_ARGS = "--force-checkout"
        SSTATE_DIR  = "${SSTATE_DIR}"
        DL_DIR      = "${DL_DIR}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.SAFE_BRANCH = env.BRANCH_NAME.replaceAll('/', '-')
                    echo "Building branch: ${env.BRANCH_NAME}  (${env.SAFE_BRANCH})"
                }
                withCredentials([file(credentialsId: 'gitcrypt-key', variable: 'GITCRYPT_KEY')]) {
                    sh '''
                        set -euo pipefail
                        echo "─── git-crypt unlock ───"
                        git-crypt unlock "$GITCRYPT_KEY"
                    '''
                }
            }
        }

        stage('Build matrix') {
            steps {
                script {
                    def parallelStages = [:]

                    for (machine in MACHINES) {
                        for (feature in FEATURES) {
                            def m = machine
                            def f = feature
                            def label = "${m}-${f}"

                            parallelStages[label] = {
                                stage("Build ${label}") {
                                    kasYoctoBuild(m, f, SSTATE_DIR, DL_DIR)
                                    archiveDeployImages(m, f, ARTEFACT_GLOBS, ARTEFACTS_DIR)
                                }
                            }
                        }
                    }

                    parallel parallelStages
                }
            }
        }

        stage('Publish index') {
            steps {
                script {
                    writeArtefactIndex(MACHINES, FEATURES, ARTEFACTS_DIR)
                }
                publishHTML(target: [
                    allowMissing         : false,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'artefact-index',
                    reportFiles          : 'index.html',
                    reportName           : 'Build Artefacts',
                ])
            }
        }
    }

    post {
        success {
            echo "✅ All builds succeeded for branch ${env.BRANCH_NAME}"
        }
        failure {
            echo "❌ Build failed for branch ${env.BRANCH_NAME}"
        }
        cleanup {
            sh 'git-crypt lock 2>/dev/null || true'
            sh 'rm -rf build-rpi5/tmp build-cm5/tmp'
        }
    }
}

def kasYoctoBuild(String machine, String feature, String sstateDir, String dlDir) {
    def kasArgs = [
        "kas/base.yml",
        "kas/machine/${machine}.yml",
        "kas/features/${feature}.yml",
    ].join(':')

    sh """
        set -euo pipefail
        echo "─── kas build: ${machine} / ${feature} ───"
        export SSTATE_DIR="${sstateDir}"
        export DL_DIR="${dlDir}"
        export KAS_BUILD_DIR="build-${machine}"
        kas build ${kasArgs}
    """
}

def archiveDeployImages(String machine, String feature, List globs, String artefactsDir) {
    def deployDir = "build-${machine}/tmp/deploy/images/drone-${machine}"
    def destDir   = "${artefactsDir}/${env.SAFE_BRANCH}/${machine}-${feature}"

    sh """
        set -euo pipefail
        echo "─── Archiving artefacts: ${machine} / ${feature} ───"
        mkdir -p "${destDir}"
        cd "${deployDir}"
        for pattern in ${globs.join(' ')}; do
            for f in \$pattern; do
                [ -e "\$f" ] || continue
                cp -v "\$f" "${destDir}/"
            done
        done
    """

    archiveArtifacts(
        artifacts         : "${deployDir}/*.rootfs.wic.bz2,${deployDir}/*.raucb",
        allowEmptyArchive : true,
        fingerprint       : true,
    )
}

def writeArtefactIndex(List machines, List features, String artefactsDir) {
    def rows = new StringBuilder()

    for (m in machines) {
        for (f in features) {
            def dir = "${artefactsDir}/${env.SAFE_BRANCH}/${m}-${f}"
            def files = sh(
                script : "ls \"${dir}\" 2>/dev/null || true",
                returnStdout: true
            ).trim().split('\n').findAll { it }

            for (file in files) {
                rows << "<tr><td>${m}</td><td>${f}</td>"
                rows << "<td><a href='file://${dir}/${file}'>${file}</a></td></tr>\n"
            }
        }
    }

    def html = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Drone OS Artefacts – ${env.SAFE_BRANCH}</title>
  <style>
    body { font-family: sans-serif; padding: 1rem 2rem; }
    h1   { color: #1a1a2e; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ccc; padding: .4rem .8rem; text-align: left; }
    th { background: #1a1a2e; color: #fff; }
    tr:nth-child(even) { background: #f4f4f4; }
  </style>
</head>
<body>
  <h1>Drone OS – Build Artefacts</h1>
  <p>Branch: <strong>${env.BRANCH_NAME}</strong>
     &nbsp;|&nbsp; Build: <strong>#${env.BUILD_NUMBER}</strong>
     &nbsp;|&nbsp; ${new Date()}</p>
  <table>
    <thead>
      <tr><th>Machine</th><th>Feature</th><th>File</th></tr>
    </thead>
    <tbody>
${rows}
    </tbody>
  </table>
</body>
</html>
"""
    sh 'mkdir -p artefact-index'
    writeFile file: 'artefact-index/index.html', text: html
}