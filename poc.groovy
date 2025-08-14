// poc.groovy — callable scripted step
def call(Map config = [:]) {
  // --- Required inputs (fail fast) ---
  def githubUrl     = require(config, 'githubUrl')
  def gitRef        = require(config, 'gitRef')
  def filePath      = require(config, 'filePath')
  def command       = require(config, 'command')
  def githubCredsId = (config.githubCredsId ?: '').trim()
  def agentLabel    = (config.agentLabel ?: 'master')

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

        // Validate the file exists and is non-empty
        sh """#!/bin/bash -e
          test -s '${filePath}' || { echo 'File not found or empty: ${filePath}'; exit 1; }
        """
      }
    }

    stage('Run command & parse JSON') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def fullPath = "${workDir}/${filePath}"

      // Run the user-supplied command against the file — command must print JSON to stdout
      String rawOut = sh(script: "${command} '${fullPath}'", returnStdout: true).trim()
      echo "Raw command output (first 500 chars):\n${rawOut.take(500)}"

      // Parse the JSON
      def parsed
      try {
        parsed = readJSON text: rawOut  // requires Pipeline Utility Steps plugin
      } catch (e) {
        error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
      }

      // Persist & stash for outer pipeline post steps
      writeJSON file: "${workDir}/command-result.json", json: parsed, pretty: 2
      stash name: 'github-process-result', includes: 'gh-src/command-result.json'

      // Optional branching
      if (parsed.status == 'success') {
        echo "✅ Success branch"
      } else if (parsed.status == 'warning') {
        echo "⚠️ Warning: ${parsed.message ?: 'No message provided'}"
      } else {
        echo "ℹ️ Non-success status: ${parsed.status ?: 'unknown'}"
      }

      return parsed
    }
  }
}

// Simple required-arg helper
@NonCPS
private String require(Map m, String key) {
  def v = m[key]
  if (v == null || (v instanceof CharSequence && v.toString().trim().isEmpty())) {
    throw new IllegalArgumentException("Missing required parameter: '${key}'")
  }
  return v.toString()
}

// IMPORTANT: make the loaded script return an object with the call() method
return this
