package org.gradle.api.plugins.release

import org.eclipse.jgit.api.Git
import org.gradle.StartParameter
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.initialization.GradleLauncherFactory
import org.gradle.util.ConfigureUtil

class ReleaseTask extends GradleBuild {
  private String aVersion
  private String aTag
  private String aTagMessage
  private String aCommitMessage
  Git git
  UpdateSpec update
  NextSpec next
  GithubReleaseSpec githubRelease
  String remote

  ReleaseTask(StartParameter currentBuild, GradleLauncherFactory gradleLauncherFactory) {
    super(currentBuild, gradleLauncherFactory)
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

    git = Git.open(new File('.'))
    remote = git.repository.config.getString('remote', 'origin', 'url')

    project.task('unSnapshotVersion', group: 'release',
        description: 'Updates version to release variant.') << this.&unSnapshotVersion

    project.task('commitReleaseVersion', group: 'release',
        description: 'Commits the release version update.') << this.&commitReleaseVersion

    project.task('tagReleaseVersion', group: 'release',
        description: 'Tags the release version update.') << this.&tagReleaseVersion

    project.task('updateVersion', group: 'release',
        description: 'Updates version to next, using x.x.x+1 pattern.') << this.&updateVersion

    project.task('commitNewVersion', group: 'release',
        description: 'Commits the version update.') << this.&commitNewVersion

    project.task('pushToRemote', group: 'release',
        description: 'Pushes changes to remote repository.') << this.&pushToRemote

//        githubRelease {
//            releaseNotes = { "Hey!  I'm releasing $project.release.version today!" }
//        }
  }

  private void setDefaults() {
    description = 'Verify project, release, and update version to next.'
    update = new UpdateSpec()
    next = new NextSpec()
    githubRelease = new GithubReleaseSpec()
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
      git.add()
          .addFilepattern(it.path)
          .call()
    }
    git.commit()
        .setMessage(getCommitMessage())
        .call()
  }

  def tagReleaseVersion() {
    git.tag()
        .setName(getTag())
        .setMessage(getTagMessage())
//        .setAnnotated(false)
        .call()
  }

  def updateVersion() {
    def oldVersion = project.version
    def newVersion = next.version
    update.files.each { project.ant.replaceregexp(file: it, match: oldVersion, replace: newVersion) }
    update.projects*.version = newVersion
  }

  def commitNewVersion() {
    update.files.each {
      git.add()
          .addFilepattern(it.path)
          .call()
    }
    git.commit()
        .setMessage(next.commitMessage)
        .call()
  }

  def pushToRemote() {
    git.push()
    .call()
  }

  void update(Closure closure) {
    ConfigureUtil.configure(closure, this.update)
  }

  void next(Closure closure) {
    ConfigureUtil.configure(closure, this.next)
  }

  void githubRelease(Closure closure) {
    ConfigureUtil.configure(closure, this.githubRelease)
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
    return aCommitMessage.toString()
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
