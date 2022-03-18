def call(src, dest, server, user = 'deploy') {
    sh "ssh ${user}@${server} rm -rf ${dest}"
    sh "ssh ${user}@${server} mkdir -m 777 -p ${dest}"
    sh "scp ${src} ${user}@${server}:${dest}"
}
