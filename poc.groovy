// poc.groovy — callable scripted step
def call(Map config = [:]) {
  def githubUrl     = require(config, 'githubUrl')
  def gitRef        = require(config, 'gitRef')
  def filePath      = require(config, 'filePath')
  def command       = require(config, 'command')
  def githubCredsId = (config.githubCredsId ?: '').trim()
  def agentLabel    = (config.agentLabel ?: '').toString()    // '' => any agent

  // '' picks any available agent
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

  // Run the command; fail fast on errors
  String rawOut = sh(
    script: """
      set -euo pipefail
      ${command} '${fullPath}'
    """.stripIndent(),
    returnStdout: true
  ).trim()

  echo "Raw command output (first 500 chars):\n${rawOut.take(500)}"

  // Parse JSON without plugins
  def parsed
  try {
    def slurper = new groovy.json.JsonSlurperClassic()
    parsed = slurper.parseText(rawOut)  // Map/List
  } catch (e) {
    error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
  }

  // Pretty-print to file and stash it
  def pretty = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(parsed))
  writeFile file: "${workDir}/command-result.json", text: pretty

  stash name: 'github-process-result', includes: 'gh-src/command-result.json'

  // Optional branching
  if (parsed instanceof Map && parsed.status == 'success') {
    echo "✅ Success branch"
  } else if (parsed instanceof Map && parsed.status == 'warning') {
    echo "⚠️ Warning: ${parsed.message ?: 'No message provided'}"
  } else {
    echo "ℹ️ Non-success status: ${(parsed instanceof Map && parsed.status) ? parsed.status : 'unknown'}"
  }

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
