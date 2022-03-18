def call() {
    textWithColor("Deploying ${ENV}")
    if(!isEmpty(env.K8S_APPLY_FILES_GLOB) && !isEmpty(env.KUBE_CONFIG_ID)) {
        deploy_k8s()
    } else {
        for(server in (readJSON(text: env.SERVERS))) {
            sshDeploy(server)
        }
    }
    textWithColor("Finished Deploying ${ENV}")
}
