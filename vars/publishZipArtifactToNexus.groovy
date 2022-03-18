#!/usr/bin/env groovy

def call(filePath, repo, user = 'jenkins:zxasqw12') {
    textWithColor("Publish Zip Artifact To Nexus")
    def fileName = JOB_NAME + '_' + BUILD_NUMBER + '.zip'
    def auth = 'Basic ' + user.bytes.encodeBase64().toString()
    httpRequest (consoleLogResponseBody: true,
            uploadFile: filePath,
            httpMode: 'PUT',
            url: repo + fileName,
            validResponseCodes: '201',
            customHeaders: [[maskValue: false, name: 'Authorization', value: auth]]
    )
    textWithColor("Done Publish Zip Artifact To Nexus")

    return repo + fileName
}
