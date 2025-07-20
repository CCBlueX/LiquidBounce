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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.script.bindings.api.ScriptAsyncUtil
import net.ccbluex.liquidbounce.script.bindings.api.ScriptContextProvider
import net.ccbluex.liquidbounce.utils.client.logger
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import java.io.File

/**
 * The ScriptManager allows to extend the client by loading supported scripts at runtime.
 * Scripts can be written in various languages when installed through GraalVM
 * and can interact with the client through the Script API.
 *
 * Scripts are stored in the scripts directory and can be organized in subdirectories when using a main script file.
 */
object ScriptManager {

    private var isInitialized = false

    /**
     * A list that holds all the loaded scripts.
     */
    val scripts = mutableSetOf<PolyglotScript>()

    /**
     * The root directory where all scripts are stored. This directory is created if it does not exist.
     */
    val root = File(ConfigSystem.rootFolder, "scripts").apply {
        if (!exists()) {
            mkdir()
        }
    }

    fun initializeEngine() {
        ScriptAsyncUtil.TickScheduler

        // Initialize the script engine and log its version and supported languages.
        val engine = Engine.create()
        logger.info(
            "[ScriptAPI] Engine Version: ${engine.version}, " +
                "Supported languages: [ ${engine.languages.keys.joinToString(", ")} ]"
        )
        isInitialized = true
    }

    /**
     * Loads all scripts found in the scripts directory. This method scans the directory for script files
     * and directories containing a main script file. It then loads and enables all found scripts.
     */
    fun loadAll() {
        require(isInitialized) { "Cannot load scripts before the script engine is initialized." }

        root.listFiles { file ->
            Source.findLanguage(file) != null || file.isDirectory
        }?.forEach { file ->
            if (file.isDirectory) {
                // If a directory is found, look for a main script file inside it.
                val mainFile = file.listFiles { dirFile ->
                    dirFile.nameWithoutExtension == "main" && Source.findLanguage(dirFile) != null
                }?.firstOrNull()

                if (mainFile != null) {
                    loadCatched(mainFile)
                } else {
                    logger.warn("Unable to find main inside the directory ${file.name}.")
                }
            } else {
                // If the file is a script, load it immediately.
                loadCatched(file)
            }
        }

        // After loading, enable all the scripts.
        enableAll()
    }

    /**
     * Unloads all currently loaded scripts. This method disables each script and clears the list of loaded scripts.
     */
    fun unloadAll() {
        scripts.forEach(PolyglotScript::disable)
        scripts.forEach(PolyglotScript::close)
        scripts.clear()
        ScriptAsyncUtil.TickScheduler.clear()
        ScriptContextProvider.cleanup()
    }

    /**
     * Loads a script from a file and catches any exceptions that occur during the loading process.
     * This ensures that a single faulty script does not prevent other scripts from being loaded.
     *
     * @param file The script file to load.
     */
    private fun loadCatched(file: File) = runCatching {
        loadScript(file)
    }.onFailure {
        logger.error("Unable to load script ${file.name}.", it)
    }.getOrNull()

    /**
     * Loads a script from a file. This method creates a new Script object, initializes it, and adds it to the list
     * of loaded scripts.
     *
     * @param file The script file to load.
     * @param language The language of the script. If not specified, it is inferred from the file.
     * @return The loaded script.
     */
    fun loadScript(
        file: File,
        language: String = Source.findLanguage(file),
        debugOptions: ScriptDebugOptions = ScriptDebugOptions()
    ): PolyglotScript {
        require(isInitialized) { "Cannot load scripts before the script engine is initialized." }

        val script = PolyglotScript(language, file, debugOptions)
        script.initScript()

        scripts += script
        return script
    }

    /**
     * Unloads a specific script. This method disables the script and removes it from the list of loaded scripts.
     *
     * @param script The script to unload.
     */
    fun unloadScript(script: PolyglotScript) {
        script.disable()
        script.close()
        scripts.remove(script)
    }

    /**
     * Enables all loaded scripts. This method iterates over the list of loaded scripts and enables each one.
     */
    fun enableAll() {
        scripts.forEach(PolyglotScript::enable)

        if (scripts.isNotEmpty()) {
            // Reload the ClickGUI to update the module list.
            RenderSystem.recordRenderCall(ModuleClickGui::reload)
        }
    }

    /**
     * Disables all loaded scripts. This method iterates over the list of loaded scripts and disables each one.
     */
    fun disableAll() {
        scripts.forEach(PolyglotScript::disable)
    }

    /**
     * Reloads all scripts. This method unloads all currently loaded scripts, loads them again from the scripts
     * directory, and then enables them. It logs a message upon successful completion.
     */
    fun reload() {
        // Unload
        try {
            disableAll()
            unloadAll()
        } catch (e: Exception) {
            logger.error("Failed to unload scripts.", e)
        }

        // Load
        loadAll()
        enableAll()

        logger.info("Successfully reloaded scripts.")
    }
}
