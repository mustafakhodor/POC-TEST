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
          "name": "AssetManagement",
          "version": "1.0.0",
          "filePath": "/asset-module/v-99-77daf4db/liferay/client-extensions/asset-management-client-extension.zip",
          "provenance": {
            "sourceBranch": "main",
            "sourceCommitId": "77daf4db",
            "buildNumber": "99",
            "releaseNotes": ""
          }
        }
      ],
      "update": [
        {
          "name": "AssetManagement",
          "version": "1.0.0",
          "filePath": "/asset-module/v-99-77daf4db/liferay/client-extensions/asset-management-client-extension.zip",
          "provenance": {
            "sourceBranch": "main",
            "sourceCommitId": "77daf4db",
            "buildNumber": "99",
            "releaseNotes": ""
          }
        }
      ],
      "delete": [
        {
          "name": "AssetManagement",
          "version": "1.0.0"
        }
      ]
    },
    "contentPages": {
      "add": [
        {
          "name": "HomePage",
          "version": "1.0.0",
          "filePath": "/asset-module/v-99-77daf4db/liferay/content-pages/homepage.lar",
          "provenance": {
            "sourceBranch": "main",
            "sourceCommitId": "77daf4db",
            "buildNumber": "99",
            "releaseNotes": ""
          }
        }
      ],
      "update": [],
      "delete": []
    }
  },
  "dynamics": {
    "solutions": {
      "add": [
        {
          "solution": "AssetManagement",
          "version": "1.0.0.9",
          "managedSolution": "/asset-module/v-99-77daf4db/crm/Asset_Management_managed.zip",
          "unmanagedSolution": "/asset-module/v-99-77daf4db/crm/Asset_Management_unmanaged.zip",
          "provenance": {
            "sourceBranch": "main",
            "sourceCommitId": "77daf4db",
            "buildNumber": "99",
            "releaseNotes": ""
          },
          "projects": [
            {
              "name": "Asset Management",
              "flop": "/asset-module/v-99-77daf4db/crm/Dispute/src/Dispute.flop",
              "localizedResourceDataMap": "/asset-module/v-99-77daf4db/crm/Dispute/src/localizedresources.datamap.xml",
              "entityDataMap": "/asset-module/v-99-77daf4db/crm/Dispute/src/dev-entity.datamap.xml",
              "dataSpecFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.spec.xml",
              "dataFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Asset Management Internet",
                  "project": "Asset Management",
                  "openApiSpecs": "/asset-module/v-99-77daf4db/crm/Asset_Management/src/Asset_Management_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Asset Management Intranet",
                  "project": "Asset Management",
                  "openApiSpecs": "/asset-module/v-99-77daf4db/crm/Asset_Management/src/Asset_Management_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Restart",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            }
          ]
        }
      ],
      "update": [
        {
          "solution": "LegalFiles",
          "version": "1.0.0.748",
          "managedSolution": "/legal-file-module/v-99-77daf4db/crm/LegalFiles_managed.zip",
          "unmanagedSolution": "/legal-file-module/v-99-77daf4db/crm/LegalFiles_unmanaged.zip",
          "provenance": {
            "sourceBranch": "main",
            "sourceCommitId": "77daf4db",
            "buildNumber": "99",
            "releaseNotes": ""
          },
          "projets": [
            {
              "name": "Dispute",
              "flop": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/Dispute.flop",
              "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/localizedresources.datamap.xml",
              "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/dev-entity.datamap.xml",
              "dataSpecFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.spec.xml",
              "dataFile": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/data.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Dispute Internet",
                  "project": "Dispute",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/Dispute_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Dispute Intranet",
                  "project": "Dispute",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Dispute/src/Dispute_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            },
            {
              "name": "Minors",
              "flop": "/legal-file-module/v-99-77daf4db/crm/Minors/src/Minors.flop",
              "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Minors/src/localizedresources.datamap.xml",
              "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Minors/src/dev-entity.datamap.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Minors Internet",
                  "project": "Minors",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Minors/src/Minors_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Minors Intranet",
                  "project": "Minors",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Minors/src/Minors_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            },
            {
              "name": "Bankruptcy",
              "flop": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/Bankruptcy.flop",
              "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/localizedresources.datamap.xml",
              "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/dev-entity.datamap.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Bankruptcy Internet",
                  "project": "Bankruptcy",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/Bankruptcy_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Bankruptcy Intranet",
                  "project": "Bankruptcy",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Bankruptcy/src/Bankruptcy_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            },
            {
              "name": "Estate",
              "flop": "/legal-file-module/v-99-77daf4db/crm/Estate/src/Estate.flop",
              "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Estate/src/localizedresources.datamap.xml",
              "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Estate/src/dev-entity.datamap.xml",
              "dataSpecFile": "/legal-file-module/v-99-77daf4db/crm/Estate/src/data.spec.xml",
              "dataFile": "/legal-file-module/v-99-77daf4db/crm/Estate/src/data.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Estate Internet",
                  "project": "Estate",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Estate/src/Estate_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Estate Intranet",
                  "project": "Estate",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Estate/src/Estate_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            },
            {
              "name": "Judgment Post Action",
              "flop": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/Judgment_Post_Action.flop",
              "localizedResourceDataMap": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/localizedresources.datamap.xml",
              "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/dev-entity.datamap.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Judgment Post Action Internet",
                  "project": "Judgment Post Action",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/Judgment_Post_Action_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Judgment Post Action Intranet",
                  "project": "Judgment Post Action",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Judgment_Post_Action/src/Judgment_Post_action_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            },
            {
              "name": "Marriage",
              "flop": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/Marriage.flop",
              "entityDataMap": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/entity.datamap.xml",
              "provenance": {
                "sourceBranch": "main",
                "sourceCommitId": "77daf4db",
                "buildNumber": "99",
                "releaseNotes": ""
              },
              "api": [
                {
                  "name": "Marriage Internet",
                  "project": "Marriage",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/Marriage_Internet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-internet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                },
                {
                  "name": "Marriage Intranet",
                  "project": "Marriage",
                  "openApiSpecs": "/legal-file-module/v-99-77daf4db/crm/Marriage/src/Marriage_Intranet.json",
                  "image": {
                    "name": "adib-is-image",
                    "version": "1.0.0",
                    "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/api-intranet:1.0.0",
                    "action": "Upgrade",
                    "provenance": {
                      "sourceBranch": "main",
                      "sourceCommitId": "77daf4db",
                      "buildNumber": "99",
                      "releaseNotes": ""
                    }
                  }
                }
              ]
            }
          ]
        }
      ],
      "delete": []
    }
  },
  "integration": {
    "packages": {
      "add": [
        {
          "name": "ADIB",
          "version": "1.0.0",
          "image": {
            "name": "adib-is-image",
            "version": "1.0.0",
            "path": "docker-registry.moj.gov.ae/repository/moj-docker/just/adib-is-image:1.0.0",
            "action": "Upgrade",
            "provenance": {
              "sourceBranch": "main",
              "sourceCommitId": "77daf4db",
              "buildNumber": "99",
              "releaseNotes": ""
            }
          },
          "configFilePath": "/asset-module/v-99-77daf4db/integration/adib/GlobalConfig_dev.json"          
        }
      ],
      "update": [],
      "delete": []
    }
  }
}'''
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
