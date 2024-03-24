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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

object ScriptManager {

    private val scriptExtensions = arrayOf("js", "mjs")

    // Loaded scripts
    val loadedScripts = mutableListOf<Script>()

    // Store all of your scripts into this folder to get loaded
    val scriptsRoot = File(ConfigSystem.rootFolder, "scripts").apply {
        if (!exists()) {
            mkdir()
        }
    }

    /**
     * Loads all scripts inside the scripts folder.
     */
    fun loadScripts() {
        scriptsRoot.listFiles {
            file -> scriptExtensions.contains(file.extension)|| file.isDirectory
        }?.forEach { file ->
            if (file.isDirectory) {
                // If we find a directory, we look for a main.js or main.mjs file inside it
                val mainFile = file.listFiles {
                    dirFile -> dirFile.nameWithoutExtension == "main" && scriptExtensions.contains(dirFile.extension)
                }?.firstOrNull()

                if (mainFile != null) {
                    loadSafely(mainFile)
                } else {
                    logger.warn("Unable to find main.js or main.mjs inside the directory ${file.name}.")
                }
            } else {
                // If the file is a script, we load it immediately
                loadSafely(file)
            }
        }

        // After loading we enable all the scripts
        enableScripts()
    }

    /**
     * Unloads all scripts.
     */
    fun unloadScripts() {
        loadedScripts.forEach(Script::disable)
        loadedScripts.clear()
    }

    private fun loadSafely(file: File) = runCatching {
        loadScript(file)
    }.onFailure {
        logger.error("Unable to load script ${file.name}.", it)
    }.getOrNull()

    /**
     * Loads a script from a file.
     */
    fun loadScript(file: File): Script {
        val script = Script(file)
        script.initScript()

        loadedScripts += script
        return script
    }

    fun unloadScript(script: Script) {
        script.disable()
        loadedScripts.remove(script)
    }

    /**
     * Enables all scripts.
     */
    fun enableScripts() {
        loadedScripts.forEach(Script::enable)
    }

    /**
     * Disables all scripts.
     */
    fun disableScripts() {
        loadedScripts.forEach(Script::disable)
    }

    /**
     * Imports a script.
     * @param file JavaScript file to be imported.
     */
    fun importScript(file: File) {
        val scriptFile = File(scriptsRoot, file.name)
        file.copyTo(scriptFile)

        loadScript(scriptFile)
        logger.info("Successfully imported script '${scriptFile.name}'.")
    }

    /**
     * Deletes a script.
     * @param script Script to be deleted.
     */
    fun deleteScript(script: Script) {
        script.disable()
        loadedScripts.remove(script)
        script.scriptFile.delete()

        logger.info("Successfully deleted script '${script.scriptFile.name}'.")
    }

    /**
     * Reloads all scripts.
     */
    fun reloadScripts() {
        unloadScripts()
        loadScripts()
        enableScripts()

        logger.info("Successfully reloaded scripts.")
    }
}
