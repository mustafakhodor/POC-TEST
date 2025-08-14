// poc.groovy  (REPLACE your file with this)
// Reusable step: checkout GitHub, read a file, run a command that outputs JSON,
// parse it, return the parsed Map.

def call(Map config = [:]) {
  def githubUrl = require(config, 'githubUrl')
  def gitRef    = require(config, 'gitRef')
  def filePath  = require(config, 'filePath')
  def command   = require(config, 'command')
  def credsId   = (config.githubCredsId ?: '').trim()

  def checkoutDir = "${env.WORKSPACE}/gh-src"

  dir(checkoutDir) {
    deleteDir()

    def extensions = [
      [$class: 'WipeWorkspace'],
      [$class: 'CloneOption', depth: 1, noTags: true, shallow: true, honorRefspec: true]
    ]
    def scmCfg = [
      $class: 'GitSCM',
      branches: [[name: gitRef]],
      userRemoteConfigs: [[url: githubUrl]],
      extensions: extensions
    ]
    if (credsId) {
      scmCfg.userRemoteConfigs = [[url: githubUrl, credentialsId: credsId]]
    }
    checkout(scmCfg)

    // Validate file existence
    sh """ test -s "${filePath}" || (echo "File not found or empty: ${filePath}" && exit 1) """

    // Run the command with the file and capture JSON output
    def fullPath = "${pwd()}/${filePath}"
    def rawOut = sh(script: "${command} '${fullPath}'", returnStdout: true).trim()
    echo "Raw command output (first 500 chars):\n${rawOut.take(500)}"

    // Parse JSON
    def parsed
    try {
      parsed = readJSON text: rawOut
    } catch (e) {
      error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
    }

    // Save for later / artifacts
    writeJSON file: "${checkoutDir}/command-result.json", json: parsed, pretty: 2
    stash name: 'github-process-result', includes: 'gh-src/command-result.json'

    return parsed
  }
}

// helper
def require(Map m, String key) {
  if (!m.containsKey(key) || m[key] == null || "${m[key]}".trim() == '') {
    error "poc.groovy: missing required argument '${key}'"
  }
  m[key]
}

return this
