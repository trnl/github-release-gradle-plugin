package org.gradle.api.plugins.github

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

class DraftReleaseTask extends DefaultTask {
  public static final String GROUP_RELEASE = 'Github Release'

  DraftReleaseTask() {
    group = GROUP_RELEASE
  }

  @TaskAction
  void run() {
    if (!(project.git.remote ==~ 'git@github\\.com:(.+)\\/(.+)\\.git')) {
      throw new GradleException("Github repo should match 'git@github.com:{user}/{repo}.git' pattern")
    }

    def gitHub = GitHub.connect()
    def matcher = (project.git.remote =~ 'git@github\\.com:(.+)\\/(.+)\\.git')[0]
    GHRepository repository = gitHub.getRepository("${matcher[1]}/${matcher[2]}")
    def notes = project.release.githubRelease.releaseNotes
    if (notes == null) {
      notes = defaultNotes(repository)
    }
    project.github.with {
      def release = repository.createRelease(release.tag.toString())
              .name(release.name.toString())
              .body(notes.toString())
              .draft(true)
              .create()
      project.subprojects { subproject ->
        subproject.configurations.archives { archive ->
          project.github.with {
            def jar = archive.getAllArtifacts().find { artifact ->
              artifact.getFile().getName().endsWith("-${project.version}.jar")
            }

            println "Uploading ${jar.getFile()}"
            release.uploadAsset(jar.getFile(), "application/jar")
          }
        }
      }
    }
  }

def defaultNotes(repository) {
    def milestone = getMilestone(repository)
    def issues = getIssues(repository, milestone)
    println "issues = $issues"

    def javadoc = project.git.remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
      m -> "https://rawgithub.com/wiki/${m[1]}/${m[2]}/javadoc/${project.release.version}/index.html"
    })

    def notes = """
## Version ${project.release.version} (${new Date().format("MMM dd, yyyy")})

### Downloads
Below and on maven central.

### Docs
${javadoc}

"""
    issues.each { label ->
      notes = notes << "### ${label.key.name.toUpperCase()}\n"
      label.value.each { issue ->
        notes = notes << "* [Issue ${issue.number}](${issue.html_url}): ${issue.title}\n"
      }
      notes = notes << "\n"
    }

    notes
  }

  def getIssues(repository, milestone) {
    def issues = [:].withDefault {[]}
    def list = repository.listIssues(GHIssueState.CLOSED)
    list.each { issue ->
      if (issue.milestone.number == milestone.number) {
        issue.labels.each { label ->
          issues[label] << issue
        }
      }
    }

    issues
  }

  def getMilestone(repository) {
    repository.listMilestones(GHIssueState.OPEN).find { milestone ->
      milestone.title == project.release.version
    }
  }

}
