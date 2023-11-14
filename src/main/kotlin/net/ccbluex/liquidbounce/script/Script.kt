/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.bindings.api.JsApiProvider
import net.ccbluex.liquidbounce.script.bindings.features.JsModule
import net.ccbluex.liquidbounce.script.bindings.features.JsSetting
import net.ccbluex.liquidbounce.script.bindings.globals.JsClient
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import java.io.File
import java.util.function.Function

class Script(val scriptFile: File) {

    private val context: Context = Context.newBuilder("js")
        .allowHostAccess(HostAccess.ALL) // Allow access to all Java classes
        .allowCreateProcess(false) // Disable process creation
        .allowCreateThread(false) // Disable thread creation
        .allowNativeAccess(false) // Disable native access
        .allowExperimentalOptions(true) // Allow experimental options
        .option("js.nashorn-compat", "true") // Enable Nashorn compatibility
        .option("js.ecmascript-version", "2022") // Enable ECMAScript 2022
        .build().apply {
            // Global instances
            val jsBindings = getBindings("js")

            JsApiProvider.setupUsefulContext(jsBindings)

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
    private val registeredModules = mutableListOf<Module>()

    /**
     * Initialization of scripts
     */
    fun initScript() {
        // Evaluate script
        context.eval("js", scriptText)

        // Call load event
        callGlobalEvent("load")

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
        val module = JsModule(moduleObject)
        ModuleManager.addModule(module)
        registeredModules += module
        callback(module)
    }

    /**
     * Registers a new script command
     *
     * @param commandObject JavaScript object containing information about the command.
     * @param callback JavaScript function to which the corresponding instance of [JsModule] is passed.
     * @see JsModule
     */
    @Suppress("unused")
    fun registerCommand(commandObject: Map<String, Any>, callback: (CommandBuilder) -> Unit) {
        val command = CommandBuilder
            .begin(commandObject["name"] as String)
            .alias(*((commandObject["aliases"] as? Array<*>) ?: emptyArray<String>()).map { it as String }
                .toTypedArray())
            .apply { callback(this) }
            .build()

        CommandManager.addCommand(command)
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
        scriptEnabled = false
    }

    /**
     * Imports another JavaScript file into the context of this script.
     * @param scriptFile Path to the file to be imported.
     */
    fun import(scriptFile: String) {
        val scriptText = File(ScriptManager.scriptsRoot, scriptFile).readText()

        context.eval("js", scriptText)
    }

    /**
     * Calls the handler of a registered event.
     * @param eventName Name of the event to be called.
     */
    private fun callGlobalEvent(eventName: String) {
        try {
            globalEvents[eventName]?.invoke()
        } catch (throwable: Throwable) {
            logger.error("[ScriptAPI] Exception in script '$scriptName'!", throwable)
        }
    }
}
