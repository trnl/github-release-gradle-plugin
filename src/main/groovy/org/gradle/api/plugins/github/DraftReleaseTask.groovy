package org.gradle.api.plugins.github

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class DraftReleaseTask extends DefaultTask {
    public static final String GROUP_RELEASE = 'Github Release'

    DraftReleaseTask() {
        group = GROUP_RELEASE
    }

    @TaskAction
    void run() {
        project.github.with {

            if (!(project.git.remote ==~ 'git@github\\.com:(.+)\\/(.+)\\.git')) {
                throw new GradleException("Github repo should match 'git@github.com:{user}/{repo}.git' pattern")
            }

            def path = project.git.remote.replaceAll(
                    'git@github\\.com:(.+)\\/(.+)\\.git',
                    { m -> "/repos/${m[1]}/${m[2]}/releases" }
            )
            def http = new HTTPBuilder('https://api.github.com')

            http.request(Method.POST) {
                uri.path = path
                requestContentType = ContentType.JSON
                headers['User-Agent'] = 'Mozilla/5.0 Ubuntu/8.10 Firefox/3.0.4'
                headers['Authorization'] = 'Basic ' + "$credentials.username:$credentials.password"
                        .getBytes('iso-8859-1')
                        .encodeBase64()
                body = [
                        tag_name: release.tag.toString(),
                        name: release.name.toString(),
                        body: '...Release Notes...',
                        draft: true
                ]
            }
        }
    }

    private String prop(String name) {
        project.hasProperty(name) ? project.property(name) as String : null
    }

}
