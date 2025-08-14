pipeline {
  agent any

  stages {
    stage('Load template') {
      steps {
        script {
          // declare the variable to avoid the "def" warning
          def githubFileRunner = load 'poc.groovy'   // adjust path if needed
          // save it to a higher scope if you need it outside this script block:
          // currentBuild.description = 'loaded'  // or use a global var declared above pipeline {}
          // but easiest: call it in the same stage or re-load in next stage.
          // In our case we'll call it in the next stage, so do this instead:
          env.GFR_LOADED = 'true' // just a marker; we will re-load below
        }
      }
    }

    stage('Run template pipeline') {
      steps {
        script {
          // safest is to load again (cheap); or declare a pipeline-scope var above and assign once
          def githubFileRunner = load 'poc.groovy'

          def result = githubFileRunner.call([
            agentLabel   : '',   // <= empty string means "any agent"
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
