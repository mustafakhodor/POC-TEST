// poc.groovy — callable scripted step loaded by Jenkinsfile via `load 'poc.groovy'`
def call(Map config = [:]) {
  // --- Required inputs (fail fast) ---
  def githubUrl     = require(config, 'githubUrl')
  def gitRef        = require(config, 'gitRef')
  def filePath      = require(config, 'filePath')
  def command       = require(config, 'command')

  // --- Optional inputs ---
  def githubCredsId = (config.githubCredsId ?: '').trim()
  def agentLabel    = (config.agentLabel ?: '').toString()  // '' => any agent

  // Use any available agent if agentLabel is empty string
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

        // Validate the file exists and is non-empty (Linux agents)
        sh """#!/bin/bash -e
          test -s '${filePath}' || { echo 'File not found or empty: ${filePath}'; exit 1; }
        """
      }
    }

    stage('Run command & parse JSON') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def fullPath = "${workDir}/${filePath}"

      // Run the command; must print JSON to stdout
     String rawOut = sh(
  script: """
    set -eu
    ${command} '${fullPath}'
  """.stripIndent(),
  returnStdout: true
).trim()


      echo "Raw command output (first 500 chars):\n${rawOut.take(500)}"

      // Parse JSON using built-in Groovy (no Pipeline Utility Steps plugin required)
      def parsed
      try {
        def slurper = new groovy.json.JsonSlurperClassic()
        parsed = slurper.parseText(rawOut)  // Map/List
      } catch (e) {
        error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
      }

      // Pretty-print to file and stash it (no writeJSON needed)
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

// Helper to enforce required params
@NonCPS
private String require(Map m, String key) {
  def v = m[key]
  if (v == null || (v instanceof CharSequence && v.toString().trim().isEmpty())) {
    throw new IllegalArgumentException("Missing required parameter: '${key}'")
  }
  return v.toString()
}

// Ensure `load 'poc.groovy'` returns an object with call()
return this
