#!/usr/bin/env groovy
def call() {
    if ("${TargetEnvironment}".contains("Prod") && ("${DeployToProd}" == "false")){
        error("[FAILURE] Delpoyment conditions are not met")
    }
}
