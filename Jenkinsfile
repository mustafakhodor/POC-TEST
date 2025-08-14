pipeline {
  agent any
  stages {
    stage('Load template') {
      steps {
        script {
          githubFileRunner = load 'poc.groovy'
        }
      }
    }
    stage('Run template pipeline') {
      steps {
        script {
          githubFileRunner.call([
            agentLabel   : 'master',                                   // optional (not used in step)
            githubCredsId: '',                                         // public repo -> leave empty
            githubUrl    : 'https://github.com/mustafakhodor/POC.git', // your repo
            gitRef       : 'main',
            filePath     : 'config/input.json',
            command      : 'cat'                                       // prints JSON to stdout
          ])
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
