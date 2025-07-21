/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.bindings.api.ScriptContextProvider.setupContext
import net.ccbluex.liquidbounce.script.bindings.features.ScriptChoice
import net.ccbluex.liquidbounce.script.bindings.features.ScriptCommandBuilder
import net.ccbluex.liquidbounce.script.bindings.features.ScriptModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.underline
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.HoverEvent
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess
import java.io.File
import java.net.BindException
import java.net.ServerSocket
import java.util.function.Function

class PolyglotScript(
    val language: String, val file: File,
    val debugOptions: ScriptDebugOptions = ScriptDebugOptions()
) {

    private val context: Context = Context.newBuilder(language)
        .allowHostAccess(HostAccess.ALL) // Allow access to all Java classes
        .allowHostClassLookup { true }
        .currentWorkingDirectory(file.parentFile.toPath())
        .allowIO(IOAccess.ALL) // Allow access to all IO operations
        .allowCreateProcess(false) // Disable process creation
        .allowCreateThread(true) // Enable thread creation
        .allowNativeAccess(false) // Disable native access
        .allowExperimentalOptions(true) // Allow experimental options
        .option("js.nashorn-compat", "true") // Enable Nashorn compatibility
        .option("js.ecmascript-version", "2023") // Enable ECMAScript 2023
        .apply {
            if (debugOptions.enabled) {
                val protocolString = debugOptions.protocol.toString().lowercase()
                option("${protocolString}.Suspend", debugOptions.suspendOnStart.toString())
                option("${protocolString}.Internal", debugOptions.inspectInternals.toString())
                option(protocolString, "${debugOptions.port}")

                when (debugOptions.protocol) {
                    DebugProtocol.INSPECT -> {
                        option("inspect.Path", file.name)

                        val devtoolURL =
                            "devtools://devtools/bundled/js_app.html?ws=127.0.0.1:${debugOptions.port}/${file.name}"

                        chat(
                            regular(translation("liquidbounce.scripts.debug.support", variable(file.toString())))
                                .append(variable(devtoolURL)
                                    .copyable(copyContent = devtoolURL, hover = HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        regular(translation("liquidbounce.scripts.debug.inspect.url"))
                                    ))
                                    .underline(true)
                                )
                        )
                    }

                    DebugProtocol.DAP -> {
                        try {
                            // this happens when trying to build the options before the port is bound.
                            ServerSocket(debugOptions.port).close()
                        } catch (e: BindException) {
                            throw IllegalStateException("Debug port ${debugOptions.port} already in use", e)
                        }

                        chat(
                            regular(translation("liquidbounce.scripts.debug.support", variable(file.toString())).append(
                                translation("liquidbounce.scripts.debug.dap", variable(debugOptions.port.toString()))
                            )
                        ))
                    }
                }
            }
        }
        .build().apply {
            // Global instances
            val bindings = getBindings(language)

            this.setupContext(language, bindings)

            // Global functions
            bindings.putMember("registerScript", RegisterScript())
        }

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
    private val registeredModules = mutableListOf<ClientModule>()
    private val registeredCommands = mutableListOf<Command>()
    private val registeredChoices = mutableListOf<Choice>()

    /**
     * Initialization of scripts
     */
    fun initScript() {
        // Evaluate script
        context.eval(Source.newBuilder(language, file).build())

        // Call load event
        callGlobalEvent("load")

        if (!::scriptName.isInitialized || !::scriptVersion.isInitialized || !::scriptAuthors.isInitialized) {
            logger.error("[ScriptAPI] Script '${file.name}' is missing required information!")
            error("Script '${file.name}' is missing required information!")
        }

        logger.info("[ScriptAPI] Successfully loaded script '${file.name}'.")
    }

    @Suppress("UNCHECKED_CAST")
    inner class RegisterScript : Function<Map<String, Any>, PolyglotScript> {

        /**
         * Global function 'registerScript' which is called to register a script.
         * @param scriptObject JavaScript object containing information about the script.
         * @return The instance of this script.
         */
        override fun apply(scriptObject: Map<String, Any>): PolyglotScript {
            scriptName = scriptObject["name"] as String
            scriptVersion = scriptObject["version"] as String

            val authors = scriptObject["authors"]
            scriptAuthors = when (authors) {
                is String -> arrayOf(authors)
                is Array<*> -> authors as Array<String>
                is List<*> -> (authors as List<String>).toTypedArray()
                else -> error("Not valid authors type")
            }

            return this@PolyglotScript
        }

    }

    /**
     * Registers a new script module
     *
     * @param moduleObject JavaScript object containing information about the module.
     * @param callback JavaScript function to which the corresponding instance of [ScriptModule] is passed.
     * @see ScriptModule
     */
    @Suppress("unused")
    fun registerModule(moduleObject: Map<String, Any>, callback: (ClientModule) -> Unit) {
        val module = ScriptModule(this, moduleObject)
        registeredModules += module
        callback(module)
    }

    /**
     * Registers a new script command
     *
     * @param commandObject From the command builder.
     */
    @Suppress("unused")
    fun registerCommand(commandObject: Value) {
        val commandBuilder = ScriptCommandBuilder(commandObject)
        registeredCommands += commandBuilder.build()
    }

    /**
     * Registers a new script choice to an existing choice configurable which can be obtained
     * from existing modules.
     *
     * @param choiceConfigurable The choice configurable to add the choice to.
     * @param choiceObject JavaScript object containing information about the choice.
     * @param callback JavaScript function to which the corresponding instance of [ScriptChoice] is passed.
     *
     * @see ScriptChoice
     * @see ChoiceConfigurable
     */
    @Suppress("unused")
    fun registerChoice(
        choiceConfigurable: ChoiceConfigurable<Choice>, choiceObject: Map<String, Any>,
        callback: (Choice) -> Unit
    ) {
        ScriptChoice(choiceObject, choiceConfigurable).apply {
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

        registeredModules.forEach(ModuleManager::addModule)
        registeredCommands.forEach(CommandManager::addCommand)

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

        registeredModules.forEach(ModuleManager::removeModule)
        registeredCommands.forEach(CommandManager::removeCommand)

        registeredChoices.forEach { it.parent.choices.remove(it) }

        EventManager.callEvent(RefreshArrayListEvent)

        scriptEnabled = false
    }

    /**
     * Called when the client unloads the script.
     */

    fun close() {
        context.close(true)
    }

    /**
     * Calls the handler of a registered event.
     * @param eventName Name of the event to be called.
     */
    private fun callGlobalEvent(eventName: String) {
        try {
            globalEvents[eventName]?.invoke()
        } catch (throwable: Throwable) {
            logger.error(
                "${file.name}::$scriptName -> Event Function $eventName threw an error",
                throwable
            )
        }
    }
}
