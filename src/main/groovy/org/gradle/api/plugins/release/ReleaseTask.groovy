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

package org.gradle.api.plugins.release

import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.util.ConfigureUtil

class ReleaseTask extends GradleBuild {

    private Object aVersion
    private Object aTag
    private Object aTagMessage
    private Object aCommitMessage

    UpdateSpec update
    NextSpec next

    ReleaseTask() {
        startParameter = project.gradle.startParameter.newInstance()
        startParameter.setRecompileScripts(false)
        startParameter.setRerunTasks(false)

        setDefaults()

        tasks = [
                'unSnapshotVersion',
                'commitReleaseVersion',
                'tagReleaseVersion',
                'updateVersion',
                'commitNewVersion',
                'pushToRemote'
        ]

        project.task(
                'unSnapshotVersion',
                group: 'release',
                description: 'Updates version to release variant.'
        ) << this.&unSnapshotVersion

        project.task(
                'commitReleaseVersion',
                group: 'release',
                description: 'Commits the release version update.'
        ) << this.&commitReleaseVersion

        project.task(
                'tagReleaseVersion',
                group: 'release',
                description: 'Tags the release version update.'
        ) << this.&tagReleaseVersion

        project.task(
                'updateVersion',
                group: 'release',
                description: 'Updates version to next, using x.x.x+1 pattern.'
        ) << this.&updateVersion

        project.task(
                'commitNewVersion',
                group: 'release',
                description: 'Commits the version update.'
        ) << this.&commitNewVersion

        project.task(
                'pushToRemote',
                group: 'release',
                description: 'Pushes changes to remote repository.'
        ) << this.&pushToRemote
    }

    private void setDefaults() {
        group = 'release'
        description = 'Verify project, release, and update version to next.'
        update = new UpdateSpec();
        next = new NextSpec();
        version = project.version - '-SNAPSHOT'
        tag = "r$version"
        tagMessage = "Release $version"
        commitMessage = "Release $version"
    }


    def unSnapshotVersion() {
        def oldVersion = project.version
        def newVersion = getVersion()
        update.files.each { project.ant.replaceregexp(file: it, match: oldVersion, replace: newVersion) }
        update.projects*.version = newVersion
    }

    def commitReleaseVersion() {
        update.files.each {
            project.git.add(it)
        }
        project.git.commit(getCommitMessage())
    }

    def tagReleaseVersion() {
        project.git.tag(getTag(), getTagMessage())
    }

    def updateVersion() {
        def oldVersion = project.version
        def newVersion = next.version
        update.files.each { project.ant.replaceregexp(file: it, match: oldVersion, replace: newVersion) }
        update.projects*.version = newVersion
    }

    def commitNewVersion() {
        update.files.each {
            project.git.add(it)
        }
        project.git.commit(next.commitMessage)
    }

    def pushToRemote() {
        project.git.push()
    }

    void update(Closure closure) {
        ConfigureUtil.configure(closure, this.update)
    }

    void next(Closure closure) {
        ConfigureUtil.configure(closure, this.next)
    }

    void dependsOn(List<Task> t) {
        tasks = tasks.plus(tasks.size() - 3, t.collect { it.name })
    }

    void dependsOn(Task t) {
        tasks = tasks.plus(tasks.size() - 3, t.collect { it.name })
    }

    def getVersion() {
        aVersion = call(aVersion)
        return aVersion
    }

    def getTag() {
        aTag = call(aTag)
        return aTag
    }

    def getTagMessage() {
        aTagMessage = call(aTagMessage)
        return aTagMessage
    }

    def getCommitMessage() {
        aCommitMessage = call(aCommitMessage)
        return aCommitMessage
    }

    void setVersion(final def version) {
        this.aVersion = version
    }

    void setTag(final def tag) {
        this.aTag = tag
    }

    void setTagMessage(final def tagMessage) {
        this.aTagMessage = tagMessage
    }

    void setCommitMessage(final def commitMessage) {
        this.aCommitMessage = commitMessage
    }

    private static Object call(def c) {
        c instanceof Closure ? c.call() : c
    }
}
