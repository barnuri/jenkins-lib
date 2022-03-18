@Library('jenkins-lib')_
pipeline {
    agent any
    options { ansiColor('xterm') }
    stages {
        stage('test') {
            steps {
                sh 'echo 1 => ./test.txt'
                zip dir: '', glob: 'test.txt', zipFile: 'test1.zip'
                downloadFile('./test-down.zip',publishZipArtifactToNexus('./test1.zip'))
                sh 'ls -al'
            }
        }
  }
  post {
        // success { slackSendHelper(true) }
        // failure { slackSendHelper(false) }
        always { 
            chuckNorris()
            cleanWs()
            // printEnv()
        }
    }
}

