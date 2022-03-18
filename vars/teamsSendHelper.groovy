#!/usr/bin/env groovy
def call(success) {
    try {
        if(isEmpty(env.BRANCH_TO_CLONE)) {
            env.BRANCH_TO_CLONE = 'unknown'
        }
        if(isEmpty(env.BUILDER_NAME)) {
            env.BUILDER_NAME = 'unknown'
        }
        if(isEmpty(env.TEAMS_WEBHOOK)) {
            textWithColor("env.TEAMS_WEBHOOK is missing")
            return
        }
        def title = " Job - ${JOB_NAME} *[${env.BRANCH_TO_CLONE}] By -${env.BUILDER_NAME}"
        def title_link = BUILD_URL;
        def message = success ? "**SUCCESSFUL**"  :  "**FAILED**"
        def color =  success ? '00FF00' : 'FF0000'
        message = message + " - takes - ${currentBuild.durationString}"

        def body = """
        {
            "@context": "https://schema.org/extensions",
            "@type": "MessageCard",
            "themeColor": "${color}",
            "title": "${title}",
            "text": "${message}",
            "potentialAction": [
                    {
                    "@type": "OpenUri",
                    "name": "Build Url",
                    "targets": [
                        { "os": "default", "uri": "${title_link}" }
                    ]
                }
            ]
        }
        """
        response = httpRequest ( consoleLogResponseBody: true,
            contentType: 'APPLICATION_JSON',
            httpMode: 'POST',
            requestBody: body,
            url: env.TEAMS_WEBHOOK,
            validResponseCodes: '200'
        )

    } catch(Exception ex) {}
}
