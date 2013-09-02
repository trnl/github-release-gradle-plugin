package org.gradle.api.plugins.release

import org.gradle.api.Project

class UpdateSpec {

    List<File> files = []
    List<Project> projects = []

    void file(File f) {
        this.files << f
    }

    void project(Project p) {
        this.projects << p
    }

    void projects(Collection<Project> p) {
        this.projects.addAll(p)
    }

}
