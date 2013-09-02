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