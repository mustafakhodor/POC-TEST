import groovy.transform.Field

@Field String TARGET_ENV_URL = null

def call(Map cfg = [:]) {

  // =========================
  // Reusable helpers (one file)
  // =========================
  def stripQuotes = { s ->
    if (s == null) return null
    def t = s.toString().trim()
    if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
      t = t.substring(1, t.length() - 1).trim()
    }
    return t
  }
  def isRealVal = { v ->
    if (v == null) return false
    def t = stripQuotes(v)
    if (t == null) return false
    t = t.trim()
    if (t.isEmpty()) return false
    if (t.equalsIgnoreCase('null')) return false
    if (t == '""' || t == "''") return false
    return true
  }
  def normalize = { v -> isRealVal(v) ? stripQuotes(v) : null }

  /** Coerce anything to Map (Map.Entry -> value, else empty) */
  def asMap = { obj ->
    if (obj instanceof Map.Entry) return (obj.value instanceof Map) ? obj.value : [:]
    return (obj instanceof Map) ? obj : [:]
  }

  /**
   * Coerce shapes to a flat List:
   *  - null -> []
   *  - List -> as is
   *  - Map with {add/update/delete} arrays -> concat add+update
   *  - Map (single object) -> [that map]
   *  - Map.Entry -> recurse on value
   */
  def asList = { obj ->
    if (obj == null)         return []
    if (obj instanceof List) return obj
    if (obj instanceof Map.Entry) return asList(obj.value)
    if (obj instanceof Map) {
      def m = (Map)obj
      def acc = []
      if (m.add    instanceof List) acc.addAll((List)m.add)
      if (m.update instanceof List) acc.addAll((List)m.update)
      if (!acc.isEmpty()) return acc
      return [m]
    }
    return []
  }

  /** Return first non-null/real value found among keys */
  def firstNonNull = { Map m, List<String> keys ->
    for (k in keys) {
      if (m?.containsKey(k)) {
        def v = normalize(m[k])
        if (v != null) return v
      }
    }
    return null
  }

  /** Parse imagePath repo:tag; if tag missing, fall back to imageVersion */
  def parseRepoTag = { imagePath, imageVersion ->
    String repo = null
    String tag  = null
    if (isRealVal(imagePath)) {
      def s = imagePath
      def idx = s.lastIndexOf(':')
      if (idx > 0 && idx < s.length() - 1) {
        repo = s.substring(0, idx)
        tag  = s.substring(idx + 1)
      } else {
        repo = s
      }
    }
    if (!isRealVal(tag)) tag = normalize(imageVersion)
    [repo, tag]
  }

  /** Build a release name with prefix and slug */
  def mkRelease = { baseName, fallbackKey, prefix ->
    def base = isRealVal(baseName) ? baseName : fallbackKey
    (prefix + "-" + base.replaceAll(/\W+/, '-').toLowerCase())
  }

  // =========================
  // Stages
  // =========================

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
    def manifestJson = '''{
  "liferay": {
    "clientExtensions": {
      "add": [
        {
          "name": "moj-task-information",
          "version": "1.0.0",
          "filePath": "tasks-module/v-34-2e0b2050/clientextensions/moj-task-information.zip",
          "provenance": {
            "sourceBranch": "refs/heads/main",
            "sourceCommitId": "2e0b205002cf0ac20eac5899b4280d642a6a5372",
            "buildNumber": "v-34-2e0b2050",
            "releaseNotes": "Initial release"
          }
        },
        {
          "name": "moj-task-internal-information",
          "version": "1.0.0",
          "filePath": "tasks-module/v-34-2e0b2050/clientextensions/moj-task-internal-information.zip",
          "provenance": {
            "sourceBranch": "refs/heads/main",
            "sourceCommitId": "2e0b205002cf0ac20eac5899b4280d642a6a5372",
            "buildNumber": "v-34-2e0b2050",
            "releaseNotes": "Initial release"
          }
        },
        {
          "name": "moj-task-internal-list",
          "version": "1.0.0",
          "filePath": "tasks-module/v-34-2e0b2050/clientextensions/moj-task-internal-list.zip",
          "provenance": {
            "sourceBranch": "refs/heads/main",
            "sourceCommitId": "2e0b205002cf0ac20eac5899b4280d642a6a5372",
            "buildNumber": "v-34-2e0b2050",
            "releaseNotes": "Initial release"
          }
        },
        {
          "name": "moj-task-list",
          "version": "1.0.0",
          "filePath": "tasks-module/v-34-2e0b2050/clientextensions/moj-task-list.zip",
          "provenance": {
            "sourceBranch": "refs/heads/main",
            "sourceCommitId": "2e0b205002cf0ac20eac5899b4280d642a6a5372",
            "buildNumber": "v-34-2e0b2050",
            "releaseNotes": "Initial release"
          }
        }
      ],
      "update": [],
      "delete": []
    },
    "contentPages": {
      "add": [],
      "update": [],
      "delete": []
    }
  },
  "dynamics": {
    "solutions": {
      "add": [],
      "update": [
        {
          "name": "LegalRepresentative",
          "version": "1.0.0.46",
          "managedSolutionFilePath": "https://artifacts.moj.gov.ae/repository/legal-representative-module/v-54-acf3bdf4/crm/src/LegalRepresentative_managed.zip",
          "unmanagedSolutionFilePath": "https://artifacts.moj.gov.ae/repository/legal-representative-module/v-54-acf3bdf4/crm/src/LegalRepresentative_unmanaged.zip",
          "projects": {
            "add": [],
            "update": [
              {
                "name": "Legal Representative",
                "flopFilePath": "https://artifacts.moj.gov.ae/repository/legal-representative-module/v-54-acf3bdf4/crm/src/Legal_Representative.flop",
                "localizedResourceDataMapFilePath": null,
                "entityDataMapFilePath": null,
                "serviceConnectionDataMapFilePath": null,
                "configurationDataMapFilePath": null,
                "dataFilePath": null,
                "apis": {
                  "add": [],
                  "update": [],
                  "delete": []
                }
              }
            ],
            "delete": []
          },
          "provenance": {
            "sourceBranch": "refs/heads/main",
            "sourceCommitId": "acf3bdf4ce6d83528ff2af9070a81173f6b83e26",
            "buildNumber": "v-54-acf3bdf4",
            "releaseNotes": "the Initial release"
          }
        }
      ],
      "delete": [
        { "name": "MOJOrganizationManagement", "artifactType": "DynamicsSolution" },
        { "name": "InboundIntegration", "artifactType": "DynamicsSolution" },
        { "name": "ReportManagement", "artifactType": "DynamicsSolution" },
        { "name": "Profile", "artifactType": "DynamicsSolution" },
        { "name": "AssetManagement", "artifactType": "DynamicsSolution" },
        { "name": "DebtManagement", "artifactType": "DynamicsSolution" },
        { "name": "Session", "artifactType": "DynamicsSolution" },
        { "name": "TE", "artifactType": "DynamicsSolution" },
        { "name": "LegalOpinion", "artifactType": "DynamicsSolution" },
        { "name": "LegalRequestEngine", "artifactType": "DynamicsSolution" },
        { "name": "Decisions", "artifactType": "DynamicsSolution" },
        { "name": "Judgment", "artifactType": "DynamicsSolution" },
        { "name": "LegalFiles", "artifactType": "DynamicsSolution" }
      ]
    }
  },
  "integration": {
    "packages": {
      "add": [
        {
          "name": "ADAWQAF",
          "version": "1.0.0",
          "image": {
            "name": "InternetIdentityService",
            "version": "1.0.0",
            "imagePath": "docker-registry.moj.gov.ae/repository/moj-docker/identity/internet:v-20-ed1edd2e",
            "action": "Update",
            "provenance": {
              "sourceBranch": "refs/heads/feature/publishMetadata",
              "sourceCommitId": "ed1edd2ed65a00e9cdaaed55afd0ba0f2f418edf",
              "buildNumber": "v-20-ed1edd2e",
              "releaseNotes": "Initial release of the internet identity service."
            }
          },
          "configFilePath": null,
          "direction": "Inbound",
          "apiScope": "Internet",
          "openApiSpecsFilePath": "https://artifacts.moj.gov.ae/repository/webmethods/integrationserver/feature/pushArtifactstoNexus/adawqaf/swagger.yaml"
        }
      ],
      "update": [],
      "delete": []
    }
  },
  "identity": {
    "internet": {
      "name": "InternetIdentityService",
      "version": "1.0.0",
      "imagePath": "docker-registry.moj.gov.ae/repository/moj-docker/identity/internet:v-20-ed1edd2e",
      "action": "Start",
      "provenance": {
        "sourceBranch": "refs/heads/feature/publishMetadata",
        "sourceCommitId": "ed1edd2ed65a00e9cdaaed55afd0ba0f2f418edf",
        "buildNumber": "v-20-ed1edd2e",
        "releaseNotes": "Initial release of the internet identity service."
      }
    },
    "intranet": {
      "name": "IntranetIdentityService",
      "version": "1.0.0",
      "imagePath": "docker-registry.moj.gov.ae/repository/moj-docker/identity/intranet:v-20-ed1edd2e",
      "action": "Start",
      "provenance": {
        "sourceBranch": "refs/heads/feature/publishMetadata",
        "sourceCommitId": "ed1edd2ed65a00e9cdaaed55afd0ba0f2f418edf",
        "buildNumber": "v-20-ed1edd2e",
        "releaseNotes": "Initial release of the intranet identity service."
      }
    }
  }
}'''
    writeFile file: 'deployment-manifest.json', text: manifestJson
    echo "Generated ${pwd()}/deployment-manifest.json"
  }

  stage('Extract Flowon commands (solutions)') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def conn = (TARGET_ENV_URL ?: '').trim()
    if (!conn) error 'TARGET_ENV_URL is empty. Make sure you set it in a prior stage.'

    def buildCommandsForSolutions = { List sols, String bucketLabel ->
      def cmds = []
      (sols ?: []).each { solAny ->
        def sol = asMap(solAny)
        def solName = firstNonNull(sol, ['name', 'solution']) ?: '(unknown)'
        echo "Processing ${bucketLabel} solution: ${solName}"

        def managedZip = firstNonNull(sol, ['managedSolutionFilePath', 'managedSolution'])
        if (managedZip) {
          def parts = []
          parts << 'flowon-dynamics i'
          parts << "--connectionstring \"${conn}\""
          parts << "-s \"${managedZip}\""
          parts << '-oc true'
          parts << '-l Verbose'
          cmds << parts.join(' ')
        } else {
          echo "WARN: solution '${solName}' missing 'managedSolutionFilePath' -> skipping solution import"
        }

        def projects = asList(sol.projects ?: sol.projets)
        if (!projects) echo "INFO: solution '${solName}' has no projects to import"

        projects.each { projAny ->
          def proj = asMap(projAny)
          def flopPath  = firstNonNull(proj, ['flopFilePath', 'flop'])
          def entityMap = firstNonNull(proj, ['entityDataMapFilePath', 'entityDataMap'])
          def locResMap = firstNonNull(proj, ['localizedResourceDataMapFilePath', 'localizedResourceDataMap'])
          def dataFile  = firstNonNull(proj, ['dataFilePath', 'dataFile'])

          if (!flopPath) {
            def pname = normalize(proj.name) ?: '?'
            echo "WARN: project '${pname}' missing 'flopFilePath' -> skipping"
            return
          }

          def p = []
          p << 'flowon-dynamics i'
          p << "--connectionstring \"${conn}\""
          p << "-p \"${flopPath}\""
          if (isRealVal(entityMap)) p << "-m \"${entityMap}\""
          if (isRealVal(locResMap)) p << "-m \"${locResMap}\""
          if (isRealVal(dataFile))  p << "-d \"${dataFile}\""
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
      commands += buildCommandsForSolutions(asList(solutionsNode?.add),    'add')
      commands += buildCommandsForSolutions(asList(solutionsNode?.update), 'update')
    }

    if (commands.isEmpty()) {
      echo 'No dynamics solution/project commands generated.'
    } else {
      echo "Generated commands:\n${commands.join('\n')}"
      // commands.each { c -> sh "set -e; echo Executing: ${c} ; ${c}" }
    }
  }

  stage('Extract Client Extension command from manifest') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def clientExts = []
    clientExts += asList(manifest?.liferay?.clientExtensions?.add)
    clientExts += asList(manifest?.liferay?.clientExtensions?.update)

    if (!clientExts) {
      echo 'No client extensions found in manifest.'
      return
    }

    def commands = []
    clientExts.each { raw ->
      def ext = asMap(raw)
      def path = normalize(ext.filePath)
      if (isRealVal(path)) {
        // Example: build real scp/rsync or other publish command here
        commands << path
      }
    }

    if (commands.isEmpty()) {
      echo 'No client extension commands generated.'
    } else {
      echo "Generated Client Extension commands:\n${commands.join('\n')}"
      // commands.each { c -> sh "set -e; echo Executing: ${c} ; ${c}" }
    }
  }

  stage('Extract APIs command from manifest') {
  def manifest = readJSON file: 'deployment-manifest.json'

  def apis = []

  def collectApis = { Object solutionsNode, String bucket ->
    def sols = asList(solutionsNode)
    sols.each { solAny ->
      def sol = asMap(solAny)
      def solName = firstNonNull(sol, ['name', 'solution']) ?: '(unknown-solution)'

      def projects = asList(sol.projects ?: sol.projets)
      projects.each { projAny ->
        def proj = asMap(projAny)
        def projName = normalize(proj.get('name')) ?: '(unknown-project)'

        // If no apis/api node, skip this project entirely
        def apisNode = proj.apis ?: proj.api
        if (apisNode == null) return

        def apiList = asList(apisNode)
        if (!apiList) return // nothing to do for this project

        apiList.each { apiAny ->
          def api = asMap(apiAny)
          def apiName   = firstNonNull(api, ['name', 'apiName']) ?: '(unknown-api)'
          def specsPath = firstNonNull(api, ['openApiSpecsFilePath', 'openApiSpecs', 'specs', 'filePath'])

          def imgMap    = asMap(api.image ?: [:])
          def imagePath = firstNonNull(imgMap, ['imagePath', 'path', 'name'])
          def actionVal = firstNonNull(api, ['action']) ?: firstNonNull(imgMap, ['action'])

          apis << [
            solution: solName,
            project : projName,
            name    : apiName,
            specs   : specsPath,
            image   : imagePath,
            action  : actionVal
          ]
        }
      }
    }
  }

  def solutionsNode = manifest?.dynamics?.solutions
  if (solutionsNode) {
    collectApis(solutionsNode?.add,    'add')
    collectApis(solutionsNode?.update, 'update')
  }

  // If nothing collected, just exit the stage silently
  if (apis.isEmpty()) return

  apis.each { api ->
    echo '======================================================='
    echo "Solution: ${api.solution}"
    echo "Project : ${api.project}"
    echo "API     : ${api.name}"
    echo "Specs   : ${api.specs ?: '(none)'}"
    echo "Image   : ${api.image ?: '(none)'}"
    echo "Action  : ${api.action ?: '(none)'}"
    echo '======================================================='

    if (isRealVal(api.action) && isRealVal(api.image)) {
      def projKey = api.project.toLowerCase().replaceAll(/\s+/, '-')
      switch (api.action.toLowerCase()) {
        case ['upgrade','update','install']:
          echo "[MOCK] helm upgrade -i ${projKey} ${api.image} --namespace <namespace>"
          break
        case 'restart':
          echo "[MOCK] kubectl rollout restart deployment/${projKey} -n <namespace>"
          break
        default:
          echo "[MOCK] (unknown action '${api.action}') — skipping k8s step"
      }
    }

    if (isRealVal(api.name) && isRealVal(api.specs)) {
      echo "[MOCK] Check if API ${api.name} exists: http GET \$API_GATEWAY_URL/rest/apigateway/apis --auth \$CRED_ID"
      echo "[MOCK] If API exists and active -> Deactivate: curl -X PUT \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>/deactivate"
      echo "[MOCK] If API exists -> Update with specs: curl -X PUT \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>?overwriteTags=true -F \"file=@${api.specs}\" -F \"apiName=${api.name}\""
      echo "[MOCK] Else Create: curl -X POST \$API_GATEWAY_URL/rest/apigateway/apis -F \"file=@${api.specs}\" -F \"apiName=${api.name}\""
      echo "[MOCK] Activate API: curl -X PUT \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>/activate"
      echo "[MOCK] Verify API: http GET \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>"
    }
  }
}


  stage('Extract Integration Server command from manifest') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def pkgs = []
    def collectPkgs = { List list, String bucket ->
      (list ?: []).each { raw ->
        def name     = normalize(raw?.name)
        def version  = normalize(raw?.version)
        def img      = raw?.image ?: [:]
        def imgName  = normalize(img?.name)
        def imgPath  = normalize(img?.imagePath)
        def imgVer   = normalize(img?.version)
        def action   = normalize(img?.action)   // Update/Upgrade/Install/Restart
        def cfgPath  = normalize(raw?.configFilePath)

        pkgs << [
          name          : name ?: '(unknown)',
          version       : version,
          imageName     : imgName,
          imagePath     : imgPath,
          imageVersion  : imgVer,
          action        : action,
          configFilePath: cfgPath,
          bucket        : bucket
        ]
      }
    }

    def pnode = manifest?.integration?.packages
    if (pnode) {
      collectPkgs(asList(pnode?.add),    'add')
      collectPkgs(asList(pnode?.update), 'update')
    }

    if (pkgs.isEmpty()) {
      echo 'No integration packages found under integration.packages.(add|update).'
      return
    }

    def lines = []
    pkgs.each { pkg ->
      def rel = mkRelease(pkg.name, pkg.name, 'integration')
      def (repo, tag) = parseRepoTag(pkg.imagePath, pkg.imageVersion)

      lines << '# ==================================================================='
      lines << "# ${pkg.bucket?.toUpperCase() ?: 'BUCKET'} :: ${pkg.name}${pkg.version ? " v${pkg.version}" : ''}"
      lines << "# imageName: ${pkg.imageName ?: '(none)'}"
      lines << "# imagePath: ${pkg.imagePath ?: '(none)'}"
      lines << "# action   : ${pkg.action ?: '(none)'}"
      lines << "# config   : ${pkg.configFilePath ?: '(none)'}"
      lines << '# ==================================================================='

      if (isRealVal(pkg.configFilePath)) {
        lines << "echo '[MOCK] Using integration config: ${pkg.configFilePath}'"
      }

      def act = (pkg.action ?: '').toLowerCase()
      if (['upgrade','update','install'].contains(act) && isRealVal(repo) && isRealVal(tag)) {
        lines << "echo '[MOCK] Helm upgrade integration server'"
        lines << "echo helm upgrade -i ${rel} <chart-path-or-name> -n <namespace> \\"
        lines << "  --set image.repository=${repo} \\"
        lines << "  --set image.tag=${tag}" + (isRealVal(pkg.configFilePath) ? ' \\' : '')
        if (isRealVal(pkg.configFilePath)) {
          lines << "  --set-file app.config=${pkg.configFilePath}"
        }
      } else if (act == 'restart') {
        lines << "echo '[MOCK] Restart integration deployment'"
        lines << "echo kubectl rollout restart deployment/${rel} -n <namespace>"
      } else {
        lines << "echo '[INFO] Unknown or missing action for ${pkg.name}; expected one of: Upgrade/Update/Install/Restart. Skipping.'"
      }
    }

    echo 'Generated Integration Server mock commands:\n' + lines.join('\n')
  }

  stage('Extract Identity commands from manifest') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def nodes = []
    def idNode = manifest?.identity ?: [:]
    if (idNode instanceof Map) {
      if (idNode.containsKey('internet')) nodes << [key:'internet', obj:idNode.internet]
      if (idNode.containsKey('intranet')) nodes << [key:'intranet', obj:idNode.intranet]
    }

    if (nodes.isEmpty()) {
      echo 'No identity nodes found (identity.internet / identity.intranet).'
      return
    }

    def lines = []
    nodes.each { n ->
      def o        = (n.obj instanceof Map) ? n.obj : [:]
      def name     = normalize(o.name) ?: "(unknown-${n.key})"
      def version  = normalize(o.version)
      def image    = normalize(o.imagePath)
      def action   = normalize(o.action)   // Start/Install/Update/Upgrade/Restart/Stop
      def rel      = mkRelease(name, n.key, 'identity')
      def (repo, tag) = parseRepoTag(image, null)

      lines << '# ==================================================================='
      lines << "# IDENTITY :: ${n.key.toUpperCase()} :: ${name}${version ? " v${version}" : ''}"
      lines << "# imagePath: ${image ?: '(none)'}"
      lines << "# action   : ${action ?: '(none)'}"
      lines << '# ==================================================================='

      if (!isRealVal(action)) {
        lines << "echo '[INFO] Missing action for ${name}; expected Start/Install/Update/Upgrade/Restart/Stop. Skipping.'"
      } else {
        switch (action.toLowerCase()) {
          case ['start','install','update','upgrade']:
            if (isRealVal(repo) && isRealVal(tag)) {
              lines << "echo '[MOCK] Helm upgrade ${n.key} identity service'"
              lines << "echo helm upgrade -i ${rel} <chart-path-or-name> -n <namespace> \\"
              lines << "  --set image.repository=${repo} \\"
              lines << "  --set image.tag=${tag}"
            } else {
              lines << "echo '[INFO] Missing image repo/tag for ${name}; cannot perform Helm upgrade. Skipping.'"
            }
            break
          case 'restart':
            lines << "echo '[MOCK] Restart ${n.key} identity deployment'"
            lines << "echo kubectl rollout restart deployment/${rel} -n <namespace>"
            break
          case 'stop':
            lines << "echo '[MOCK] Stop ${n.key} identity deployment (scale to 0)'"
            lines << "echo kubectl scale deployment/${rel} -n <namespace> --replicas=0"
            break
          default:
            lines << "echo '[INFO] Unknown action \"${action}\" for ${name}; expected Start/Install/Update/Upgrade/Restart/Stop. Skipping.'"
            break
        }
      }
    }

    echo 'Generated Identity mock commands:\n' + lines.join('\n')
  }

} // end call

return this
