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
          githubFileRunner([
            agentLabel   : 'master',                                        // optional
            githubCredsId: '',                                              // leave empty for public repo
            githubUrl    : 'https://github.com/mustafakhodor/POC.git',      // your GitHub repo
            gitRef       : 'main',                                          // branch
            filePath     : 'config/input.json',                             // file path in repo
            command      : 'cat'                                            // must print JSON to stdout; cat works for testing
          ])
        }
      }
    }
  }
}
