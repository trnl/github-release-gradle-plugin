package org.gradle.api.plugins.git

import org.gradle.api.Plugin
import org.gradle.api.Project

class GitPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        createExtension(project)
    }

    void createExtension(Project project) {
        project.extensions.create('git', GitPluginExtension.class, project).with {
            workTree = project.rootProject.projectDir
            remote = 'origin'
            (GitPluginExtension) delegate
        }
    }
}
