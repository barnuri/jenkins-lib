@Library('jenkins-lib')_
env.SLACK_TOKEN = ''
env.SLACK_CH = ''
env.CONTAINER_NAME = ""
env.CONFIG = """
{
   "integration": {
      "DOCKER_REPO": "",
      "K8S_APPLY_FILES_GLOB": "k8s/${ENV}.yaml",
      "KUBE_CONFIG_ID": "kubeconfig",
   },
   "qa": {
      "DOCKER_REPO": "",
      "K8S_APPLY_FILES_GLOB": "k8s/${ENV}.yaml",
      "KUBE_CONFIG_ID": "kubeconfig",
   },
   "stage": {
      "DOCKER_REPO": "",
      "SERVERS": [],
      "CONTAINER_RUN_ARGS": "",
   },
   "production": {
      "DOCKER_REPO": "",
      "SERVERS": [],
      "CONTAINER_RUN_ARGS": "",
   }
}
"""
pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    stages {
        stage('init') {
            steps {
                init()
            }
        }
        stage('Git Clone') {
            steps {
                gitClone()
            }
        }
        stage('Docker Image Tag & Push') {
            steps {
                buildContainer([ASPNETCORE_ENVIRONMENT: ENV, NODE_ENV: ENV])
            }
        }
        stage('Deploying Image From Repo') {
            steps {
                deploy()
            }
        }
   }
   post {
        success { slackSendHelper(true) }
        failure { slackSendHelper(false) }
        always { 
            chuckNorris()
            cleanWs()
            // printEnv()
        }
    }
}
