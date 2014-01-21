package org.gradle.api.plugins.github

import org.gradle.api.Plugin
import org.gradle.api.Project

class GithubPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.apply(plugin: 'git')
    project.extensions.create('github', GithubPluginExtension, project).with {
      return (GithubPluginExtension) delegate
    }
  }
}