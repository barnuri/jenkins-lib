@Library('jenkins-lib')_
env.ENV = "integration"
env.SLACK_CH = 'PROJECT'
env.CONTAINER_NAME = "PROJECT-client"
env.CONFIG = """
{
   "integration": {
      "DOCKER_REPO": "URL",
      "KUBE_CONFIG_ID": "kubeconfig",
      "K8S_APPLY_FILES_GLOB": "integration-client.yaml"
   }
}
"""
pipeline {
    agent any
    options { ansiColor('xterm') }
    stages {
        stage('init cicd') {
            steps {
                script {
                    init()
                    env.BRANCH_TO_CLONE = "master"
                    BRANCH_TO_CLONE = "master"
                }
            }
        }
        stage('Git Clone cicd') {
            steps {
                script {
                    gitClone(false)  
                    env.k8sfile = bashCommand("cat integration-client.yaml")
                    env.dockerfileClient = bashCommand("cat Dockerfile-client")
                }
            }
        }
        stage('init github') {
            steps {
                script {
                    env.GIT_REPO = "https://github.com/REPO"
                    GIT_REPO = "https://github.com/REPO"
                    env.BRANCH_TO_CLONE = "integration"
                    BRANCH_TO_CLONE = "integration"
                }
            }
        }
        stage('Git Clone github') {
            steps {
                gitClone(false, 'CREDS')   
                writeFile(file: './integration-client.yaml', text: env.k8sfile) 
                writeFile(file: './Dockerfile-client', text: env.dockerfileClient) 
            }
        }
        stage('Docker Image Tag & Push') {
            steps {
                script {
                    buildContainer([ASPNETCORE_ENVIRONMENT: ENV, NODE_ENV: ENV], './Dockerfile-client', './client')
                }
            }
        }
        stage('Deploying Image From Repo') {
            steps {
                deploy()
            }
        }
   }
   post {
        always { 
            notifyHelper(false)
            chuckNorris()
            cleanWs()
        }
    }
}
 
 
