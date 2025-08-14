def call(Map config = [:]) {
  def githubUrl     = require(config, 'githubUrl')
  def gitRef        = require(config, 'gitRef')
  def filePath      = require(config, 'filePath')
  def command       = require(config, 'command')

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

        stage('Run command & parse JSON') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def fullPath = "${workDir}/${filePath}"

      String rawOut = sh(
            script: """
            set -eu
            ${command} '${fullPath}'
            """.stripIndent(),
            returnStdout: true
        ).trim()

      echo "Raw command output (first 500 chars):\n${rawOut.take(500)}"

      def parsed
      try {
        parsed = readJSON text: rawOut
        } catch (e) {
        error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
      }

      writeJSON file: "${workDir}/command-result.json", json: parsed, pretty: 2

      return parsed
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
