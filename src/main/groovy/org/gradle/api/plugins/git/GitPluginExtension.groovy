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

package org.gradle.api.plugins.git

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GitPluginExtension {

    private final Project project

    String remote
    File workTree

    GitPluginExtension(final Project project) {
        this.project = project
    }

    Map<String, List<String>> status() {
        exec('status', '--porcelain').readLines().groupBy {
            it ==~ /^\s*\?{2}.*/ ? 'unversioned' : 'uncommitted'
        }
    }

    List<String> remotes() {
        exec('remote', '-v').readLines().collect { it.split('\\s+')[1] }
    }

    void tag(String tagname, String message) {
        exec('tag', '-a', tagname, '-m', message)
    }

    void clone(File destinationDir, Integer depth, String branch) {
        clone(destinationDir, remote, depth, branch)
    }

    void clone(File destinationDir, String url, Integer depth, String branch) {
        exec('clone', url,
                '--depth', depth.toString(),
                '--branch', branch,
                updateSeparators(destinationDir.absolutePath)
        )
    }

    void clone(File destinationDir, String url, Integer depth) {
        exec('clone', url,
                '--depth', depth.toString(),
                updateSeparators(destinationDir.absolutePath)
        )
    }

    void push(File workDir, String url, String branch) {
        exec(workDir, 'push', '-q', '--tags', url, branch)
    }

    void push(File workDir, String branch) {
        push(workDir, remote, branch)
    }

    void push(String branch) {
        push(workTree, remote, branch)
    }

    void push() {
        push(workTree, currentBranch(workTree))
    }

    String currentBranch(File workDir) {
        exec(workDir, 'rev-parse', '--abbrev-ref', 'HEAD').readLines().first()
    }

    void add(File file) {
        exec('add', updateSeparators(file.absolutePath))
    }

    void add(File workDir, String path) {
        exec(workDir, 'add', path)
    }

    void commit(String message) {
        exec('commit', '-m', message)
    }

    void commit(File workDir, String message) {
        exec(workDir, 'commit', '-m', message)
    }

    String exec(String... commands) {
        exec(workTree, commands)
    }

    String exec(File workDir, String... commands) {
        def cmdLine = ['git']
        cmdLine.addAll commands
        getLog().debug("git - 2 - {cmdLine: ${cmdLine}}")
        execute(true, ['HOME': System.getProperty("user.home")], workDir, cmdLine as String[])
    }

    private String execute(boolean failOnStderr = true, Map env = [:], File directory = null, String... commands) {

        def out = new StringBuffer()
        def err = new StringBuffer()
        def logMessage = "Running \"${commands.join(' ')}\"${ directory ? ' in [' + directory.canonicalPath + ']' : '' }"
        def process = (env || directory) ?
            (commands as List).execute(env.collect { "$it.key=$it.value" } as String[], directory) :
            (commands as List).execute()

        getLog().info(logMessage)

        process.waitForProcessOutput(out, err)

        getLog().info("$logMessage: [$out][$err]")

        if (err.toString()) {
            def message = "$logMessage produced an error: [${err.toString().trim()}]"
            if (failOnStderr) {
                throw new GradleException(message)
            } else {
                log.warn(message)
            }
        }
        out.toString()
    }

    private Logger getLog() { project?.logger ?: LoggerFactory.getLogger(this.class) }

    private static String updateSeparators(String path) {
        path.replaceAll('\\\\', '/')
    }
}
