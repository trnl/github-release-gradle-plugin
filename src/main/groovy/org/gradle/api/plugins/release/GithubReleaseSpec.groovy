package org.gradle.api.plugins.release

class GithubReleaseSpec {
    private def releaseNotes = { }
    
    def getReleaseNotes() {
        releaseNotes = call(releaseNotes)
        return releaseNotes
    }

    void setReleaseNotes(final def releaseNotes) {
        this.releaseNotes = releaseNotes
    }
    
    private static Object call(def c) {
        c instanceof Closure ? c.call() : c
    }
    
}
