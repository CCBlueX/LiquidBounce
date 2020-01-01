package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.io.File
import java.io.FileFilter

/**
 * A nashorn based script manager
 *
 * @author CCBlueX
 */
class ScriptManager {

    val scripts = mutableListOf<Script>()

    val scriptsFolder = File(LiquidBounce.CLIENT.fileManager.dir, "scripts")
    private val scriptFileExtension = ".js"

    /**
     * Load scripts from directory
     */
    fun loadScripts() {
        if(!scriptsFolder.exists())
            scriptsFolder.mkdir()

        scriptsFolder.listFiles(FileFilter { it.name.endsWith(scriptFileExtension) }).forEach { loadScript(it) }
    }

    /**
     *
     */
    fun unloadScripts() {
        scripts.clear()
    }

    /**
     * Load script from file
     */
    fun loadScript(scriptFile : File) {
        try {
            scripts.add(Script(scriptFile))
            ClientUtils.getLogger().info("Successfully loaded script: ${scriptFile.name}")
        } catch(t : Throwable) {
            ClientUtils.getLogger().error("Failed to load script: ${scriptFile.name}", t)
        }
    }

    /**
     * Enable all scripts
     */
    fun enableScripts() {
        scripts.forEach { it.onEnable() }
    }

    /**
     * Disable all scripts
     */
    fun disableScripts() {
        scripts.forEach { it.onDisable() }
    }

    /**
     * Import script
     */
    fun importScript(file : File) {
        val scriptFile = File(scriptsFolder, file.name)
        file.copyTo(scriptFile)

        loadScript(scriptFile)
        ClientUtils.getLogger().info("Successfully imported script: ${scriptFile.name}")
    }

    /**
     * Delete script
     */
    fun deleteScript(script : Script) {
        script.onDisable()
        scripts.remove(script)
        script.scriptFile.delete()

        ClientUtils.getLogger().info("Successfully deleted script: ${script.scriptFile.name}")
    }

    /**
     * Reload scripts
     */
    fun reloadScripts() {
        disableScripts()
        unloadScripts()
        loadScripts()
        enableScripts()

        ClientUtils.getLogger().info("Successfully reloaded scripts.")
    }
}