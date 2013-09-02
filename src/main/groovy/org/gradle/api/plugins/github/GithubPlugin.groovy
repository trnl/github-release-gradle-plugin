/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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