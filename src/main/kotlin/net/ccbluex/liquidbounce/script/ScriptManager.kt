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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.io.FileFilter

object ScriptManager {

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
        scriptsRoot.listFiles(FileFilter { it.name.endsWith(".js") })?.forEach(ScriptManager::loadScript)
    }

    /**
     * Unloads all scripts.
     */
    fun unloadScripts() {
        loadedScripts.forEach(Script::disable)
        loadedScripts.clear()
    }

    /**
     * Loads a script from a file.
     */
    fun loadScript(file: File) = runCatching {
        val script = Script(file).also { loadedScripts += it }
        script.initScript()

        script
    }.onFailure {
        logger.error("Unable to load script ${file.name}.", it)
    }.getOrNull()

    /**
     * Enables all scripts.
     */
    fun enableScripts() = loadedScripts.forEach(Script::enable)

    /**
     * Disables all scripts.
     */
    fun disableScripts() = loadedScripts.forEach(Script::disable)

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
        disableScripts()
        unloadScripts()
        loadScripts()
        enableScripts()

        logger.info("Successfully reloaded scripts.")
    }
}
