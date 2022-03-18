#!groovy
@Library('jenkins-lib')_
def label = "slave-unittests"
def testError = null
def stage_name = ''
podTemplate(
        label: label,
        cloud: "kubernetes-test",
        yaml: """\
    apiVersion: v1
    kind: Pod
    metadata:
    spec:
      containers:
      - name: jnlp
        image: REPO/backend/base-images:jnlp-slave-4-7-1
        args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
        imagePullPolicy: Always
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
      - name: $label
        image: REPO/backend/base-images:devbox-slave
        command:
        - cat
        tty: true
        imagePullPolicy: Always
        volumeMounts:
        - mountPath: /var/run/docker.sock
          name: docker-sock
      volumes:
      - name: docker-sock
        hostPath:
          path: /var/run/docker.sock
    """.stripIndent()) {
        node(label) {
            stage('Checkout') {
                withCredentials([usernameColonPassword(credentialsId: 'credentialsId', variable: 'REPO'),
                    file(credentialsId: 'GIT_PUB_KEY', variable: 'GIT_PUB_KEY'),
                    file(credentialsId: 'GIT_CONFIG', variable: 'GIT_CONFIG'),
                    file(credentialsId: 'AUTHORIZED_KEYS', variable: 'AUTHORIZED_KEYS'),
                    file(credentialsId: 'ID_RSA', variable: 'ID_RSA'),
                    file(credentialsId: 'KNOWN_HOSTS', variable: 'KNOWN_HOSTS'),]) {
                        sh '''
                                cat $AUTHORIZED_KEYS > /root/.ssh/authorized_keys
                                cat $GIT_PUB_KEY > /root/.ssh/git_pub_key
                                cat $GIT_CONFIG > /root/.ssh/config
                                cat $ID_RSA > /root/.ssh/id_rsa
                                cat $KNOWN_HOSTS > /root/.ssh/known_hosts
                                chmod 400  /root/.ssh/id_rsa
                                ssh-keyscan git.ke-la.private >> /root/.ssh/known_hosts
                            ''' 
                        }
                    checkout(
                        [$class: 'GitSCM',
                            branches: [[name: '*/${Branch}']], 
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'CleanBeforeCheckout']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[
                                credentialsId: 'credentialsId',
                                url: 'REPO'
                            ]],
                            depth: 1
                        ]
                    )
            }
            container(label) {
                try {
                    stage('Dotnet Restore') {
                        stage_name = 'Dotnet Restore'
                        sh "export DOTNET_CLI_TELEMETRY_OPTOUT=true"
                        sh "dotnet restore ./src/project.sln -clp:ErrorsOnly -v m"
                    }
                    stage('Run Unit Tests') {
                        stage_name = 'Run Unit Tests'
                        try {
                            sh (script: "dotnet sln ./src/project.sln list | grep -P \\.Test | while read -r project; do echo \"Start Test \$project\"; dotnet test ./src/\$project --no-restore -clp:ErrorsOnly -v m --interactive --nologo --logger 'junit;LogFilePath=${env.WORKSPACE}/tests-results/{assembly}.xml' ; echo \"Done \$project\"; done;", returnStatus: true)
                        } catch(err) {
                            testError = err
                        }
                        sh "ls -ltr ./tests-results"
                        archiveArtifacts(artifacts: '**/tests-results/*.xml', allowEmptyArchive: true)
                        junit (allowEmptyResults: true, healthScaleFactor: 10.0, keepLongStdio: true, testResults: '**/tests-results/*.xml')
                        step([$class: 'JUnitResultArchiver', testResults: '**/tests-results/*.xml', allowEmptyResults: true, healthScaleFactor: 10.0])
                        if(testError != null) {
                            throw testError
                        }
                        if(currentBuild.result == 'UNSTABLE') {
                            error('not all unit tests passed')
                        }
                        slackNotificationsForTests('backend-tests', true, '', stage_name)
                        cleanWs()
                    }
                } catch (e) {
                    slackNotificationsForTests('backend-tests', false, '', stage_name)
                    throw e
                }
        }
    }
}













