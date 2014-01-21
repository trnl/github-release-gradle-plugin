package org.gradle.api.plugins.github

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy

class WikiTask extends DefaultTask {
  private static final String GROUP_WIKI = 'Github Wiki'
  final String wikiUri;
  final File workingDir
  Git wiki;

  WikiTask() {
    setDefaults()
    workingDir = new File(project.buildDir, 'wiki')
    wikiUri = Git.open(new File('.')).repository.config.getString('remote', 'origin', 'url')
        .replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', { m ->
      "git@github.com:${m[1]}/${m[2]}.wiki.git"
    })

    project.task('cleanWiki', group: GROUP_WIKI) << this.&cleanWiki
    project.task('cloneWiki', group: GROUP_WIKI, dependsOn: 'cleanWiki') << this.&cloneWiki
    project.task('processWiki', group: GROUP_WIKI, type: Copy, dependsOn: 'cloneWiki') {
      with(project.github.wiki)
      into(workingDir)
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    project.task('updateIndex', group: GROUP_WIKI, dependsOn: 'processWiki') << this.&updateIndex
    project.task('commitWiki', group: GROUP_WIKI, dependsOn: 'updateIndex') << this.&commitWiki
    project.task('pushWiki', group: GROUP_WIKI, dependsOn: 'commitWiki') << this.&pushWiki
    this.dependsOn('pushWiki')
  }

  private void setDefaults() {
    group = GROUP_WIKI
    description = 'Clone, process, commit and push wiki to remote.'
  }

  def cleanWiki() {
    workingDir.deleteDir()
    workingDir.mkdirs()
  }

  def cloneWiki() {
    wiki = Git.cloneRepository()
        .setURI(wikiUri)
        .setDirectory(workingDir)
        .call()
  }

  def updateIndex() {
    def versions = []
    def git = Git.open(new File("."))
    def remote = git.repository.config.getString('remote', 'origin', 'url')
    def index = remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', { m ->
      "https://rawgithub.com/wiki/${m[1]}/${m[2]}/javadoc/${project.version}/index.html"
    })
    def markdown = "## [Current](${index})\n"
    new File(workingDir, 'javadoc').eachDir {
      d ->
        versions << d
    }
    versions.reverse().each {
      index = remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
        m -> "https://rawgithub.com/wiki/${m[1]}/${m[2]}/javadoc/${it.name}/index.html"
      })
      markdown = markdown + " * [${it.name}](${index})\n"
    }
    new File(workingDir, "Javadoc.md").write(markdown);
  }

  def commitWiki() {
    wiki.add()
        .addFilepattern('.')
        .call()
    wiki.commit()
        .setMessage('publish wiki from Gradle')
        .call()
  }

  def pushWiki() {
    wiki.push()
        .call();
  }
}
