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
