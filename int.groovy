@Library('moj_shared_library@main') _  // Import the shared library

pipeline {
    agent { label 'linux-agent-70' }

    parameters {
        choice(
      name: 'ENV',
      choices: ['dev', 'qa', 'stage1', 'stage2', 'prod'],
      description: 'Target environment'
    )
        string(
      name: 'BRANCH',
      defaultValue: '*/dev',
      description: 'Git branch to build (e.g. */dev, */main)'
    )
        string(
      name: 'IMAGE_TAG',
      defaultValue: '',
      description: '(Non-dev) Image tag to deploy'
    )
    // gitParameter(...) // if you re-enable, remove the BRANCH string param above
    }

    environment {
        appname        = 'integration-server'
        // IMAGE_TAG    = '0.1' // removed to avoid conflict with params.IMAGE_TAG used later
        trivy_db_dir   = '/opt/trivy_db'
        workspaces     = '/var/lib/jenkins/trivy-scans'
        shortCommit    = (env.GIT_COMMIT ?: 'unknown').take(8)
        tag            = "v-${env.BUILD_NUMBER}-${(env.GIT_COMMIT ?: 'unknown').take(8)}"
    }

    stages {
        stage('Checkout') {
            when { expression { params.ENV == 'dev' } }
            steps {
                script {
                    checkout([
            $class           : 'GitSCM',
            branches         : [[ name: "${params.BRANCH}" ]],
            userRemoteConfigs: [[
              url          : 'https://gitlab.moj.gov.ae/moj/webmethods.git',
              credentialsId: 'gitlab_ci_token'
            ]]
          ])
                }
            }
        }

        stage('Build') {
            when { expression { params.ENV == 'dev' } }
            steps {
                script {
                    // Build Docker image
                    sh """
            docker build -t docker-registry.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${env.GIT_COMMIT} -f Helm/webmethods-integ/Dockerfile .
            docker build -t docker-registry2.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${env.GIT_COMMIT} -f Helm/webmethods-integ/Dockerfile .
          """
                }
            }
        }

    // stage('Trivy image scanning for integ-server') {
    //   steps {
    //     script {
    //       withCredentials([string(credentialsId: 'ado_sync_user_api_access', variable: 'glpat')]) {
    //         trivyImageScan([
    //           glpat         : "${glpat}",
    //           trivyDbDir    : "${trivy_db_dir}",
    //           image         : "${integserver_image}",
    //           trivyWorkspace: "${workspaces}",
    //           appName       : "${appname}",
    //           commitHash    : "${env.GIT_COMMIT}"
    //         ])
    //       }
    //     }
    //   }
    //   post {
    //     success {
    //       publishHTML([
    //         allowMissing           : false,
    //         alwaysLinkToLastBuild  : false,
    //         keepAll                : false,
    //         reportDir              : '/var/lib/jenkins/trivy-scans/',
    //         reportFiles            : 'integration-server-trivy-result-t.json',
    //         reportName             : 'Integ Trivy Scan Report',
    //         reportTitles           : 'Trivy Result',
    //         useWrapperFileDirectly : true
    //       ])
    //     }
    //   }
    // }

        stage('Push Image') {
            when { expression { params.ENV == 'dev' } }
            steps {
                sh """
          docker push docker-registry.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${env.GIT_COMMIT}

          echo "Tagging the Image ${appname}"
          docker tag docker-registry.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${env.GIT_COMMIT} docker-registry.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${tag}
          docker tag docker-registry2.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${env.GIT_COMMIT} docker-registry2.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${tag}

          echo "Pushing the Image to GITLAB Container Registry"
          docker push docker-registry.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${tag}
          # docker push docker-registry2.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${env.GIT_COMMIT}
          # docker push docker-registry2.moj.gov.ae/repository/moj-docker/webmethods/integ-server:${tag}
        """
            }
        }

        stage('Deploy') {
            steps {
                script {
                    dir('Helm/webmethods-integ') {
                        // Select kubeconfig credential per ENV
                        def kubeconfigCredential
                        if (['dev', 'qa', 'prod'].contains(params.ENV)) {
                            kubeconfigCredential = "kubeconfig-${params.ENV}"
            } else if (params.ENV == 'stage1' || params.ENV == 'stage2') {
                            kubeconfigCredential = 'kubeconfig-stage'
            } else {
                            error "Unknown ENV: ${params.ENV}"
                        }

                        // Namespace mapping
                        def namespace = (params.ENV == 'dev')                       ? 'webmethods-ns' :
                            (['qa', 'stage1', 'prod'].contains(params.ENV)) ? 'nginx-ingress' :
                                                                             'nginx-ingress-stg2'

                        withCredentials([file(credentialsId: kubeconfigCredential, variable: 'KUBECONFIG')]) {
                            echo "we are in ${params.ENV} environment"

                            if (params.ENV == 'dev') {
                                sh """
                  helm upgrade -i ms . \
                    --set image.tag=${tag} \
                    -n ${namespace} \
                    --set ingress.defaultHostname=${params.ENV}-msr.moj.gov.ae
                """
              } else if (params.ENV == 'stage1') {
                                sh """
                  helm upgrade -i ms . \
                    --set image.tag=${params.IMAGE_TAG} \
                    -n ${namespace} \
                    --set ingress.defaultHostname=stg1-msr.moj.gov.ae
                """
              } else if (params.ENV == 'stage2') {
                                sh """
                  helm upgrade -i ms . \
                    --set image.tag=${params.IMAGE_TAG} \
                    -n ${namespace} \
                    --set ingress.defaultHostname=stg2-msr.moj.gov.ae
                """
              } else {
                                sh """
                  helm upgrade -i ms . \
                    --set image.tag=${params.IMAGE_TAG} \
                    -n ${namespace} \
                    --set ingress.defaultHostname=${params.ENV}-msr.moj.gov.ae
                """
                            }
                        }
                    }
                }
            }
        }

        stage('Publish Integration Release Metadata') {
            steps {
                script {
                    String metaPath = 'release.metadata.json'
                    if (!fileExists(metaPath)) {
                        def hits = findFiles(glob: '**/release.metadata.json')
                        if (hits) {
                            metaPath = hits[0].path
        } else {
                            error 'release.metadata.json not found'
                        }
                    }
                    def original = readJSON file: metaPath

                    def branchName   = params.BRANCH ?: 'main'
                    def branchRef    = "refs/heads/${branchName}"
                    def buildTag = "v-${env.BUILD_NUMBER}-${shortSha}"
                    def commitSha    = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()

                    def meta = [
                        name                : original.name,
                        type                : original.type,
                        version             : original.version,
                        imagePage           : original.imagePage,
                        openApiSpecsFilePath: original.openApiSpecsFilePath,
                        direction           : original.direction,
                        apiScope            : original.apiScope,
                        provenance          : [
                            sourceBranch   : branchRef,
                            sourceCommitId : commitSha,
                            buildNumber    : buildTag,
                            releaseNotes   : original.releaseNotes
                        ]
                    ]

                    // --- Write to file with new name ---
                    def pkg = meta.name.toString().toLowerCase()
                    sh 'mkdir -p build'
                    String outName = "build/${pkg}.integration.json"
                    writeJSON file: outName, json: meta, pretty: 4

                    // --- Upload to Nexus ---
                    withCredentials([usernamePassword(
        credentialsId: "${env.nexusCredentialsId}",
        usernameVariable: 'NEXUS_USER',
        passwordVariable: 'NEXUS_PASS'
      )]) {
                        def urls = [env.nexusUrl1, env.nexusUrl2].findAll { it?.trim() }
                        if (urls.isEmpty()) error 'No Nexus URLs configured (nexusUrl1 / nexusUrl2)'

                        def uploads = urls.collectEntries { u ->
                            ["Upload to ${u}" : {
            sh """
              curl --fail --retry 3 --retry-delay 5 -u "${NEXUS_USER}:${NEXUS_PASS}" \\
                --upload-file "${outName}" \\
                "${u}/repository/metadata/release/${params.BRANCH}/${pkg}.integration.json"
            """
          }]
                        }
                        parallel uploads
      }
                }
            }
        }


        stage('Package & Upload Bundle') {
  steps {
    script {
      // --- Locate release.metadata.json (and read for package name) ---
      String metaPath = 'release.metadata.json'
      if (!fileExists(metaPath)) {
        def hits = findFiles(glob: '**/release.metadata.json')
        if (hits && hits[0]?.path) {
          metaPath = hits[0].path
        } else {
          error "release.metadata.json not found in workspace"
        }
      }
      def original = readJSON file: metaPath
      if (!original?.name) error "Field 'name' is required in ${metaPath}"

      String pkg = original.name.toString().toLowerCase()

      // --- Locate swagger.yaml (required) ---
      String swaggerPath = 'swagger.yaml'
      if (!fileExists(swaggerPath)) {
        def sw = findFiles(glob: '**/swagger.yaml')
        if (sw && sw[0]?.path) {
          swaggerPath = sw[0].path
        } else {
          error "swagger.yaml not found (required)"
        }
      }

      // --- Locate src directory (required) ---
      String srcDir = 'src'
      if (!fileExists(srcDir)) {
        // find the first directory named 'src'
        def srcCandidates = findFiles(glob: '**/')
          .findAll { it.name == 'src/' } // Jenkins returns dirs ending with '/'
        if (srcCandidates && srcCandidates[0]?.path) {
          srcDir = srcCandidates[0].path
        } else {
          error "src directory not found (required)"
        }
      }

      // --- Prepare bundle folder (build/<pkg>/...) ---
      String buildDir   = 'build'
      String bundleDir  = "${buildDir}/${pkg}"
      String srcZipPath = "${bundleDir}/src.zip"
      sh """
        rm -rf "${bundleDir}"
        mkdir -p "${bundleDir}"
      """

      // Zip the src directory into bundleDir/src.zip
      // (zip -r preserves tree inside 'src', change to 'cd' if you want flat)
      sh """
        cd "$(dirname "${srcDir}")"
        zip -r "${pwd()}/${srcZipPath}" "$(basename "${srcDir}")" >/dev/null
      """

      // Copy release.metadata.json and swagger.yaml to the bundle
      sh """
        cp "${metaPath}" "${bundleDir}/release.metadata.json"
        cp "${swaggerPath}" "${bundleDir}/swagger.yaml"
      """

      // Create a single tarball for upload
      String tarball = "${buildDir}/${pkg}-bundle.tgz"
      sh """
        cd "${buildDir}"
        tar -czf "${pkg}-bundle.tgz" "${pkg}"
      """
      echo "Bundle created: ${tarball}"

      // --- Upload tarball to Nexus (parallel to both URLs) ---
      if (!env.nexusCredentialsId)   error "env.nexusCredentialsId is not set"
      if (!env.nexusUrl1 && !env.nexusUrl2) error "No Nexus URLs configured (nexusUrl1 / nexusUrl2)"

      withCredentials([usernamePassword(
        credentialsId: "${env.nexusCredentialsId}",
        usernameVariable: 'NEXUS_USER',
        passwordVariable: 'NEXUS_PASS'
      )]) {
        def urls = [env.nexusUrl1, env.nexusUrl2].findAll { it?.trim() }
        if (urls.isEmpty()) error "No Nexus URLs configured (nexusUrl1 / nexusUrl2)"

        // Path: .../metadata/release/<branch>/<pkg>/<pkg>-bundle.tgz
        def uploads = urls.collectEntries { u ->
          ["Upload bundle to ${u}" : {
            sh """
              curl --fail --retry 3 --retry-delay 5 -u "${NEXUS_USER}:${NEXUS_PASS}" \\
                --upload-file "${tarball}" \\
                "${u}/repository/metadata/release/${params.BRANCH}/${pkg}/${pkg}-bundle.tgz"
            """
          }]
        }
        parallel uploads
      }
    }
  }
}

  } // stages
} // pipeline
