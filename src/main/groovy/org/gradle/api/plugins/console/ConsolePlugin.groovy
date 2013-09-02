package org.gradle.api.plugins.console

import org.gradle.api.Plugin
import org.gradle.api.Project

class ConsolePlugin implements Plugin<Project> {
    @Override
    void apply(final Project project) {
        project.extensions.create('console',ConsolePluginExtension.class)
    }
}
