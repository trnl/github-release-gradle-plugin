package org.gradle.api.plugins.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GitPluginExtension {
  private final Project project
  String remote
  File workTree
  private Git git

  GitPluginExtension(final Project project) {
    this.project = project
    git = Git.open(new File("."));

  }

  void tag(String name, String message) {
    git.tag()
        .setName(name)
        .setMessage(message)
        .setAnnotated(false)
        .call()
  }

  void push(File workDir, String url, String branch) {
    git.push()
        .setPushTags()
        .setRefSpecs(new RefSpec("refs/heads/${branch}"))
        .setRemote(url)
  }

  void push(File workDir, String branch) {
    push(workDir, remote, branch)
  }

  void add(File file) {
      git.add()
        .addFilepattern(file.absolutePath)
        .call()
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
    def logMessage = "Running \"${commands.join(' ')}\"${directory ? ' in [' + directory.canonicalPath + ']' : ''}"
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
