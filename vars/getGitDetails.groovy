#!/usr/bin/env groovy
def call() {
    env.LastCommitMessage = sh(returnStdout: true, script:"git log -1 --format=%s").replace("\n", "")
    env.LastCommitHash = sh(returnStdout: true, script:"git log -1 --format=%h").replace("\n", "")
    env.LastCommitUser = sh(returnStdout: true, script:"git log -1 --format=%cn").replace("\n", "")
    env.LastCommitDate = sh(returnStdout: true, script:"git log -1 --format=%ai").replace("\n", "")
    env.LastCommit = "${env.LastCommitHash} - ${env.LastCommitDate} - ${env.LastCommitUser} - ${env.LastCommitMessage}".replace("\n", "")

    env.LastCommitWithoutMergesMessage = sh(returnStdout: true, script:"git log -1 --format=%s --no-merges").replace("\n", "")
    env.LastCommitWithoutMergesHash = sh(returnStdout: true, script:"git log -1 --format=%h --no-merges").replace("\n", "")
    env.LastCommitWithoutMergesUser = sh(returnStdout: true, script:"git log -1 --format=%cn --no-merges").replace("\n", "")
    env.LastCommitWithoutMergesDate = sh(returnStdout: true, script:"git log -1 --format=%ai --no-merges").replace("\n", "")
    env.LastCommitWithoutMerges = "${env.LastCommitWithoutMergesHash} - ${env.LastCommitWithoutMergesDate} - ${env.LastCommitWithoutMergesUser} - ${env.LastCommitWithoutMergesMessage}".replace("\n", "")

    env.LastTenCommitsInfo = sh(returnStdout: true, script:"git log -10 --format=\"%h - %cr - %cn - %s\" --no-merges")

    if(env.LastCommitMessage.indexOf("Merge") < 0) {
        env.LastCommitWithoutMergesMessage = ""
        env.LastCommitWithoutMergesHash = ""
        env.LastCommitWithoutMergesUser = ""
        env.LastCommitWithoutMergesDate = ""
        env.LastCommitWithoutMerges = ""
    }
}
