package org.gradle.api.plugins.github

import org.gradle.api.Plugin
import org.gradle.api.Project

class GithubPlugin implements Plugin<Project> {
    static final String USERNAME_PROPERTY = 'github.credentials.username'
    static final String PASSWORD_PROPERTY = 'github.credentials.password'

    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.apply(plugin: 'git')
        addExtension()
    }

    void addExtension() {
        project.extensions.create('github', GithubPluginExtension, project).with {
            repo = project.git.remotes()[0]
            credentials = new CredentialsSpec(prop(USERNAME_PROPERTY) ?: '', prop(PASSWORD_PROPERTY) ?: '')
            return (GithubPluginExtension) delegate
        }
    }

    private String prop(final String p) {
        project.hasProperty(p) ? project.property(p) : null
    }
}
