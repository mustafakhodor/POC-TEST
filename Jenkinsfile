pipeline {
  agent any
  options { timestamps() }

  parameters {
    choice(
      name: 'ENVIRONMENT',
      choices: ['DEV', 'QA', 'Stage 1', 'Stage 2', 'Prod'],
      description: 'Select Deployment Environment'
    )
    string(name: 'PASSWORD',     defaultValue: 'yourPassword', description: 'Liferay SSH password')
    string(name: 'LIFERAY_USER', defaultValue: 'yourUser',     description: 'Liferay SSH user')
    string(name: 'LIFERAY_IP',   defaultValue: '1.2.3.4',      description: 'Liferay host/IP')
  }

  environment {
    MANIFEST_FILE = 'deployment-manifest.json'
    TARGET_ENV_URL = ''   // we’ll set this after calling the helper
  }

  stages {
    stage('Load template') {
      steps {
        script {
          // Load your helper (poc.groovy). It must end with: "return this"
          githubFileRunner = load 'poc.groovy'
        }
      }
    }

    stage('Run template pipeline') {
      steps {
        script {
          // Call the helper. Pass everything it needs, including WORKSPACE via pwd().
          def mod = githubFileRunner.call([
            environment: params.ENVIRONMENT,
            password   : params.PASSWORD,
            liferayUser: params.LIFERAY_USER,
            ip         : params.LIFERAY_IP,
            WORKSPACE  : pwd()
          ])

          // Since poc.groovy returns "this", 'mod' is the script object.
          // It exposes @Field TARGET_ENV_URL after stage 'Determine target environment URL'.
          env.TARGET_ENV_URL = (mod.TARGET_ENV_URL ?: '')
          echo "Resolved TARGET_ENV_URL='${env.TARGET_ENV_URL}' for ENV='${params.ENVIRONMENT}'"

          // If you want a friendly result echo:
          echo "Helper finished. URL='${env.TARGET_ENV_URL}'"
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
