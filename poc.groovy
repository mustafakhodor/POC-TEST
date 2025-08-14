def call(Map config) {
pipeline {
    agent { label config.agentLabel ?: 'master' }

    environment {
        // Optional credential ID for private GitHub repos (PAT/User+Pass/SSH).
        GITHUB_CREDS_ID = "${config.githubCredsId ?: ''}"
    }

    parameters {
        // Where to fetch the file from in GitHub
        string(name: 'GITHUB_URL',  defaultValue: config.githubUrl ?: 'https://github.com/you/repo.git', description: 'GitHub repo URL (.git)')
        string(name: 'GIT_REF',     defaultValue: config.gitRef    ?: 'main',                             description: 'Branch/Tag/SHA to checkout')
        string(name: 'FILE_PATH',   defaultValue: config.filePath  ?: 'configs/input.json',              description: 'Path to the file inside the repo')
        string(name: 'PROCESS_CMD', defaultValue: config.command   ?: 'cat',                             description: 'Command to run; must output JSON to stdout')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout GitHub') {
            steps {
                script {
                    def checkoutDir = "${env.WORKSPACE}/gh-src"
                    dir(checkoutDir) {
                        deleteDir()
                        def extensions = [
                            [$class: 'WipeWorkspace'],
                            [$class: 'CloneOption', depth: 1, noTags: true, shallow: true, honorRefspec: true]
                        ]
                        def scmCfg = [
                            $class: 'GitSCM',
                            branches: [[name: params.GIT_REF]],
                            userRemoteConfigs: [[url: params.GITHUB_URL]],
                            extensions: extensions
                        ]
                        if (env.GITHUB_CREDS_ID?.trim()) {
                            scmCfg.userRemoteConfigs = [[url: params.GITHUB_URL, credentialsId: env.GITHUB_CREDS_ID]]
                        }
                        checkout(scmCfg)

                        // Validate file existence
                        sh """
                          test -s "${params.FILE_PATH}" || (echo "File not found or empty: ${params.FILE_PATH}" && exit 1)
                        """
                    }
                }
            }
        }

        stage('Run command & parse JSON') {
            steps {
                script {
                    def workDir = "${env.WORKSPACE}/gh-src"
                    def fullPath = "${workDir}/${params.FILE_PATH}"
                    def rawOut = sh(script: "${params.PROCESS_CMD} '${fullPath}'", returnStdout: true).trim()

                    echo "Raw command output (first 500 chars):\n${rawOut.take(500)}"

                    def parsed
                    try {
                        parsed = readJSON text: rawOut
                    } catch (e) {
                        error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
                    }

                    // Persist for later stages / artifacts
                    writeJSON file: "${workDir}/command-result.json", json: parsed, pretty: 2
                    stash name: 'github-process-result', includes: 'gh-src/command-result.json'

                    // Branching logic (customize as needed)
                    if (parsed.status == 'success') {
                        echo "✅ Success branch"
                    } else if (parsed.status == 'warning') {
                        echo "⚠️ Warning: ${parsed.message ?: 'No message provided'}"
                    } else {
                        error "❌ Failure: ${parsed.message ?: 'No message provided'}"
                    }
                }
            }
        }

        stage('Post step (optional)') {
            when { expression { true } }
            steps {
                script {
                    echo "Add any follow-up actions here (deploy, notify, etc.)"
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
