/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.bindings.features.JsModule
import net.ccbluex.liquidbounce.script.bindings.features.JsSetting
import net.ccbluex.liquidbounce.script.bindings.globals.JsChat
import net.ccbluex.liquidbounce.script.bindings.globals.JsClient
import net.ccbluex.liquidbounce.script.bindings.globals.JsItem
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import java.io.File
import java.util.function.Function

class Script(val scriptFile: File) {

    private val context: Context

    private val scriptText: String = scriptFile.readText()

    // Script information
    lateinit var scriptName: String
    lateinit var scriptVersion: String
    lateinit var scriptAuthors: Array<String>

    private var state = false

    private val events = HashMap<String, Function<Event?, Void>>()

    private val registeredModules = mutableListOf<Module>()
    private val registeredCommands = mutableListOf<Command>()

    init {
        context = Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup { true }
            .allowExperimentalOptions(true)
            .option("js.nashorn-compat", "true")
            .option("js.ecmascript-version", "2021")
            .build()

        // Global instances
        val jsBindings = context.getBindings("js")
        jsBindings.putMember("Chat", JsChat)
        jsBindings.putMember("Setting", JsSetting)
        jsBindings.putMember("Item", JsItem)

        jsBindings.putMember("mc", mc)
        jsBindings.putMember("client", JsClient)

        // Global functions
        jsBindings.putMember("registerScript", RegisterScript())
    }

    /**
     * Initialization of script
     */
    fun initScript() {
        context.eval("js", scriptText)
        callEvent("load")
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
    fun registerModule(moduleObject: Map<String, Any>, callback: Function<Module, Void>) {
        val module = JsModule(moduleObject)
        ModuleManager.addModule(module)
        registeredModules += module
        callback.apply(module)
    }

    /**
     * Gets the value of a magic comment from the script. Used for specifying additional information about the script.
     *
     * @param name Name of the comment.
     * @return Value of the comment.
     */
    private fun getMagicComment(name: String): String? {
        val magicPrefix = "///"

        scriptText.lines().forEach {
            if (!it.startsWith(magicPrefix)) return null

            val commentData = it.substring(magicPrefix.length).split("=", limit = 2)

            if (commentData.first().trim() == name) {
                return commentData.last().trim()
            }
        }

        return null
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: Function<Event?, Void>) {
        events[eventName] = handler
    }

    /**
     * Called when the client enables the script.
     */
    fun enable() {
        if (state) return

        callEvent("enable")
        state = true
    }

    /**
     * Called when the client disables the script. Handles unregistering all modules and commands
     * created with this script.
     */
    fun disable() {
        if (!state) return

        callEvent("disable")
        state = false
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
    private fun callEvent(eventName: String) {
        try {
            events[eventName]?.apply(null)
        } catch (throwable: Throwable) {
            logger.error("[ScriptAPI] Exception in script '$scriptName'!", throwable)
        }
    }
}
