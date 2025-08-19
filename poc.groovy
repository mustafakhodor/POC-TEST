import groovy.transform.Field

@Field String TARGET_ENV_URL = null

def call(Map cfg = [:]) {
  stage('Determine target environment URL') {
    def envUrlByName = [
      dev   : 'https://devmerge.netways1.com',
      qa    : 'https://qamerge.netways1.com',
      stage1: 'https://stage1merge.netways1.com',
      stage2: 'https://stage2merge.netways1.com',
      prod  : 'https://merge.netways1.com'
    ]

    def key = (params.ENVIRONMENT ?: '').toLowerCase().replaceAll(/\s+/, '').trim()
    def url = envUrlByName[key]
    if (!url) {
      error "No URL mapping found for ENVIRONMENT='${params.ENVIRONMENT}'. Update envUrlByName."
    }

    TARGET_ENV_URL = url
    echo "TARGET_ENV_URL: ${TARGET_ENV_URL}"
  }

  stage('Create deployment manifest file') {
    def manifestJson = '''<... your same big JSON ...>'''
    writeFile file: 'deployment-manifest.json', text: manifestJson

    // ✅ use pwd(), not env.WORKSPACE
    echo "Generated ${pwd()}/deployment-manifest.json"
  }

  stage('Extract Flowon commands (solutions)') {
    // scripted pipeline: no "steps", just code here
    def manifest = readJSON file: 'deployment-manifest.json'

    def conn = (TARGET_ENV_URL ?: '').trim()
    if (!conn) {
      error 'TARGET_ENV_URL is empty. Make sure you set it in a prior stage.'
    }

    def buildCommandsForSolutions = { List solutions, String bucketLabel ->
      def cmds = []
      (solutions ?: []).each { sol ->
        def solName = sol.solution ?: '(unknown)'
        echo "Processing ${bucketLabel} solution: ${solName}"

        if (sol.managedSolution) {
          def importParts = []
          importParts << 'flowon-dynamics i'
          importParts << "--connectionstring \"${conn}\""
          importParts << "-s \"${sol.managedSolution}\""
          importParts << '-oc true'
          importParts << '-l Verbose'
          cmds << importParts.join(' ')
        } else {
          echo "WARN: solution '${solName}' missing 'managedSolution' -> skipping solution import"
        }

        def projects = (sol.projects ?: sol.projets ?: [])
        if (!projects) {
          echo "WARN: solution '${solName}' has no 'projects' -> nothing to import at project level"
        }

        projects.each { proj ->
          def flopPath = proj.flop
          if (!flopPath) {
            echo "WARN: project '${proj.name ?: '?'}' missing 'flop' -> skipping"
            return
          }

          def p = []
          p << 'flowon-dynamics i'
          p << "--connectionstring \"${conn}\""
          p << "-p \"${flopPath}\""
          if (proj.entityDataMap)            p << "-m \"${proj.entityDataMap}\""
          if (proj.localizedResourceDataMap) p << "-m \"${proj.localizedResourceDataMap}\""
          if (proj.dataFile)                 p << "-d \"${proj.dataFile}\""
          p << '-l Verbose'
          cmds << p.join(' ')
        }
      }
      return cmds
    }

    def commands = []
    def solutionsNode = manifest?.dynamics?.solutions
    if (!solutionsNode) {
      echo 'No dynamics.solutions section found in manifest.'
    } else {
      commands += buildCommandsForSolutions(solutionsNode.add    as List ?: [], 'add')
      commands += buildCommandsForSolutions(solutionsNode.update as List ?: [], 'update')
    }

    if (commands.isEmpty()) {
      echo 'No dynamics solution/project commands generated.'
    } else {
      echo "Generated commands:\n${commands.join('\n')}"
      // If you want to execute them:
      // commands.each { c -> sh "set -e; echo Executing: ${c}; ${c}" }
    }
  }

  stage('Extract Client Extension command from manifest') { echo 'TODO' }
  stage('Extract APIs command from manifest') { echo 'TODO' }
  stage('Extract gateways command from manifest') { echo 'TODO' }
  stage('Extract Integration Server command from manifest') { echo 'TODO' }
}
return this
