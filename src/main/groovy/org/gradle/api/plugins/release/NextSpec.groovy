package org.gradle.api.plugins.release

class NextSpec {

    private def version
    private def commitMessage

    def getVersion() {
        version = call(version)
        return version
    }

    def getCommitMessage() {
        commitMessage = call(commitMessage)
        return commitMessage
    }

    void setVersion(final def version) {
        this.version = version
    }

    void setCommitMessage(final def commitMessage) {
        this.commitMessage = commitMessage
    }

    private static Object call(def c) {
        c instanceof Closure ? c.call() : c
    }
}
