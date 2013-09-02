package org.gradle.api.plugins.github

import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy

class PagesTask extends DefaultTask {

    private static final String GROUP_PAGES = 'Github Pages'

    PagesTask() {
        setDefaults()

        project.task('cleanPages', group: GROUP_PAGES) << this.&cleanPages
        project.task('clonePages', group: GROUP_PAGES, dependsOn: 'cleanPages') << this.&clonePages
        project.task('processPages', group: GROUP_PAGES, type: Copy, dependsOn: 'clonePages') {
            with(project.github.pages)
            into(workingDir())
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        project.task('commitPages', group: GROUP_PAGES, dependsOn: 'processPages') << this.&commitPages
        project.task('pushPages', group: GROUP_PAGES, dependsOn: 'commitPages') << this.&pushPages
        this.dependsOn('pushPages')
    }

    private void setDefaults() {
        group = GROUP_PAGES
        description = 'Clone, process, commit and push Github Pages.'
    }

    def cleanPages() {
        workingDir().deleteDir()
        workingDir().mkdirs()
    }

    def clonePages() {
        project.git.clone(workingDir(), 1, 'gh-pages')
    }

    def commitPages() {
        project.git.add(workingDir(), '.')
        project.git.commit(workingDir(), 'Publish of github pages from Gradle')
    }

    def pushPages() {
        project.git.push(workingDir(), 'gh-pages')
    }

    private File workingDir() {
        new File(project.buildDir, 'gh-pages')
    }
}
