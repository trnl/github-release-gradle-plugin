package org.gradle.api.plugins.github

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy

class WikiTask extends DefaultTask {

    private static final String GROUP_WIKI = 'Github Wiki'

    final String remote;
    final File workingDir;

    WikiTask() {
        setDefaults()
        workingDir = new File(project.buildDir, 'wiki')
        if (!(project.git.remote ==~ 'git@github\\.com:(.+)\\/(.+)\\.git')) {
            throw new GradleException("Github repo should match 'git@github.com:{user}/{repo}.git' pattern")
        }

        remote = project.git.remote.replaceAll(
                'git@github\\.com:(.+)\\/(.+)\\.git',
                { m -> "git@github.com:${m[1]}/${m[2]}.wiki.git" }
        )


        project.task('cleanWiki', group: GROUP_WIKI) << this.&cleanWiki
        project.task('cloneWiki', group: GROUP_WIKI, dependsOn: 'cleanWiki') << this.&cloneWiki
        project.task('processWiki', group: GROUP_WIKI, type: Copy, dependsOn: 'cloneWiki') {
            with(project.github.wiki)
            into(workingDir)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        } 
        project.task('updateIndex', group: GROUP_WIKI, dependsOn: 'processWiki')<< this.&updateIndex
        project.task('commitWiki', group: GROUP_WIKI, dependsOn: 'updateIndex') << this.&commitWiki
        project.task('pushWiki', group: GROUP_WIKI, dependsOn: 'commitWiki') << this.&pushWiki
        this.dependsOn('pushWiki')
    }

    private void setDefaults() {
        group = GROUP_WIKI
        description = 'Clone, process, commit and push wiki to remote.'
    }

    def cleanWiki() {
        workingDir.deleteDir()
        workingDir.mkdirs()
    }

    def cloneWiki() {
        project.git.clone(workingDir, remote, 1)
    }

    def updateIndex() {
        def versions = []
        def remote = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
            m -> "https://rawgithub.com/wiki/${m[1]}/${m[2]}/javadoc/${project.version}/index.html"
        })
        def markdown = "## [Current](${remote})\n"
        new File(workingDir, 'javadoc').eachDir {
            d ->
                versions << d
        }
        versions.reverse().each {
            remote = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
                m -> "https://rawgithub.com/wiki/${m[1]}/${m[2]}/javadoc/${it.name}/index.html" 
            })
            markdown = markdown + " * [${it.name}](${remote})\n"
        }
        new File(workingDir, "Javadoc.md").write(markdown);
    }

    def commitWiki() {
        project.git.add(workingDir, '.')
        project.git.commit(workingDir, 'publish wiki from Gradle')
    }

    def pushWiki() {
        project.git.push(workingDir, remote, project.git.currentBranch(workingDir))
    }
}
