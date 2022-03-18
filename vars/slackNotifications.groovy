#!/usr/bin/env groovy
// in readme have example
def call(String buildStatus, String channel, String baseUrl, String teamDomain, String tokenCredentialId) {
    // def notifyBuild(String buildStatus) {

    def subject = "${buildStatus} --> ${env.JOB_NAME} Build Number: ${env.BUILD_NUMBER}"
    def summary = "${subject} (${env.BUILD_URL})"

    // Send notifications
    colorCode = 'good'
    slackSend baseUrl: baseUrl ,channel: channel, color: buildStatus == 'SUCCESS' ? 'good' : 'danger', message: summary, teamDomain, tokenCredentialId: tokenCredentialId
}
