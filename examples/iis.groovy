@Library('jenkins-lib')_
env.SLACK_TOKEN = 'TOKEN'
env.SLACK_CH = 'SLACK_CH'
env.MSBUILD_OUTPUT_FOLDER = '_PublishedWebsites/PROJECT'
env.isWindows = "true"
env.CONFIG = """
{
   "Integration": {
      "agentLabel": "integration",
      "IIS_FOLDER": "C:/Sites/PROJECT"
   },
   "Qa": {
      "agentLabel": "qa1",
      "IIS_FOLDER": "D:/PROJECT"
   }
}
"""
pipeline {
    agent none
    options { ansiColor('xterm') }
    triggers { pollSCM(scmpoll_spec: '* * * * *', ignorePostCommitHooks: true) }
    stages {
        stage('Git Clone') {
            when { expression { needToTrigger()} }
            agent { label 'jenkins-windows' }
            steps {
                script {
                    init(true,true)
                    printEnv()
                    gitClone()
                }
            }
        }
        stage('Build & Publish Artifact') {
            when { expression { needToTrigger()} }
            agent { label 'jenkins-windows' }
            steps {
                script {
                    powershell 'nuget restore'
                    buildFolderName = "build-${ENV}"
                    buildCmd = "msbuild /p:OutDir=\"../../${buildFolderName}\"  /p:Configuration=${ENV} /p:TransformConfigFile=true /p:SkipInvalidConfigurations=true /p:DeleteExistingFiles=True"
                    echo buildCmd
                    powershell buildCmd
 
                    fileName = "${buildFolderName}.zip"
                    bashCommand("rm -rf ./${fileName}")
                    zip dir: "./${buildFolderName}/${env.MSBUILD_OUTPUT_FOLDER}", zipFile: fileName
                    env.ArtifactUrl = publishZipArtifactToNexus("./${fileName}")
                    bashCommand("rm -rf ./${fileName}")
 
                    echo "env.ArtifactUrl = ${env.ArtifactUrl}"
                }
            }
        }
        stage('Download & Update IIS') {
            when { expression { needToTrigger()} }
            agent { label env.agentLabel }
            options { skipDefaultCheckout true }
            steps {
                IIS_DownloadZipToWebsite()
            }
        }
  }
  post {
        success { jiraWebhooks(true) }
        always { 
            notifyHelper()
            chuckNorris()
        }
    }
}
 
 
