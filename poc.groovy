// poc.groovy
import groovy.transform.Field

@Field String TARGET_ENV_URL = null  // script-scope, survives across stages

def call(Map cfg = [:]) {

  stage('Determine target environment URL') {
    def envUrlByName = [
      dev   : 'https://devmerge.netways1.com',
      qa    : 'https://qamerge.netways1.com',
      stage1: 'https://stage1merge.netways1.com',
      stage2: 'https://stage2merge.netways1.com',
      prod  : 'https://merge.netways1.com'
    ]

    def key = params.ENVIRONMENT.toLowerCase().replaceAll(/\s+/, '').trim()
    echo "Normalized key: '${key}'"

    def url = envUrlByName[key]
    if (!url) {
      error "No URL mapping found for ENVIRONMENT='${params.ENVIRONMENT}'. Update envUrlByName."
    }

    // Store in script-scope var (reliable across later stages in this file)
    TARGET_ENV_URL = url
    echo "TARGET_ENV_URL (local): ${TARGET_ENV_URL}"

    // (Optional) try to set Jenkins env too — may be null for you, so we won't rely on it
    env.TARGET_ENV_URL = "${url}"
    echo "env.TARGET_ENV_URL (may be null in sandbox): ${env.TARGET_ENV_URL}"

    currentBuild.displayName = "#${env.BUILD_NUMBER} • ${params.ENVIRONMENT}"
  }

  stage('Create deployment manifest file') {
    echo "Using TARGET_ENV_URL locally: ${TARGET_ENV_URL}"
    // ... writeFile, etc.
  }

  stage('Extract Flowon commands (solutions)') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def buildCommandsFor = { dynamicsList ->
      (dynamicsList ?: []).collectMany { dyn ->
        def cmds = []

        if (dyn.managedSolution) {
          def parts = []
          parts << 'flowon-dynamics i'
          // ✅ provide env var via withEnv when running commands
          parts << "--connectionstring \"${TARGET_ENV_URL}\""
          parts << "-s \"${dyn.managedSolution}\""
          parts << '-oc true'
          parts << '-l Verbose'
          cmds << parts.join(' ')
        } else {
          echo "WARN: dynamics entry missing 'managedSolution' -> skipping solution import"
        }

        def projects = (dyn.projects ?: dyn.projets ?: [])
        if (!projects) echo "WARN: no projects under solution '${dyn.solution ?: '?'}'"

        projects.each { proj ->
          def flopPath = proj.flop
          if (!flopPath) {
            echo "WARN: project '${proj.name ?: '?'}' missing 'flop' -> skipping"
            return
          }
          def p = []
          p << 'flowon-dynamics i'
          p << "--connectionstring \"${TARGET_ENV_URL}\""
          p << "-p \"${flopPath}\""
          if (proj.entityDataMap)            p << "-m \"${proj.entityDataMap}\""
          if (proj.localizedResourceDataMap) p << "-m \"${proj.localizedResourceDataMap}\""
          if (proj.dataFile)                 p << "-d \"${proj.dataFile}\""
          p << '-l Verbose'
          cmds << p.join(' ')
        }
        return cmds
      }
    }

    def commands = []
    if (manifest.add?.dynamics)    commands += buildCommandsFor(manifest.add.dynamics)
    if (manifest.update?.dynamics) commands += buildCommandsFor(manifest.update.dynamics)

    if (commands.isEmpty()) {
      echo 'No dynamics entries found to build commands.'
    } else {
      echo "Generated commands:\n${commands.join('\n')}"
      // To execute safely with the env var available to the process:
      // withEnv(["TARGET_ENV_URL=${TARGET_ENV_URL}"]) {
      //   commands.each { c -> sh "set -e; echo Executing: ${c}; ${c}" }
      // }
    }
  }

  // Optional TODO stages …

  // Return so the outer Jenkinsfile can set env globally if it wants
  return [env: params.ENVIRONMENT, url: TARGET_ENV_URL]
}
return this
