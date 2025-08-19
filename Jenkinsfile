// 👇 declare here so it's visible to all stages
def githubFileRunner

pipeline {
  agent any
  options { timestamps() }

  parameters {
    choice(name: 'ENVIRONMENT', choices: ['DEV','QA','Stage 1','Stage 2','Prod'], description: 'Select Deployment Environment')
    string(name: 'PASSWORD',     defaultValue: 'yourPassword', description: 'Liferay SSH password')
    string(name: 'LIFERAY_USER', defaultValue: 'yourUser',     description: 'Liferay SSH user')
    string(name: 'LIFERAY_IP',   defaultValue: '1.2.3.4',      description: 'Liferay host/IP')
  }

  environment {
    MANIFEST_FILE  = 'deployment-manifest.json'
    TARGET_ENV_URL = ''   // will be set after calling helper
  }

  stages {
    stage('Load template') {
      steps {
        script {
          // make sure poc.groovy is in the workspace root (or adjust path)
          githubFileRunner = load 'poc.groovy'
          if (!githubFileRunner) {
            error "Failed to load 'poc.groovy' (githubFileRunner is null)"
          }
        }
      }
    }

    stage('Run template pipeline') {
      steps {
        script {
          // sanity guard just in case
          if (!githubFileRunner) {
            error "githubFileRunner is null; 'Load template' stage did not run or failed."
          }

          def result = githubFileRunner.call([
            environment: params.ENVIRONMENT,
            password   : params.PASSWORD,
            liferayUser: params.LIFERAY_USER,
            ip         : params.LIFERAY_IP,
            WORKSPACE  : pwd()
          ])

          // poc.groovy returns [environment: ..., url: ...]
          env.TARGET_ENV_URL = (result?.url ?: '')
          echo "Helper finished for ENV='${result?.environment}', URL='${env.TARGET_ENV_URL}'"
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'deployment-manifest.json', fingerprint: true
      echo 'Pipeline finished.'
    }
  }
}
