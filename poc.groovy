def call(Map config = [:]) {
  def githubUrl     = require(config, 'githubUrl')
  def gitRef        = require(config, 'gitRef')
  def filePath      = require(config, 'filePath')
  def command       = require(config, 'command')

  def githubCredsId = (config.githubCredsId ?: '').trim()
  def agentLabel    = (config.agentLabel ?: '').toString()

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

        sh """#!/bin/bash -e
          test -s '${filePath}' || { echo 'File not found or empty: ${filePath}'; exit 1; }
        """
      }
    }

    stage('Run command & parse JSON') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def fullPath = "${workDir}/${filePath}"

      String rawOut = sh(
        script: """
          set -eu
          ${command} '${fullPath}'
        """.stripIndent(),
        returnStdout: true
      ).trim()

      def parsed
      try {
        parsed = readJSON text: rawOut
      } catch (e) {
        error "Failed to parse JSON from command output.\nError: ${e}\nOutput was:\n${rawOut}"
      }

      writeJSON file: "${workDir}/command-result.json", json: parsed, pretty: 2
    }

    stage('Read deployment.manifest.json & Build Commands') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def manifestPath = "${workDir}/${filePath}"
      if (!fileExists(manifestPath)) {
        error "Manifest file not found: ${manifestPath}"
      }

      def manifest = readJSON file: manifestPath

      def buildCommands = { dynamicsList ->
        (dynamicsList ?: []).collectMany { dyn ->
          def managedSolutionPath = dyn.managedSolution
          if (!managedSolutionPath) {
            echo "WARN: dynamics entry missing 'managedSolution' -> skipping"
            return []
          }
          def projects = (dyn.projects ?: dyn.projets ?: [])
          projects.collect { proj ->
            def flopPath  = proj.flop
            if (!flopPath) {
              echo "WARN: project '${proj.name ?: '?'}' missing 'flop' -> skipping"
              return null
            }
            def dataFile  = proj.dataFile ?: ''
            def entityMap = proj.entityDataMap ?: ''
            def localizedDataMap = proj.localizedResourceDataMap ?: ''

            def parts = []
            parts << 'flowon-dynamics i'
            parts << "--connectionstring \"${env.FLOWON_CONN ?: ''}\""
            parts << "-p \"${flopPath}\""
            parts << "-s \"${managedSolutionPath}\""
            parts << '-oc true'
            if (dataFile)  parts << "-d \"${dataFile}\""
            if (entityMap) parts << "-m \"${entityMap}\""
            if (localizedDataMap) parts << "-m \"${localizedDataMap}\""

            parts.join(' ')
          }.findAll { it }
          }
        }

      def commands = []
      if (manifest.add?.dynamics)    { commands += buildCommands(manifest.add.dynamics) }
      if (manifest.update?.dynamics) { commands += buildCommands(manifest.update.dynamics) }

      if (commands.isEmpty()) {
        echo 'No dynamics entries found to build commands.'
        return
      }

      echo "Generated commands:\n${commands.join('\n')}"

      // Execute them (remove this loop if you only want to print)
      // commands.each { cmd ->
      //   sh """
      //     set -e
      //     echo "Executing: ${cmd}"
      //     ${cmd}
      //   """
      // }
      }

    
    
    
    stage('Build API Deployment Commands') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def manifestPath = "${workDir}/${filePath}"
      if (!fileExists(manifestPath)) {
        error "Manifest file not found: ${manifestPath}"
      }

      def manifest = readJSON file: manifestPath

      // Namespaces (override via env if you want different ones per channel)
      def nsDefault   = (env.KUBE_NAMESPACE ?: 'default')
      def nsInternet  = (env.KUBE_NS_INTERNET ?: nsDefault)
      def nsIntranet  = (env.KUBE_NS_INTRANET ?: nsDefault)

      // Optional prefix for deployment names, e.g., "just" -> "just-decisions"
      def depPrefix   = (env.DEPLOYMENT_PREFIX ?: '').trim()

      // Container name in the Deployment. Defaults to project.toLowerCase()
      def resolveContainerName = { String project ->
        (env.API_CONTAINER_NAME ?: project?.toLowerCase())
      }

      // Build 'kubectl set image' commands for a list of APIs under a given namespace
      def buildApiCommands = { List apiList, String namespace ->
        (apiList ?: []).collect { api ->
          def project = api.project
          def image   = api.image
          if (!project || !image) {
            echo "Skipping API entry with missing project or image -> ${api}"
            return null
          }

          def depBase   = project.toLowerCase()
          def depName   = depPrefix ? "${depPrefix}-${depBase}" : depBase
          def container = resolveContainerName(project)

          // Final command
          "kubectl -n ${namespace} set image deployment/${depName} ${container}=${image} --record"
          }.findAll { it }
        }

      def commands = []
      if (manifest?.add?.api?.internet) commands += buildApiCommands(manifest?.add?.api?.internet, nsInternet)
      if (manifest?.add?.api?.intranet) commands += buildApiCommands(manifest?.add?.api?.intranet, nsIntranet)
      if (manifest?.update?.api?.internet) commands += buildApiCommands(manifest?.update?.api?.internet, nsInternet)
      if (manifest?.update?.api?.intranet) commands += buildApiCommands(manifest?.update?.api?.intranet, nsIntranet)

      if (commands.isEmpty()) {
        echo 'No API deployments found in manifest'
        return
      }

      echo "Generated API deployment commands:\n${commands.join('\n')}"

      // To execute them, uncomment below. DRY_RUN=true will only print.
      if (!(env.DRY_RUN ?: 'false').toBoolean()) {
      // commands.each { cmd ->
      //   sh """
      //     set -e
      //     echo "Executing: ${cmd}"
      //     ${cmd}
      //   """
      // }
      }
    }
  
  // === Build & (optionally) run API Gateway OpenAPI import commands ===
stage('Build API Gateway Spec Commands') {
      def workDir  = "${env.WORKSPACE}/gh-src"
      def manifestPath = "${workDir}/${filePath}"
      if (!fileExists(manifestPath)) {
        error "Manifest file not found: ${manifestPath}"
      }

      def manifest = readJSON file: manifestPath

  // Channels -> where to send these specs (use env to map per channel)
  def channelInternet = (env.GW_CHANNEL_INTERNET ?: 'internet')
  def channelIntranet = (env.GW_CHANNEL_INTRANET ?: 'intranet')

  // Service name shaping (prefix/suffix optional)
  def svcPrefix = (env.API_SERVICE_PREFIX ?: '').trim()
  def svcSuffix = (env.API_SERVICE_SUFFIX ?: '').trim()

  // Command templates (override these with your real gateway CLI)
  // Available placeholders: ${SPEC}, ${SERVICE}, ${NAME}, ${PROJECT}, ${CHANNEL}
  def tplInternet = (env.GW_CMD_INTERNET ?: 'gatewayctl apis import --spec "${SPEC}" --service "${SERVICE}" --channel "${CHANNEL}" --publish')
  def tplIntranet = (env.GW_CMD_INTRANET ?: 'gatewayctl apis import --spec "${SPEC}" --service "${SERVICE}" --channel "${CHANNEL}" --publish')

  // Where specs live (prefix path if your manifest paths are relative)
  def specsRoot = (env.API_SPEC_ROOT ?: workDir)

  // Helpers
  def resolveServiceName = { String project ->
    def base = (project ?: '').toLowerCase().replaceAll('\\s+', '-')
    def parts = []
    if (svcPrefix) parts << svcPrefix
    parts << base
    if (svcSuffix) parts << svcSuffix
    parts.findAll { it }.join('-')
  }

  def resolveSpecPath = { String p ->
    if (!p) return null
    // If manifest path starts with '/', treat as repo-relative under gh-src
    if (p.startsWith('/')) return "${workDir}${p}"
    // Otherwise, join with specsRoot (defaults to gh-src)
    return "${specsRoot}/${p}"
  }

  def fill = { String tpl, Map vars ->
    tpl
      .replace('${SPEC}',    vars.SPEC    ?: '')
      .replace('${SERVICE}', vars.SERVICE ?: '')
      .replace('${NAME}',    vars.NAME    ?: '')
      .replace('${PROJECT}', vars.PROJECT ?: '')
      .replace('${CHANNEL}', vars.CHANNEL ?: '')
  }

  def buildForList = { List apiList, String channel, String tpl ->
    (apiList ?: []).collect { api ->
      def project = api?.project
      def name    = api?.name ?: project
      def specRel = api?.openApiSpecs
      def specAbs = resolveSpecPath(specRel)
      if (!project || !specAbs) {
        echo "Skipping API (missing project or openApiSpecs): ${groovy.json.JsonOutput.toJson(api)}"
        return null
      }
      // if (!fileExists(specAbs)) {
      //   error "OpenAPI spec not found: ${specAbs} (from ${specRel})"
      // }
      def service = resolveServiceName(project)
      fill(tpl, [SPEC: specAbs, SERVICE: service, NAME: name, PROJECT: project, CHANNEL: channel])
    }.findAll { it }
  }

  // Collect APIs from both add & update
  def addInternet    = (manifest?.add?.api?.internet    ?: [])
  def addIntranet    = (manifest?.add?.api?.intranet    ?: [])
  def updateInternet = (manifest?.update?.api?.internet ?: [])
  def updateIntranet = (manifest?.update?.api?.intranet ?: [])

  def commands = []
  commands += buildForList(addInternet   + updateInternet, channelInternet, tplInternet)
  commands += buildForList(addIntranet   + updateIntranet, channelIntranet, tplIntranet)

  if (commands.isEmpty()) {
    echo 'No API gateway imports to process (no openApiSpecs found).'
    return
  }

  echo "Gateway import commands:\n${commands.join('\n')}"

  // Execute unless DRY_RUN=true
  if (!(env.DRY_RUN ?: 'false').toBoolean()) {
    commands.each { cmd ->
      sh """
        set -e
        echo "Executing: ${cmd}"
        ${cmd}
      """
    }
  }
}


  }
    }

@NonCPS
private String require(Map m, String key) {
  def v = m[key]
  if (v == null || (v instanceof CharSequence && v.toString().trim().isEmpty())) {
    throw new IllegalArgumentException("Missing required parameter: '${key}'")
  }
  return v.toString()
}

return this
