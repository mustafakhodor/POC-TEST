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
        {
          "name": "MOJOrganizationManagement",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "InboundIntegration",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "ReportManagement",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "Profile",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "AssetManagement",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "DebtManagement",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "Session",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "TE",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "LegalOpinion",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "LegalRequestEngine",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "Decisions",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "Judgment",
          "artifactType": "DynamicsSolution"
        },
        {
          "name": "LegalFiles",
          "artifactType": "DynamicsSolution"
        }
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

  stage('Extract Client Extension command from manifest') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def clientExts = []
    clientExts += manifest?.liferay?.clientExtensions?.add ?: []
    clientExts += manifest?.liferay?.clientExtensions?.update ?: []

    if (!clientExts) {
      echo 'No client extensions found in manifest.'
      return
    }

    def commands = []
    clientExts.each { ext ->
      def destPath = ext.filePath
      def cmd = """sshpass -p password scp -o StrictHostKeyChecking=no -r \\
                 ${pwd()}/artifacts/clientextensions/*.zip \\
                 liferayUser@ip:${destPath}"""
      commands << cmd.stripIndent()
    }

    if (commands.isEmpty()) {
      echo 'No client extension commands generated.'
    } else {
      echo "Generated Client Extension commands:\n${commands.join('\n')}"
    // To actually run them:
    // commands.each { c -> sh "set -e; echo Executing: ${c}; ${c}" }
    }
  }

  stage('Extract APIs command from manifest') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def apis = []
    def collectApis = { List solutions, String bucket ->
      (solutions ?: []).each { sol ->
        def solName = sol.solution ?: '(unknown-solution)'
        (sol.projects ?: sol.projets ?: []).each { proj ->
          def projName = proj.name ?: '(unknown-project)'
          (proj.api ?: []).each { api ->
            apis << [
              name    : api.name,
              project : projName,
              specs   : api.openApiSpecs,
              image   : api.image.path,
              action   : api.image.action,
            ]
          }
        }
      }
    }

    def solutionsNode = manifest?.dynamics?.solutions
    if (solutionsNode) {
      collectApis(solutionsNode.add    as List ?: [], 'add')
      collectApis(solutionsNode.update as List ?: [], 'update')
    }

    if (apis.isEmpty()) {
      echo 'No APIs found under dynamics.solutions.*.projects[].api[].'
      return
    }

    apis.each { api ->
      echo '======================================================='
      echo "API: ${api.name} (Project: ${api.project})"
      echo "Specs: ${api.specs}"
      echo "Image: ${api.image}"
      echo "Action: ${api.action}"
      echo '======================================================='

      // === Mock Deployment Flow ===
      if (api.action?.toLowerCase() == 'upgrade') {
        echo "[MOCK] helm upgrade -i ${api.project.toLowerCase()} ${api.image} --namespace <namespace>"
    } else if (api.action?.toLowerCase() == 'restart') {
        echo "[MOCK] kubectl rollout restart deployment/${api.project.toLowerCase()} -n <namespace>"
      }

      echo "[MOCK] Check if API ${api.name} exists: http GET \$API_GATEWAY_URL/rest/apigateway/apis --auth \$CRED_ID"

      echo "[MOCK] If API exists and active -> Deactivate: curl -X PUT \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>/deactivate"

      echo "[MOCK] If API exists -> Update with specs: curl -X PUT \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>?overwriteTags=true -F \"file=@${api.specs}\" -F \"apiName=${api.name}\""

      echo "[MOCK] Else Create: curl -X POST \$API_GATEWAY_URL/rest/apigateway/apis -F \"file=@${api.specs}\" -F \"apiName=${api.name}\""

      echo "[MOCK] Activate API: curl -X PUT \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>/activate"

      echo "[MOCK] Verify API: http GET \$API_GATEWAY_URL/rest/apigateway/apis/<API_ID>"
    }

  // deploy to k8s ( if action upgrade helm upgrade else restart pod)
  //  check if api exist
  // if exist and active then deactivate it then update
  // else create it
  // activate api
  // verify api
  }

  stage('Extract Integration Server command from manifest') {
      def manifest = readJSON file: 'deployment-manifest.json'

      def pkgs = []
      def collectPkgs = { List list, String bucket ->
        (list ?: []).each { pkg ->
          pkgs << [
            name           : pkg.name ?: '',
            version        : pkg.version ?: '',
            imageName      : pkg.image?.name ?: '',
            imagePath      : pkg.image?.path ?: '',
            imageVersion   : pkg.image?.version ?: '',
            action         : pkg.image?.action ?: '',
            configFilePath : pkg.configFilePath ?: '',
            bucket         : bucket
          ]
        }
      }

      def pnode = manifest?.integration?.packages
      if (pnode) {
        collectPkgs(pnode.add    as List ?: [], 'add')
        collectPkgs(pnode.update as List ?: [], 'update')
      }

      if (pkgs.isEmpty()) {
        echo 'No integration packages found under integration.packages.(add|update).'
        return
      }

      def lines = []
      pkgs.each { pkg ->
        def rel = "integration-${pkg.name.replaceAll(/\\W+/, '-').toLowerCase()}"// change later if you want to map by ENVIRONMENT

        lines << '# ==================================================================='
        lines << "# ${pkg.bucket.toUpperCase()} :: ${pkg.name} v${pkg.version}"
        lines << "# imageName: ${pkg.imageName}"
        lines << "# imagePath: ${pkg.imagePath}"
        lines << "# action   : ${pkg.action}"
        lines << "# config   : ${pkg.configFilePath}"
        lines << '# ==================================================================='

        if (pkg.configFilePath) {
          lines << "echo \"[MOCK] Using integration config: ${pkg.configFilePath}\""
        }

        if (pkg.action?.equalsIgnoreCase('Upgrade')) {
          lines << 'echo \"[MOCK] Helm upgrade integration server\"'
          lines << "echo helm upgrade -i ${rel} <chart-path-or-name> -n namespace \\"
          lines << "  --set image.repository=${pkg.imagePath.split(':')[0]} \\"
          lines << "  --set image.tag=${(pkg.imagePath.contains(':') ? pkg.imagePath.split(':')[-1] : pkg.imageVersion)} \\"
          (pkg.configFilePath ? lines << "  --set-file app.config=${pkg.configFilePath}" : null)
        } else {
          lines << 'echo \"[MOCK] Restart integration deployment\"'
          lines << "echo kubectl rollout restart deployment/${rel} -n namespace"
        }
      }

      echo 'Generated Integration Server mock commands:\n' + lines.join('\n')
  }
}
return this
