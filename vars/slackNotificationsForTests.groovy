#!/usr/bin/env groovy

def call(url ,SLACK_CH, success , extraString = '', stage = '') {
    try {
        getGitDetails()
        if((stage == '' || stage == null) && env.STAGE_NAME != null && env.STAGE_NAME != '') {
            stage = env.STAGE_NAME
        }
        if(stage != null && stage != '') {
            stage = "[${stage}]"
        }
        if(!success) {
            stage = ""
        }
        def title = " Job '${JOB_NAME}' ${stage}"
        if(extraString != null && extraString.length() > 0) {
            title += " - ${extraString}"
        }
        def text = (success ? "*SUCCESSFUL*"  :  "*FAILED*") + " - ${env.LastCommit} \n ${env.LastCommitWithoutMerges}".replace("\n","\\n")
        text = text.trim().replace("\n","\\n").trim()
        def body = """
        {
            "username": "Jenkins",
            "icon_url": "${success ? "https://i.imgur.com/T0O4r13.png" : "https://i.imgur.com/f2V8vlc.png"}",
            "channel": "${SLACK_CH}",
            "attachments": [
                {
                    "color": "${success ? '#00FF00' : '#FF0000'}",
                    "text": "${text}",
                    "footer": "takes ${currentBuild.durationString}",
                    "title": "${title}",
                    "title_link": "${BUILD_URL}"
                }
            ]
        }
        """
        response = httpRequest ( 
            consoleLogResponseBody: true,
            httpMode: 'POST',
            requestBody: body,
            url: url,
            customHeaders: [ [name: 'Content-Type', value: 'application/json; charset=utf-8'] ]
        )
    } catch(Exception ex) {    }
}
