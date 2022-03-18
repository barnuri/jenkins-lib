#!/usr/bin/env groovy
def call(success, useHttpRequest = true) {
    teamsSendHelper(success)
    slackSendHelper(success, useHttpRequest)
}
