package org.gradle.api.plugins.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild

class ReleasePlugin implements Plugin<Project> {

    private Project project

    @Override
    void apply(final Project project) {
        this.project = project
        project.apply(plugin: 'git')
        project.task( 'release', type: ReleaseTask)
    }
}

