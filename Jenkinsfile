pipeline {
  agent any
  stages {
    stage('Load template') {
      steps {
        script {
          githubFileRunner = load 'poc.groovy'   // adjust path if placed elsewhere
        }
      }
    }
    stage('Run template pipeline') {
      steps {
        script {
          def result = githubFileRunner.call([
            agentLabel   : 'master',                                   // optional
            githubCredsId: '',                                         // empty for public repo
            githubUrl    : 'https://github.com/mustafakhodor/POC.git', // your repo
            gitRef       : 'main',
            filePath     : 'config/input.json',
            command      : 'cat'                                       // must print JSON
          ])
          echo "Parsed keys: ${result instanceof Map ? (result.keySet() as List) : 'N/A'}"
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
  }
}
