package org.gradle.api.plugins.github

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.release.ReleaseTask
import org.gradle.api.tasks.TaskAction
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

import static org.kohsuke.github.GHIssueState.CLOSED
import static org.kohsuke.github.GHIssueState.OPEN

class DraftReleaseTask extends DefaultTask {
    private String remote
    def ReleaseTask releaseTask

    DraftReleaseTask() {
        group = ReleaseTask.GROUP_RELEASE
    }

    def runBuild(closure) {
        build = closure
    }
    @TaskAction
    void run() {
        if (releaseTask.createGitHubRelease) {
            def git = Git.open(new File("."))
            remote = git.repository.config.getString('remote', 'origin', 'url')

            if (!(this.remote ==~ 'git@github\\.com:(.+)\\/(.+)\\.git')) {
                throw new GradleException("Github repo should match 'git@github.com:{user}/{repo}.git' pattern.  found ${this.remote}")
            }

            def gitHub = GitHub.connect()
            def matcher = (this.remote =~ 'git@github\\.com:(.+)\\/(.+)\\.git')[0]
            GHRepository repository = gitHub.getRepository("${matcher[1]}/${matcher[2]}")
            def notes = project.release.githubRelease.releaseNotes
            if (notes == null) {
                notes = defaultNotes(repository)
            }
            project.github.with {
                println "Creating release notes"
                def ghRelease = repository.createRelease(releaseTask.tagName())
                        .name(releaseTask.releaseVersion)
                        .body(notes.toString())
                        .draft(true)
                        .create()
                project.subprojects { subproject ->
                    subproject.configurations.archives { archive ->
                        gitHub.with {
                            def jar = archive.getAllArtifacts().find { artifact ->
                                artifact.getFile().getName().endsWith("-${project.version}.jar")
                            }

                            println "Uploading ${jar.getFile()}"
                            ghRelease.uploadAsset(jar.getFile(), "application/jar")
                        }
                    }
                }
            }
        }
    }

    def defaultNotes(repository) {
        def milestone = getMilestone(repository)
        def issues = getIssues(repository, milestone)

        def javadoc = remote.replaceAll('git@github\\.com:(.+)\\/(.+)\\.git', {
            m -> "https://rawgithub.com/wiki/${m[1]}/${m[2]}/javadoc/${releaseTask.releaseVersion}/index.html"
        })

        def notes = """
## Version ${releaseTask.releaseVersion} (${new Date().format("MMM dd, yyyy")})

### Downloads
Below and on maven central.

### Docs
${javadoc}

### Issues Resolved
"""
        issues.keySet().each { entry ->
            notes += "#### ${entry.toUpperCase()}\n"
            issues[entry].each { issue ->
                notes += "* [Issue ${issue.number}](${issue.html_url}): ${issue.title}\n"
            }
            notes += "\n"
        }

        notes
    }

    static def getIssues(repository, milestone) {
        def issues = [:].withDefault { [] }
        def list = repository.listIssues(CLOSED)
        list.each { issue ->
            if (issue.milestone?.number == milestone.number) {
                if (issue.labels) {
                    issue.labels.each { label ->
                        issues[label.name] << issue
                    }
                } else {
                    issues['Uncategorized'] << issue
                }
            }
        }

        issues
    }

    def getMilestone(repository) {
        def find = repository.listMilestones(OPEN).find { milestone ->
            milestone.title == releaseTask.releaseVersion
        }
        if (!find) {
            find = repository.listMilestones(CLOSED).find { milestone ->
                milestone.title == releaseTask.releaseVersion
            }
        }

        find
    }

}