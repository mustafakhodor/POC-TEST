def call(Map cfg = [:]) {
  stage('Determine target environment URL') {
    def envUrlByName = [
    "dev"   : 'https://devmerge.netways1.com',
    "qa"    : 'https://qamerge.netways1.com',
    "stage1": 'https://stage1merge.netways1.com',
    "stage2": 'https://stage2merge.netways1.com',
    "prod"  : 'https://merge.netways1.com'
  ]

    def key = params.ENVIRONMENT.toLowerCase().replaceAll(/\s+/, '').trim()
    echo "Normalized key: '${key}'"

    def url = envUrlByName[key]

    if (!url) {
      error "No URL mapping found for ENVIRONMENT='${params.ENVIRONMENT}'. Update envUrlByName."
    }

    env.TARGET_ENV_URL = "${url}"
    echo "Resolved TARGET_ENV_URL (inside helper): ${env.TARGET_ENV_URL}"

    currentBuild.displayName = "#${env.BUILD_NUMBER} • ${params.ENVIRONMENT}"
  }

  stage('Create deployment manifest file') {
    def manifestJson = '''{
      "add": {
        "dynamics": [
          {
            "solution": "AssetManagement",
            "version": "1.0.0.9",
            "managedSolution": "/asset-module/v-99-77daf4db/crm/Asset_Management_managed.zip",
            "unmanagedSolution": "/asset-module/v-99-77daf4db/crm/Asset_Management_unmanaged.zip",
            "projects": [
              {
                "name": "Asset Management",
                "flop": "/asset-module/v-99-77daf4db/crm/Dispute/src/Dispute.flop",
                "localizedResourceDataMap": "/asset-module/v-99-77daf4db/crm/Dispute/src/localizedresources.datamap.xml",
                "entityDataMap": "/asset-module/v-99-77daf4db/crm/Dispute/src/dev-entity.datamap.xml",
                "dataSpecFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.spec.xml",
                "dataFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.xml"
              }
            ]
          }
        ],
        "api": {
          "internet": [
            { "name": "Asset Management Internet", "project": "Asset Management" }
          ],
          "intranet": [
            { "name": "Asset Management Intranet", "project": "Asset Management" }
          ]
        },
        "integration": [
          { "name": "MOE", "direction": "inbound" }
        ]
      },
      "update": {
        "dynamics": [
          {
            "solution": "LegalFiles",
            "version": "1.0.0.748",
            "managedSolution": "/legal-file-module/v-99-77daf4db/crm/LegalFiles_managed.zip",
            "unmanagedSolution": "/legal-file-module/v-99-77daf4db/crm/LegalFiles_unmanaged.zip",
            "projets": [
              {
                "name": "Dispute",
                "flop": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/Dispute.flop",
                "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/localizedresources.datamap.xml",
                "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/dev-entity.datamap.xml",
                "dataSpecFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.spec.xml",
                "dataFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.xml"
              },
              {
                "name": "Minors",
                "flop": "/legal-file-module/v-99-77daf4db/crm/Minors/src/Minors.flop",
                "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Minors/src/localizedresources.datamap.xml",
                "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Minors/src/dev-entity.datamap.xml"
              },
              {
                "name": "Bankruptcy",
                "flop": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/Bankruptcy.flop",
                "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/localizedresources.datamap.xml",
                "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/dev-entity.datamap.xml"
              },
              {
                "name": "Estate",
                "flop": "/legal-file-module/v-99-77daf4db/crm/Estate/src/Estate.flop",
                "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Estate/src/localizedresources.datamap.xml",
                "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Estate/src/dev-entity.datamap.xml",
                "dataSpecFile": "/legal-file-module/v-99-77daf4db/crm/Estate/src/data.spec.xml",
                "dataFile": "/legal-file-module/v-99-77daf4db/crm/Estate/src/data.xml"
              },
              {
                "name": "Judgment Post Action",
                "flop": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/Judgment_Post_Action.flop",
                "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/localizedresources.datamap.xml",
                "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/dev-entity.datamap.xml"
              },
              {
                "name": "Marriage",
                "flop": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/Marriage.flop",
                "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/entity.datamap.xml"
              }
            ]
          }
        ],
        "api": {
          "internet": [
            { "name": "Decisions Internet", "project": "Decisions", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Decisions/src/Decisions_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" },
            { "name": "Dispute Internet", "project": "Dispute", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/Dispute_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" },
            { "name": "Minors Internet", "project": "Minors", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Minors/src/Minors_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" },
            { "name": "Bankruptcy Internet", "project": "Bankruptcy", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/Bankruptcy_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" },
            { "name": "Estate Internet", "project": "Estate", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Estate/src/Estate_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" },
            { "name": "Judgment Post Action Internet", "project": "Judgment Post Action", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/Judgment_Post_Action_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" },
            { "name": "Marriage Internet", "project": "Marriage", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/Marriage_Internet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0" }
          ],
          "intranet": [
            { "name": "Decisions Intranet", "project": "Decisions", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Decisions/src/Decisions_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" },
            { "name": "Dispute Intranet", "project": "Dispute", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/Dispute_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" },
            { "name": "Minors Intranet", "project": "Minors", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Minors/src/Minors_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" },
            { "name": "Bankruptcy Intranet", "project": "Bankruptcy", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/Bankruptcy_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" },
            { "name": "Estate Intranet", "project": "Estate", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Estate/src/Estate_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" },
            { "name": "Judgment Post Action Intranet", "project": "Judgment Post Action", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/Judgment_Post_action_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" },
            { "name": "Marriage Intranet", "project": "Marriage", "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/Marriage_Intranet.json", "image": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0" }
          ]
        },
        "integration": [
          { "name": "ICP", "direction": "inbound" }
        ]
      },
      "remove": { }
    }'''

    writeFile file: 'deployment-manifest.json', text: manifestJson
    echo "Generated ${env.WORKSPACE}/deployment-manifest.json"
  }

  stage('Extract Flowon commands (solutions)') {
    def manifest = readJSON file: 'deployment-manifest.json'

    def buildCommandsFor = { dynamicsList ->
      (dynamicsList ?: []).collectMany { dyn ->
        def cmds = []

        def managed = dyn.managedSolution
        if (managed) {
          def parts = []
          parts << 'flowon-dynamics i'
          parts << "--connectionstring \"${env.TARGET_ENV_URL}\""
          parts << "-s \"${managed}\""
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
          p << "--connectionstring \"${env.TARGET_ENV_URL}\""
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
    // To execute them for real, uncomment:
    // commands.each { c -> sh "set -e; echo Executing: ${c}; ${c}" }
    }
  }

  // TODO stages (left as placeholders exactly like your original)
  stage('Extract Client Extension command from manifest') { echo 'TODO' }
  stage('Extract APIs command from manifest')            { echo 'TODO' }
  stage('Extract gateways command from manifest')        { echo 'TODO' }
  stage('Extract Integration Server command from manifest') { echo 'TODO' }

  // Return anything you want to the caller
  return [env: params.ENVIRONMENT, url: env.TARGET_ENV_URL]
}
return this
