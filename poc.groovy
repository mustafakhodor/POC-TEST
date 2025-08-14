def call(Map cfg = [:]) {
  pipeline {
    agent any

    options {
      timestamps()
      disableConcurrentBuilds()
    }

    environment {
      GITHUB_URL      = require(cfg, 'githubUrl')
      GIT_REF         = require(cfg, 'gitRef')
      FILE_PATH       = require(cfg, 'filePath')
      PROCESS_CMD     = require(cfg, 'command')
      GITHUB_CREDS_ID = (cfg.githubCredsId ?: '').trim()
    }

    stages {
      stage('Checkout GitHub') {
        steps {
          script {
            def work = "${env.WORKSPACE}/gh-src"
            dir(work) {
              deleteDir()
              def exts = [
                [$class: 'WipeWorkspace'],
                [$class: 'CloneOption', depth: 1, noTags: true, shallow: true, honorRefspec: true]
              ]
              def scmCfg = [
                $class: 'GitSCM',
                branches: [[name: env.GIT_REF]],
                userRemoteConfigs: [[url: env.GITHUB_URL]],
                extensions: exts
              ]
              if (env.GITHUB_CREDS_ID) {
                scmCfg.userRemoteConfigs = [[url: env.GITHUB_URL, credentialsId: env.GITHUB_CREDS_ID]]
              }
              checkout(scmCfg)

              sh """ test -s "${env.FILE_PATH}" || (echo "File not found or empty: ${env.FILE_PATH}" && exit 1) """
            }
          }
        }
      }

      stage('Run command & parse JSON') {
        steps {
          script {
            def work = "${env.WORKSPACE}/gh-src"
            def full = "${work}/${env.FILE_PATH}"
            def raw  = sh(script: "${env.PROCESS_CMD} '${full}'", returnStdout: true).trim()
            echo "Raw command output (first 500 chars):\n${raw.take(500)}"

            def parsed
            try {
              parsed = readJSON text: raw
            } catch (e) {
              error "Failed to parse JSON.\nError: ${e}\nOutput was:\n${raw}"
            }

            writeJSON file: "${work}/command-result.json", json: parsed, pretty: 2
            stash name: 'github-process-result', includes: 'gh-src/command-result.json'

            if (parsed.status == 'success') {
              echo "✅ Success"
            } else if (parsed.status == 'warning') {
              echo "⚠️ Warning: ${parsed.message ?: 'No message'}"
            } else {
              error "❌ Failure: ${parsed.message ?: 'No message'}"
            }
          }
        }
      }
    }

    post {
      success {
        script {
          unstash 'github-process-result'
          archiveArtifacts artifacts: 'gh-src/command-result.json', onlyIfSuccessful: true, allowEmptyArchive: true
        }
      }
      always {
        cleanWs notFailBuild: true
      }
    }
  }
}

// helper (inside same file)
def require(Map m, String key) {
  if (!m.containsKey(key) || m[key] == null || "${m[key]}".trim() == '') {
    error "githubFileRunner: missing required argument '${key}'"
  }
  m[key]
}
