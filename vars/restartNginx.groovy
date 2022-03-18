def call(server, user = 'deploy') {
    sh "ssh ${user}@${server} sudo nginx -t"
    sh "ssh ${user}@${server} sudo systemctl reload nginx"
}
