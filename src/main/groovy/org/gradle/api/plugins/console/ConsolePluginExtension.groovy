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

package org.gradle.api.plugins.console

class ConsolePluginExtension {

    boolean promptYesOrNo(String message, boolean defaultValue = false) {
        def defaultStr = defaultValue ? 'Y' : 'n'
        String consoleVal = prompt("${message} (Y|n)", defaultStr)
        if (consoleVal) {
            return consoleVal.toLowerCase().startsWith('y')
        }
        defaultValue
    }

    /**
     * Reads user input from the console.
     *
     * @param message Message to display
     * @param defaultValue (optional) default value to display
     * @return User input entered or default value if user enters no data
     */
    String prompt(String message, String defaultValue = null) {
        String _message = "$message" + (defaultValue ? " [$defaultValue] " : "")
        def console = System.console()
        if (console) {
            return console.readLine(_message) ?: defaultValue
        } else {
            println "$_message (WAITING FOR INPUT BELOW)"
            return System.in.newReader().readLine() ?: defaultValue
        }
    }
}
