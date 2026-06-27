// ─────────────────────────────────────────────────────────────────────────────
//  Drone OS – Yocto Multibranch Pipeline
//
//  Triggered automatically for every branch in the GitHub repo.
//  Builds all machine × feature combinations and archives the deploy images.
// ─────────────────────────────────────────────────────────────────────────────

// Build matrix: every combination below is built in parallel.
def MACHINES  = ['rpi5', 'cm5']
def FEATURES  = ['dev']

// Shared cache paths (mounted as Docker volumes in docker-compose.yml)
def SSTATE_DIR    = '/var/cache/yocto/sstate-cache'
def DL_DIR        = '/var/cache/yocto/downloads'
def ARTEFACTS_DIR = '/var/jenkins_home/artefacts'

// Artefact globs relative to build/tmp/deploy/images/<machine>/
def ARTEFACT_GLOBS = [
    '*.rootfs.wic.bz2',
    '*.raucb',
]

pipeline {

    // Run on any available agent (the Jenkins controller itself when using the
    // single-container setup from docker-compose.yml).
    agent any

    options {
        // Keep the last 10 builds per branch to save disk space.
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        // Abort if a build is still running when a new one starts for the same branch.
        disableConcurrentBuilds()

    }

    triggers {
        // Poll GitHub every 5 minutes.  Replace with a webhook for instant triggers:
        // In GitHub → repo Settings → Webhooks → add http://<jenkins>/github-webhook/
        // and install the GitHub plugin, then replace pollSCM with:
        //   githubPush()
        pollSCM('H/5 * * * *')
    }

    environment {
        // kas reads KAS_BUILD_SYSTEM_ARGS for extra bitbake settings.
        KAS_BUILD_SYSTEM_ARGS = "--force-checkout"
        // Point kas / bitbake at the shared caches.
        SSTATE_DIR  = "${SSTATE_DIR}"
        DL_DIR      = "${DL_DIR}"
    }

    stages {

        // ── 1. Checkout ──────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // Make branch name safe for artefact paths (replace / with -)
                    env.SAFE_BRANCH = env.BRANCH_NAME.replaceAll('/', '-')
                    echo "Building branch: ${env.BRANCH_NAME}  (${env.SAFE_BRANCH})"
                }
            }
        }

        // ── 2. Matrix build ──────────────────────────────────────────────────
        stage('Build matrix') {
            steps {
                script {
                    def parallelStages = [:]

                    for (machine in MACHINES) {
                        for (feature in FEATURES) {
                            // Capture loop variables for closure
                            def m = machine
                            def f = feature
                            def label = "${m}-${f}"

                            parallelStages[label] = {
                                stage("Build ${label}") {
                                    ws("/var/jenkins_home/workspace/drone-os-${env.SAFE_BRANCH}-${label}") {
                                        checkout scm
                                        kasYoctoBuild(m, f)
                                        archiveDeployImages(m, f, ARTEFACT_GLOBS, ARTEFACTS_DIR)
                                    }
                                }
                            }
                        }
                    }

                    parallel parallelStages
                }
            }
        }

        // ── 3. Publish artefact index ─────────────────────────────────────────
        stage('Publish index') {
            steps {
                script {
                    writeArtefactIndex(MACHINES, FEATURES, ARTEFACTS_DIR)
                }
                // Serve the HTML index via the Jenkins HTML Publisher plugin.
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
            // Remove the Yocto build directory after archiving to reclaim disk.
            // Comment this out if you want to keep the build tree for debugging.
            sh 'rm -rf build/tmp'
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helper: run kas build for one machine × feature combination
// ─────────────────────────────────────────────────────────────────────────────
def kasYoctoBuild(String machine, String feature) {
    def kasArgs = [
        "kas/base.yml",
        "kas/machine/${machine}.yml",
        "kas/features/${feature}.yml",
    ].join(':')

    sh """
        set -euo pipefail
        echo "─── kas build: ${machine} / ${feature} ───"
        export SSTATE_DIR="${env.SSTATE_DIR}"
        export DL_DIR="${env.DL_DIR}"
        # Use host Podman via its socket. kas-container detects DOCKER_HOST
        # and uses it as the container runtime, bypassing rootless UID mapping.
        export DOCKER_HOST=unix:///run/podman/podman.sock
        export KAS_CONTAINER_ENGINE=docker
        kas-container build ${kasArgs}
    """
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helper: copy deploy images to artefact volume and archive in Jenkins
// ─────────────────────────────────────────────────────────────────────────────
def archiveDeployImages(String machine, String feature, List globs, String artefactsDir) {
    // Machine names in the deploy dir use the full distro prefix, e.g. drone-rpi5
    def deployDir = "build/tmp/deploy/images/drone-${machine}"
    def destDir   = "${artefactsDir}/${env.SAFE_BRANCH}/${machine}-${feature}"

    sh """
        set -euo pipefail
        echo "─── Archiving artefacts: ${machine} / ${feature} ───"
        mkdir -p "${destDir}"
        cd "${deployDir}"
        for pattern in ${globs.join(' ')}; do
            # nullglob-style: skip silently if pattern matches nothing
            for f in \$pattern; do
                [ -e "\$f" ] || continue
                cp -v "\$f" "${destDir}/"
            done
        done
    """

    // Also archive inside Jenkins so artefacts are accessible from the build page.
    archiveArtifacts(
        artifacts         : "${deployDir}/*.rootfs.wic.bz2,${deployDir}/*.raucb",
        allowEmptyArchive : true,
        fingerprint       : true,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Helper: write a simple HTML index of available artefacts
// ─────────────────────────────────────────────────────────────────────────────
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