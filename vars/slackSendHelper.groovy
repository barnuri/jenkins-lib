#!/usr/bin/env groovy
def call(success, useHttpRequest = true) {
    try {
        if(isEmpty(env.SLACK_TOKEN)) {
            env.SLACK_TOKEN = ''
        }
        if(isEmpty(env.BRANCH_TO_CLONE)) {
            env.BRANCH_TO_CLONE = 'unknown'
        }
        if(isEmpty(env.BUILDER_NAME)) {
            env.BUILDER_NAME = 'unknown'
        }
        if(isEmpty(env.SLACK_CH)) {
            textWithColor("env.SLACK_CH is missing")
            return
        }
        def title = " Job '${JOB_NAME} *[${env.BRANCH_TO_CLONE}]'* By *${env.BUILDER_NAME}*"
        def title_link = BUILD_URL;
        def message = success ? "*SUCCESSFUL*"  :  "*FAILED*"
        def color =  success ? '#00FF00' : '#FF0000'
        if(useHttpRequest) {
            def body = """
            {
                "channel": "${env.SLACK_CH}",
                "attachments": [
                    {
                        "color": "${color}",
                        "text": "${message}",
                        "footer": "${currentBuild.durationString}",
                        "title": "${title}",
                        "title_link": "${title_link}"
                    }
                ]
            }
            """
            response = httpRequest (consoleLogResponseBody: true,
            contentType: 'APPLICATION_JSON',
            httpMode: 'POST',
            requestBody: body,
            url: env.SLACK_URL,
            validResponseCodes: '200')
            return
        }
        message = message + title
        message = message + "*- _${currentBuild.durationString}_*"
        message = message + title_link
        slackSend(token: env.SLACK_TOKEN, channel: env.SLACK_CH, color: color, message: message)
    } catch(Exception ex) {}
}
