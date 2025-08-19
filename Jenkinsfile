pipeline {
  agent any

  parameters {
    choice(
      name: 'ENVIRONMENT',
      choices: ['DEV', 'QA', 'Stage 1', 'Stage 2', 'Prod'],
      description: 'Select Deployment Environment'
    )
  }

  stages {
    stage('Run Template Pipeline') {
      steps {
        script {
          // Load the other file
          def template = load("${env.WORKSPACE}/poc.groovy")

          // Call its entry method and pass the parameter
          template.runPipeline(params.ENVIRONMENT)
        }
      }
    }
  }
}
