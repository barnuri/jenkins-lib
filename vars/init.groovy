#!/usr/bin/env groovy
def call() {
    textWithColor("Init")
    if(isEmpty(ENV)) {
        textWithColor('ENV param is must')
        throw new Exception('ENV param is must')
    }

    env.BRANCH_TO_CLONE = params.TAG_OR_BRANCH ?: GIT_BRANCH ?: "integration"
    if(isEmpty(env.BRANCH_TO_CLONE)) {
        textWithColor('env.BRANCH_TO_CLONE param is must')
        throw new Exception('env.BRANCH_TO_CLONE param is must')
    }

    env.GIT_REPO = GIT_URL ?: gitlabSourceRepoURL ?: gitlabSourceRepoSshUrl
    if(isEmpty(env.GIT_REPO)) {
        textWithColor('env.GIT_REPO param is must')
        throw new Exception('env.GIT_REPO param is must')
    }
    
    textWithColor("Config ${ENV}")
    CONFIG = readJSON(text: CONFIG)
    CONFIG = CONFIG["${ENV}"]
    writeJSON file: 'myConfig.json', json: CONFIG
    sh "cat myConfig.json"
    
    env.CONTAINER_RUN_ARGS = CONFIG.CONTAINER_RUN_ARGS ?: ""
    env.SERVERS = CONFIG.SERVERS ?: ""
    env.DOCKER_REPO = CONFIG.DOCKER_REPO ?: ""
    env.K8S_APPLY_FILES_GLOB = CONFIG.K8S_APPLY_FILES_GLOB ?: ""
    env.KUBE_CONFIG_ID = CONFIG.KUBE_CONFIG_ID ?: ""
    
    try {
        wrap([$class: 'BuildUser']) {
            env.BUILDER_NAME = (BUILD_USER == '' || BUILD_USER == null || BUILD_USER == 'SCMTrigger') ? gitlabUserName : BUILD_USER
        }
    } catch(Exception ex) {
        env.BUILDER_NAME = 'unknown'
    }

    
    env.CONTAINER_NAME = "${env.CONTAINER_NAME}-${ENV}".replace("_", "-")
    env.CONTAINER_VERSION = "${(params.TAG_OR_BRANCH == null) ? BUILD_NUMBER : env.BRANCH_TO_CLONE}"
    env.CONTAINER_NAME_REPO = "${CONFIG.DOCKER_REPO}/${env.CONTAINER_NAME}:${env.CONTAINER_VERSION}"
}
