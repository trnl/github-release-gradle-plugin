# github-release-gradle-plugin

A set of plugins to release, publish wiki, github pages and create draft github release from Gradle.

Consists of the following plugins:
- **console**: makes it possible to asks for user input
- **git**: provides several git operations for your project (commit, push, clone, tag)
- **github**: let you to publish gh-pages, wiki, gh release from gradle
- **release**: adds release routine to your build (unsnapshotVersion, commit, tag, updateVersion, push)

## Adding plugins

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'me.trnl:github-release-gradle-plugin:0.1'
    }
}

apply plugin: 'git'
apply plugin: 'console'
apply plugin: 'github'
apply plguin: 'release'
```

Please note that if you are applying 'github' or 'release', there is no need of adding 'git'.
It will be done automatically.

### Console

With this plugin your can ask for user input in your build.

```groovy
apply plugin: 'console'

version = { console.prompt(' > Please enter release version:', project.version - '-SNAPSHOT') }
dryRun = { console.promptYesOrNo(' > Should send data to remote server?') }
```

### Git

Adds an ability to perform git operations from Gradle.

```groovy
apply plugin: 'git'

/* list remotes */
project.git.remotes()

/* git status */
project.git.status()

/* git tag -a {tagname} -m {message} */
project.git.tag(String tagname, String message)

/* git clone {url} --depth {depth} --branch {branch} {destinationDir} */
project.git.clone(File destinationDir, String url, Integer depth, String branch)

/* git push -q --tags {url} {branch} | in {workDir} */
project.git.push(File workDir, String url, String branch)

/* git commit -m {message} */
project.git.commit(String message)
```

### Github

Let you create draft release, publish wiki and github pages.

```groovy
apply plugin: 'github'

github {
    /* Wiki repo will be calculated from this. */
    repo = 'git@github.com:trnl/mongo-java-driver.git'

    wiki {
        /*
        This is the same as used in Gradle Copy Task.
        Please refer to Gradle documentation for information.
        */
        from(project(':bson').javadoc.outputs.files) {
            into 'bson/javadoc'
        }
        from(project(':driver').javadoc.outputs.files) {
            into 'driver/javadoc'
        }
        from(project(':driver-compat').javadoc.outputs.files) {
            into 'driver-compat/javadoc'
        }
        into { "javadoc/$project.release.version" }
    }

    release {
        /* Use {} brackets for lazy evaluation */
        tag = { "r$project.release.version" }
        name = { "$project.release.version" }
    }
}
```

Publish Wiki:
```groovy
gradle publishWiki
```

Publish Draft Release:
```groovy
gradle draftGhRelease
```

#### Credentials

For some tasks (draftGhRelease) you need to provide your github credentials. There two ways of doing this.

In build script:
```groovy
apply plugin: 'github'

github {
    repo = 'git@github.com:trnl/mongo-java-driver.git'
    credentials {
        username = { console.prompt ('Please enter Github username:') }
        password = { console.prompt ('Please enter Github password:') }
    }
}
```

In `~/.gradle/gradle.properties`:
```properties
github.credentials.username={username}
github.credentials.password={password}
```

### Release

Release plugin adds a typical release routine to your build.

Invoke:
```groovy
gradle release
```

Steps:
- Remove '-SNAPSHOT' postfix from project version and update all files;
- Commit release ('unsnapshotted') version;
- Tag current HEAD with 'r$version';
- Perform all required tasks for release (install, uploadArchives, publishWiki, draftGhRelease);
- Update project version to next (with -SNAPSHOT postfix)
- Commit new version;
- Push changes to remote repo.

```groovy
release {
    /* Let's check that everything is committed */
    doFirst {
        if (!project.git.status().isEmpty()) {
            throw new GradleException('You have uncommitted changes.')
        }
    }

    /* Ask user for a release version, suggesting some default variant. */
    version = { console.prompt(' > Please enter release version:', project.version - '-SNAPSHOT') }
    tag = { "r$project.release.version" }
    commitMessage = { "Release $project.release.version" }

    /* Files and projects that needs to be updated with a new release version. */
    update {
        file project('driver-compat').file('src/main/com/mongodb/Mongo.java')
        file project.file('build.gradle')
        projects allprojects
    }

    /* Spec for next version. As above, asking user input. */
    next {
        version = { console.prompt(' > Please enter next version:', bumpVersion(project.release.version)) }
        commitMessage = { "Bumping version to $project.release.next.version" }
    }

    /* Tasks that needs to be performed during release. Will be done after tag step. */
    dependsOn subprojects.findAll { it.name != 'util' }*.install
    dependsOn project('driver').uberJar
    dependsOn project('driver-compat').uberJar
    dependsOn publishWiki
    dependsOn draftGhRelease
}
```
