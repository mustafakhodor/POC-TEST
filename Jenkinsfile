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
            agentLabel   : '',   
            githubCredsId: '',
            githubUrl    : 'https://github.com/mustafakhodor/POC.git',
            gitRef       : 'main',
            filePath     : 'config/input.json',
            command      : 'cat'
          ])

          echo "Parsed keys: ${result instanceof Map ? (result.keySet() as List) : 'N/A'}"
        }
      }
    }
  }
}
