package org.gradle.api.plugins.github

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.util.ConfigureUtil

class GithubPluginExtension {
    CopySpec pages
    CopySpec wiki
    ReleaseSpec release

    Project project

    GithubPluginExtension(final Project project) {
        this.project = project
    }

    def pages(Closure c) {
        pages = project.copySpec {}
//        project.task('publishGhPages', type: PagesTask)
        ConfigureUtil.configure(c, pages)
    }

    def wiki(Closure c) {
        wiki = project.copySpec {}
        project.task('publishWiki', type: WikiTask)
        ConfigureUtil.configure(c, wiki)
    }

    def release(Closure c) {
        release = new ReleaseSpec()
        project.task('draftGhRelease', type: DraftReleaseTask)
        ConfigureUtil.configure(c, release)
    }
}