package org.gradle.api.plugins.github

import org.gradle.api.artifacts.repositories.PasswordCredentials

class CredentialsSpec implements PasswordCredentials {
    def aUsername
    def aPassword

    CredentialsSpec(final String username, final String password) {
        this.aUsername = username
        this.aPassword = password
    }

    @Override
    String getUsername() {
        if (aUsername instanceof Closure) {
            aUsername = aUsername.call()
        }
        return aUsername
    }

    @Override
    void setUsername(final String username) {
        this.aUsername = username
    }

    @Override
    String getPassword() {
        if (aPassword instanceof Closure) {
            aPassword = aPassword.call()
        }
        return aPassword
    }

    @Override
    void setPassword(final String password) {
        this.aPassword = password
    }
}
