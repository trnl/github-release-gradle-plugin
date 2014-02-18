package org.gradle.api.plugins.release

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.github.DraftReleaseTask
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil

class ReleaseTask extends DefaultTask {
    public static final String GROUP_RELEASE = 'Github Release'
    def String tagPrefix = "r"
    def String releaseVersion
    def String nextVersion
    def boolean tagRelease = false
    def boolean createGitHubRelease = false
    def Git git
    UpdateSpec update = new UpdateSpec()
    GithubReleaseSpec githubRelease = new GithubReleaseSpec()
    DraftReleaseTask ghReleaseTask
    String remote

    ReleaseTask() {
        group = GROUP_RELEASE
        description = 'Verify project, release, and update version to next.'
        ghReleaseTask = project.task('draftGhRelease', type: DraftReleaseTask)

        dependsOn('clean')
        
        ghReleaseTask.releaseTask = this

    }

    @TaskAction
    void run() {
        git = Git.open(new File('.'))
        remote = git.repository.config.getString('remote', 'origin', 'url')

        project.task('unSnapshotVersion', group: 'release',
                description: 'Updates version to release variant.') << this.&unSnapshotVersion

        project.task('commitReleaseVersion', group: 'release',
                description: 'Commits the release version update.') << this.&commitReleaseVersion

        project.task('tagReleaseVersion', group: 'release',
                description: 'Tags the release version update.') << this.&tagReleaseVersion


        def update = project.task('updateVersion', group: 'release',
                description: 'Updates version to next, using x.x.x+1 pattern.') << this.&updateVersion
        update.dependsOn('uploadArchives')
        project.task('commitNewVersion', group: 'release',
                description: 'Commits the version update.') << this.&commitNewVersion

        def pushToRemote = project.task('pushToRemote', group: 'release',
                description: 'Pushes changes to remote repository.') << this.&pushToRemote
        
        pushToRemote.dependsOn(ghReleaseTask)

    }


    def unSnapshotVersion() {
        update.files.each { project.ant.replaceregexp(file: it, match: project.version, replace: nextVersion) }
        update.projects*.version = nextVersion
    }

    def commitReleaseVersion() {
        update.files.each {
            git.add()
                    .addFilepattern(it.path)
                    .call()
        }
        git.commit()
                .setMessage("Release $releaseVersion")
                .call()
    }

    def tagReleaseVersion() {
        if (tagRelease) {
            git.tag()
                    .setName(tagName())
                    .setMessage("Release ${releaseVersion}")
                    .call()
        }
    }

    def tagName() {
        tagPrefix + releaseVersion
    }

    def updateVersion() {
        update.files.each { project.ant.replaceregexp(file: it, match: project.version, replace: nextVersion) }
        update.projects*.version = nextVersion
    }

    def commitNewVersion() {
        update.files.each {
            git.add()
                    .addFilepattern(it.path)
                    .call()
        }
        git.commit()
                .setMessage("Bumping version to ${nextVersion}")
                .call()
    }

    def pushToRemote() {
//        git.push()
//                .call()
    }

    void githubRelease(Closure closure) {
        ConfigureUtil.configure(closure, this.githubRelease)
    }

    void tagPrefix(value) {
        tagPrefix = call(value)
    }

    void version(final def block) {
        println "********** org.gradle.api.plugins.release.ReleaseTask.version"
        println "********** block = ${call(block)}"
        this.releaseVersion = call(block) - '-SNAPSHOT'
        println "********** releaseVersion = ${releaseVersion}"
        nextVersion = bumpVersion(releaseVersion)
        println "********** nextVersion = ${nextVersion}"
    }

    void createRelease(boolean create) {
        createGitHubRelease = create;
    }

    def bumpVersion(String old) {
        if (old != "unspecified") {
            String[] split = old.split('\\.')
            println "********** split = ${split}"
            def next = (split.last() as int) + 1

            def updated = split[0..-2].join('.')
            updated += ".${next}-SNAPSHOT"
            updated
        }

    }

    void update(Closure closure) {
        ConfigureUtil.configure(closure, this.update)
    }

//    void dependsOn(List<Task> t) {
//        tasks = tasks.plus(tasks.size() - 3, t.collect { it.name })
//    }
//
//    void dependsOn(Task t) {
//        tasks = tasks.plus(tasks.size() - 3, t.collect { it.name })
//    }


    private static Object call(def c) {
        c instanceof Closure ? c.call() : c
    }
}
