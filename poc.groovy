def call(Map config = [:]) {
  def githubUrl     = require(config, 'githubUrl')
  def gitRef        = require(config, 'gitRef')
  def filePath      = require(config, 'filePath')
  def command       = require(config, 'command')

  def githubCredsId = (config.githubCredsId ?: '').trim()
  def agentLabel    = (config.agentLabel ?: '').toString()

  node(agentLabel) {
        stage('Checkout GitHub') {
          def checkoutDir = "${env.WORKSPACE}/gh-src"
          dir(checkoutDir) {
            deleteDir()

            def scmCfg = [
              $class: 'GitSCM',
              branches: [[name: gitRef]],
              userRemoteConfigs: [[url: githubUrl] + (githubCredsId ? [credentialsId: githubCredsId] : [:])],
              extensions: [
                [$class: 'WipeWorkspace'],
                [$class: 'CloneOption', depth: 1, shallow: true, noTags: true, honorRefspec: true]
              ]
            ]
            checkout(scmCfg)

            sh """#!/bin/bash -e
              test -s '${filePath}' || { echo 'File not found or empty: ${filePath}'; exit 1; }
            """
          }
        }

        stage('Read deployment.manifest.json & Build Commands') {
      steps {
        script {
          def manifestPath = "${env.WORKSPACE}/deployment.manifest.json"
          if (!fileExists(manifestPath)) {
            error "Manifest file not found: ${manifestPath}"
          }

          def manifest = readJSON file: manifestPath

          def buildCommands = { dynamicsList ->
            dynamicsList.collectMany { dyn ->
              def managedSolutionPath = dyn.managedSolution
              def projects = (dyn.projects ?: dyn.projets ?: [])
              projects.collect { proj ->
                def flopPath  = proj.flop
                def dataFile  = proj.dataFile ?: ''
                def entityMap = proj.entityDataMap ?: ''

                def parts = []
                parts << 'flowon-dynamics i'
                parts << "--connectionstring \"${env.FLOWON_CONN ?: ''}\""
                parts << "-p \"${flopPath}\""
                parts << "-s \"${managedSolutionPath}\""
                parts << '-oc true'                                        // ← fixed here
                if (dataFile)  parts << "-d \"${dataFile}\""
                if (entityMap) parts << "-m \"${entityMap}\""

                return parts.join(' ')
              }
            }
          }

          def commands = []
          if (manifest.add?.dynamics)    { commands += buildCommands(manifest.add.dynamics) }
          if (manifest.update?.dynamics) { commands += buildCommands(manifest.update.dynamics) }

          if (commands.isEmpty()) {
            echo 'No dynamics entries found to build commands.'
            return
          }

          echo "Generated commands:\n${commands.join('\n')}"

          // Execute them (remove this loop if you only want to print)
          commands.each { cmd ->
            sh """
          set -e
          echo "Executing: ${cmd}"
          ${cmd}
        """
          }
        }
      }
        }
  }
}

@NonCPS
private String require(Map m, String key) {
  def v = m[key]
  if (v == null || (v instanceof CharSequence && v.toString().trim().isEmpty())) {
    throw new IllegalArgumentException("Missing required parameter: '${key}'")
  }
  return v.toString()
}

return this
