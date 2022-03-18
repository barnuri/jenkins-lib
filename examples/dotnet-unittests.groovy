#!groovy
@Library('jenkins-lib')_

def K8s_Cluster = "test"
def REGISTRY = "REGISTAR"
def DEFAULT_UNITTEST_FILTER = "Category!=Slow"
def unitTestFilter = DEFAULT_UNITTEST_FILTER
try {
    unitTestFilter = params.UNIT_TEST_FILTER
} catch(err) {}
def DOCKER_FILE_NAME = "Dockerfile"
try {
    DOCKER_FILE_NAME = params.DOCKER_FILE_NAME
} catch(err) {}
DOCKER_FILE_NAME = DOCKER_FILE_NAME == null ? "Dockerfile" : DOCKER_FILE_NAME
unitTestFilter = unitTestFilter == null ? DEFAULT_UNITTEST_FILTER : unitTestFilter
def haveUnitTests = unitTestFilter.length() != 0
def label = "slave-builder"
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
        image: $REGISTRY/backend/base-images:jnlp-slave-4-7-1
        args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
      - name: $label
        image: $REGISTRY/backend/base-images:devbox-slave
        command:
        - cat
        tty: true
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
                withCredentials([usernameColonPassword(credentialsId: 'CRED', variable: 'GIT_USER_PASS'),
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
                                chmod 400  /root/.ssh/id_rsa
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
                                credentialsId: 'CRED',
                                url: 'REPO'
                            ]],
                            depth: 1
                        ]
                    )
            }
            container(label) {
                try {
                    if(haveUnitTests) {
                        stage ("Restore Nuget") {
                            sh "dotnet restore ./src/project.sln -clp:ErrorsOnly -v m"
                        }
                        stage ("UnitTests") {
                            def testError = null
                            try {
                                sh (script: "dotnet sln ./src/project.sln list | grep -P \\.Test | while read -r project; do echo \"Start Test \$project\"; dotnet test ./src/\$project --no-restore -clp:ErrorsOnly -v m --interactive --filter ${unitTestFilter} --nologo --logger 'junit;LogFilePath=${env.WORKSPACE}/tests-results/{assembly}.xml' ; echo \"Done \$project\"; done;", returnStatus: true)
                            } catch(err) {
                                testError = err
                            }
                            // cleanup unit tests chromium
                            try {
                                sh (script: "ps aux | grep \$(basename \"$PWD\") | grep .local-chromium | awk '{print \$2}' | xargs kill -9")
                            } catch(err) {
                                echo "failed to kill chromium"
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
                        }
                    }
                    stage("Build App"){
                        sh "dotnet publish ${SRC_PATH}/${APP_NAME}/${APP_NAME}.csproj -clp:ErrorsOnly -v m --configuration Release --output ${SRC_PATH}/${APP_NAME}/out"   
                        sh "ls -ltr ${SRC_PATH}/${APP_NAME}/out" 
                    }
                    stage("Build,Tag and Push Docker Image"){
                        withAWS(credentials: 'jenkins', region: 'us-east-1') {
                            def ENV_NAME = TargetEnvironment.toLowerCase()
                            ENV_NAME = ENV_NAME.split("-")[0]
                            ENV_NAME = ENV_NAME == "not applicable" ? "dev" : ENV_NAME
                            getGitDetails()
                            writeFile(file: "${SRC_PATH}/${APP_NAME}/out/gitInfo.txt", text: env.LastTenCommitsInfo.toString())
                            def buildName = "${REPO_NAME}:${BUILD_NUMBER}"
                            def extraScript = ""
                            // if prod also deploy to stage
                            if (TargetEnvironment.contains("Prod")) {
                                extraScript = """docker tag ${buildName} $REGISTRY/$REPO_NAME:stage && 
                                    docker push $REGISTRY/$REPO_NAME:stage &&"""
                            }
                            sh """
                                aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $REGISTRY && 
                                docker build --pull --network=host -t ${buildName} -f ${SRC_PATH}/${APP_NAME}/${DOCKER_FILE_NAME} . && 
                                docker tag ${buildName} $REGISTRY/$REPO_NAME:${env.BUILD_NUMBER} && 
                                docker push $REGISTRY/$REPO_NAME:${env.BUILD_NUMBER} &&
                                docker tag ${buildName} $REGISTRY/$REPO_NAME:$ENV_NAME && 
                                docker push $REGISTRY/$REPO_NAME:$ENV_NAME && ${extraScript}
                                docker rmi -f ${buildName} $REGISTRY/$REPO_NAME:$BUILD_NUMBER $REGISTRY/$REPO_NAME:$ENV_NAME
                            """
                        }
                    }
                    if ("${env.PerformDeploy}" != "false") {
                        stage('Trigger Deploy') {

                            if (TargetEnvironment.equals("Prod")) { 
                                K8s_Cluster = "prod"
                            } else if (TargetEnvironment.equals("Prod-indexing")) { 
                                K8s_Cluster = "indexing-prod"
                            } else if (TargetEnvironment.equals("Test-indexing")) { 
                                K8s_Cluster = "indexing-test"
                            } else {
                                K8s_Cluster = "test"
                                echo "${K8s_Cluster}"
                            }

                            build (
                                job: "../Deployments/${DEPLOY_JOB_NAME}", 
                                parameters: [
                                    string(name: 'BuildJobNumber', value: String.valueOf(BUILD_NUMBER)), 
                                    string(name: 'DeployToProd', value: String.valueOf(DeployToProd)), 
                                    string(name: 'TargetEnvironment', value: String.valueOf(TargetEnvironment)), 
                                    string(name: 'K8s_Cluster', value: String.valueOf(K8s_Cluster)),
                                    string(name: 'Branch', value: String.valueOf(Branch))
                                ]
                            )
                            // if prod also deploy to stage
                            if (TargetEnvironment.contains("Prod") && "${env.DeployStageWhenProd}" == "true") {
                                echo "deploy also to stage"
                                build (
                                    wait: false,
                                    job: "../Deployments/${DEPLOY_JOB_NAME}", 
                                    parameters: [
                                        string(name: 'BuildJobNumber', value: String.valueOf(BUILD_NUMBER)), 
                                        string(name: 'DeployToProd', value: String.valueOf(DeployToProd)), 
                                        string(name: 'TargetEnvironment', value: String.valueOf(TargetEnvironment.replace("Prod","Stage"))),
                                        string(name: 'K8s_Cluster', value: String.valueOf(K8s_Cluster.replace("prod","test"))),
                                        string(name: 'Branch', value: String.valueOf(Branch))
                                    ]
                                )
                            }
                            cleanWs()
                        }
                    }
                } catch (e) {
                    currentBuild.result = "FAILED"
                    throw e
                } finally {
                    def buildStatus = currentBuild.result
                    // build status of null means successful
                    buildStatus = buildStatus ?: 'SUCCESS'
                }
            }
        }
    }


