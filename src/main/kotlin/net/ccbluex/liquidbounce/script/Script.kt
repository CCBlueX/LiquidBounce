/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.bindings.api.JsContextProvider
import net.ccbluex.liquidbounce.script.bindings.features.JsChoice
import net.ccbluex.liquidbounce.script.bindings.features.JsCommandBuilder
import net.ccbluex.liquidbounce.script.bindings.features.JsModule
import net.ccbluex.liquidbounce.utils.client.logger
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess
import java.io.File
import java.util.function.Function

class Script(val scriptFile: File) {

    private val context: Context = Context.newBuilder("js")
        .allowHostAccess(HostAccess.ALL) // Allow access to all Java classes
        .allowHostClassLookup { true }
        .currentWorkingDirectory(scriptFile.parentFile.toPath())
        .allowIO(IOAccess.ALL) // Allow access to all IO operations
        .allowCreateProcess(false) // Disable process creation
        .allowCreateThread(true) // Disable thread creation
        .allowNativeAccess(false) // Disable native access
        .allowExperimentalOptions(true) // Allow experimental options
        .option("js.nashorn-compat", "true") // Enable Nashorn compatibility
        .option("js.ecmascript-version", "2022") // Enable ECMAScript 2022
        .build().apply {
            // Global instances
            val jsBindings = getBindings("js")

            JsContextProvider.setupUsefulContext(jsBindings)

            // Global functions
            jsBindings.putMember("registerScript", RegisterScript())
        }

    private val scriptText: String = scriptFile.readText()

    // Script information
    lateinit var scriptName: String
    lateinit var scriptVersion: String
    lateinit var scriptAuthors: Array<String>

    /**
     * Whether the script is enabled
     */
    private var scriptEnabled = false

    private val globalEvents = mutableMapOf<String, () -> Unit>()

    /**
     * Tracks client modifications made by the script
     */
    private val registeredModules = mutableListOf<Module>()
    private val registeredCommands = mutableListOf<Command>()
    private val registeredChoices = mutableListOf<Choice>()

    /**
     * Initialization of scripts
     */
    fun initScript() {
        // Evaluate script
        val language = Source.findLanguage(scriptFile) ?: error("Unknown language")
        context.eval(Source.newBuilder(language, scriptText, scriptFile.name).build())

        // Call load event
        callGlobalEvent("load")

        if (!::scriptName.isInitialized || !::scriptVersion.isInitialized || !::scriptAuthors.isInitialized) {
            logger.error("[ScriptAPI] Script '${scriptFile.name}' is missing required information!")
            error("Script '${scriptFile.name}' is missing required information!")
        }

        logger.info("[ScriptAPI] Successfully loaded script '${scriptFile.name}'.")
    }

    @Suppress("UNCHECKED_CAST")
    inner class RegisterScript : Function<Map<String, Any>, Script> {

        /**
         * Global function 'registerScript' which is called to register a script.
         * @param scriptObject JavaScript object containing information about the script.
         * @return The instance of this script.
         */
        override fun apply(scriptObject: Map<String, Any>): Script {
            scriptName = scriptObject["name"] as String
            scriptVersion = scriptObject["version"] as String

            val authors = scriptObject["authors"]
            scriptAuthors = when (authors) {
                is String -> arrayOf(authors)
                is Array<*> -> authors as Array<String>
                is List<*> -> (authors as List<String>).toTypedArray()
                else -> error("Not valid authors type")
            }

            return this@Script
        }

    }

    /**
     * Registers a new script module
     *
     * @param moduleObject JavaScript object containing information about the module.
     * @param callback JavaScript function to which the corresponding instance of [JsModule] is passed.
     * @see JsModule
     */
    @Suppress("unused")
    fun registerModule(moduleObject: Map<String, Any>, callback: (Module) -> Unit) {
        val module = JsModule(this, moduleObject)
        registeredModules += module
        callback(module)
    }

    /**
     * Registers a new script command
     *
     * @param command From the command builder.
     */
    @Suppress("unused")
    fun registerCommand(commandObject: Value) {
        val commandBuilder = JsCommandBuilder(commandObject)
        registeredCommands += commandBuilder.build()
    }

    /**
     * Registers a new script choice to an existing choice configurable which can be obtained
     * from existing modules.
     *
     * @param choiceConfigurable The choice configurable to add the choice to.
     * @param choiceObject JavaScript object containing information about the choice.
     * @param callback JavaScript function to which the corresponding instance of [JsChoice] is passed.
     *
     * @see JsChoice
     * @see ChoiceConfigurable
     */
    @Suppress("unused")
    fun registerChoice(
        choiceConfigurable: ChoiceConfigurable<Choice>, choiceObject: Map<String, Any>,
        callback: (Choice) -> Unit
    ) {
        JsChoice(choiceObject, choiceConfigurable).apply {
            callback(this)
            registeredChoices += this
        }
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: () -> Unit) {
        globalEvents[eventName] = handler
    }

    /**
     * Called when the client enables the script.
     */
    fun enable() {
        if (scriptEnabled) {
            return
        }

        callGlobalEvent("enable")
        ModuleManager += registeredModules
        CommandManager += registeredCommands
        registeredChoices.forEach { choice ->
            @Suppress("UNCHECKED_CAST")
            (choice.parent.choices as MutableList<Any>).add(choice)
        }
        scriptEnabled = true
    }

    /**
     * Called when the client disables the script. Handles unregistering all modules and commands
     * created with this script.
     */
    fun disable() {
        if (!scriptEnabled) {
            return
        }

        callGlobalEvent("disable")
        ModuleManager -= registeredModules
        CommandManager -= registeredCommands
        registeredChoices.forEach { it.parent.choices.remove(it) }

        scriptEnabled = false
    }

    /**
     * Calls the handler of a registered event.
     * @param eventName Name of the event to be called.
     */
    private fun callGlobalEvent(eventName: String) {
        try {
            globalEvents[eventName]?.invoke()
        } catch (throwable: Throwable) {
            logger.error("${scriptFile.name}::$scriptName -> Event Function $eventName threw an error",
                throwable)
        }
    }
}
