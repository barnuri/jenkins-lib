def call(sshUser = 'deploy') {
    for(sshServer in (readJSON(text: env.SERVERS))) {
        textWithColor("Deploy Container To - ${sshUser}@${sshServer}")
        def scriptFile = "./script.sh"
        sh """
        echo '
            cd ./${CONTAINER_NAME}
            git reset --hard
            git fetch --all
            git checkout ${env.BRANCH_TO_CLONE} -f
            git pull -f
            git reset --hard
            git rm -r --cached .

            docker build -t ${env.CONTAINER_NAME} --build-arg NODE_ENV=${ENV} .
            docker stop ${env.CONTAINER_NAME} || true
            docker rm ${env.CONTAINER_NAME} || true
            echo "\033[44m --------------------------------------- \033[0m"
            echo "\033[44m docker run command \033[0m"
            echo "\033[44m --------------------------------------- \033[0m"
            ${CONTAINER_RUN_ARGS} --name ${env.CONTAINER_NAME} ${env.CONTAINER_NAME} || exit 1
            docker ps -a ' > ${scriptFile}
        """
        sh "ssh ${sshUser}@${sshServer} bash -s < ${scriptFile}"
        textWithColor("Finished Deploy Container")
    }
}
