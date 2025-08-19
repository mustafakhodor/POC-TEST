pipeline {
  agent any

  stages {
    stage('Load template') {
      steps {
        script {
          def githubFileRunner = load 'poc.groovy'  
          env.GFR_LOADED = 'true' 
        }
      }
    }

    stage('Run template pipeline') {
      steps {
        script {
          def githubFileRunner = load 'poc.groovy'

          def result = githubFileRunner.call([
           environment: "DEV"
          ])

        }
      }
    }
  }
}
