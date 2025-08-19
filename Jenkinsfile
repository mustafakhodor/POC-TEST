pipeline {
  agent any
  options { timestamps() }

  parameters {
    choice(
      name: 'ENVIRONMENT',
      choices: ['DEV', 'QA', 'Stage 1', 'Stage 2', 'Prod'],
      description: 'Select Deployment Environment'
    )
  }

  environment {
    MANIFEST_FILE = 'deployment-manifest.json'
    TARGET_ENV_URL = ''
  }

  stages {
    stage('Load template') {
      steps {
        script {
          githubFileRunner = load 'poc.groovy'   // DO NOT contain pipeline {} inside
        }
      }
    }

    stage('Run template pipeline') {
      steps {
        script {
          // Pass the choice param down to the loaded script
          def result = githubFileRunner.call([ environment: params.ENVIRONMENT ])
          echo "Helper finished for ENV='${result.env}', URL='${result.url}'"
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
