/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.GuiUnsignedScripts
import net.ccbluex.liquidbounce.utils.BarrageFile
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

class ScriptManager {

    private lateinit var allowedPublicKeys: List<ByteArray>
    val scripts = mutableListOf<Script>()

    /**
     * A list of scripts that are unsigned and require the ok of the user
     */
    val lateInitScripts = mutableListOf<Script>()

    val scriptsFolder = File(LiquidBounce.fileManager.dir, "scripts")
    private val scriptFileExtension = ".js"

    /**
     * Loads all scripts inside the scripts folder.
     */
    fun loadScripts() {
        if (!scriptsFolder.exists())
            scriptsFolder.mkdir()

        scriptsFolder.listFiles(FileFilter { it.name.endsWith(scriptFileExtension) })?.forEach(this@ScriptManager::loadScript)
    }

    /**
     * Unloads all scripts.
     */
    fun unloadScripts() {
        scripts.clear()
    }

    /**
     * Loads a script from a file.
     */
    fun loadScript(scriptFile: File) {
        try {
            val script = Script(scriptFile)

            if (script.isSignatureValid) {
                script.initScript()

                scripts.add(script)
            } else {
                lateInitScripts.add(script)

                ClientUtils.getLogger().info("[ScriptAPI] Postponed the initialization of '${scriptFile.name}'.")
            }
        } catch (t: Throwable) {
            ClientUtils.getLogger().error("[ScriptAPI] Failed to load script '${scriptFile.name}'.", t)
        }
    }

    /**
     * Enables all scripts.
     */
    fun enableScripts() {
        scripts.forEach { it.onEnable() }
    }

    /**
     * Disables all scripts.
     */
    fun disableScripts() {
        scripts.forEach { it.onDisable() }
    }

    /**
     * Imports a script.
     * @param file JavaScript file to be imported.
     */
    fun importScript(file: File) {
        val scriptFile = File(scriptsFolder, file.name)
        file.copyTo(scriptFile)

        loadScript(scriptFile)
        ClientUtils.getLogger().info("[ScriptAPI] Successfully imported script '${scriptFile.name}'.")
    }

    /**
     * Deletes a script.
     * @param script Script to be deleted.
     */
    fun deleteScript(script: Script) {
        script.onDisable()
        scripts.remove(script)
        script.scriptFile.delete()

        ClientUtils.getLogger().info("[ScriptAPI]  Successfully deleted script '${script.scriptFile.name}'.")
    }

    /**
     * Reloads all scripts.
     */
    fun reloadScripts() {
        disableScripts()
        unloadScripts()
        loadScripts()
        enableScripts()

        LiquidBounce.wrapper.minecraft.displayGuiScreen(LiquidBounce.wrapper.classProvider.wrapGuiScreen(GuiUnsignedScripts()))

        ClientUtils.getLogger().info("[ScriptAPI]  Successfully reloaded scripts.")
    }

    fun refreshAuthority() {
        val file = File(LiquidBounce.fileManager.dir, "allowed_keys")

        try {
            HttpUtils.download("${LiquidBounce.CLIENT_CLOUD}/allowed_keys", file)
        } catch (e: Throwable) {
            println("[ScriptAPI] Failed to download latest authority data")
            e.printStackTrace()
        }

        try {
            FileInputStream(file).use(this::loadPublicKeyHashes)
        } catch (e: Throwable) {
            try {
                ScriptManager::class.java.getResourceAsStream("/allowed_keys").use(this::loadPublicKeyHashes)
            } catch (e: Throwable) {
                throw Error("[ScriptAPI] Failed to load authority data", e)
            }
        }
    }

    private fun loadPublicKeyHashes(inputStream: InputStream) {
        val data = BarrageFile.read(inputStream) {
            val sha512: MessageDigest = MessageDigest.getInstance("SHA-512")

            Arrays.equals(sha512.digest(it), AUTHORITY_CERTIFICATE_HASH)
        }

        this.allowedPublicKeys = FileManager.PRETTY_GSON.fromJson(String(data, Charset.forName("UTF-8")), Array<String>::class.java).map(BarrageFile::hexStringToByteArray)
    }

    companion object {
        private val AUTHORITY_CERTIFICATE_HASH = BarrageFile.hexStringToByteArray("eebbf89753ab1af9fc435f3e01d7120916d31a3ac2c9ba6a80b23504088c5b44b656c63074fb7da70fb7dfe8344f041819ae676559e6ca555c628407c8d80e3a")
    }
}