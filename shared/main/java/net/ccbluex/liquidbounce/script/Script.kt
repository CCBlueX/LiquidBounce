/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script

import jdk.internal.dynalink.beans.StaticClass
import jdk.nashorn.api.scripting.JSObject
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import jdk.nashorn.api.scripting.ScriptUtils
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.ScriptCommand
import net.ccbluex.liquidbounce.script.api.ScriptModule
import net.ccbluex.liquidbounce.script.api.ScriptTab
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.script.api.global.Item
import net.ccbluex.liquidbounce.script.api.global.Setting
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.io.File
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.function.Function
import javax.crypto.Cipher
import javax.script.ScriptEngine
import kotlin.collections.HashMap

class Script(val scriptFile: File) : MinecraftInstance() {
    private val scriptEngine: ScriptEngine
    private val scriptText: String

    // Script information
    lateinit var scriptName: String
    lateinit var scriptVersion: String
    lateinit var scriptAuthors: Array<String>

    private var state = false

    private val events = HashMap<String, JSObject>()

    private val registeredModules = mutableListOf<Module>()
    private val registeredCommands = mutableListOf<Command>()

    val isSignatureValid: Boolean

    companion object {
        /**
         * Throws an exception when the script is invalid
         */
        fun checkSignature(scriptText: String): String {
            val indexOfLinebreak = scriptText.indexOf('\n')
            val signature = scriptText.subSequence("// ".length, indexOfLinebreak)

            val split = signature.split(" ")

            val decoder = Base64.getDecoder()

            val publicKey = decoder.decode(split[0])
            val encryptedSha512Hash = decoder.decode(split[1])

            val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            val sha512 = MessageDigest.getInstance("SHA-512")

            if (!LiquidBounce.scriptManager.allowedPublicKeys.any { Arrays.equals(sha512.digest(publicKey), it) })
                throw IllegalStateException("Unknown public key (Try updating the script)")

            rsa.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKey)))

            if (!Arrays.equals(sha512.digest(scriptText.substring(indexOfLinebreak + 1, scriptText.length).toByteArray(Charset.forName("UTF-8"))), rsa.doFinal(encryptedSha512Hash))) {
                throw IllegalStateException("Encrypted hash doesn't match")
            }

            return scriptText
        }
    }

    init {
        var isSignatureValid = false

        var scriptText = scriptFile.readText()

        if (scriptFile.name.endsWith(".signed.js")) {
            try {
                checkSignature(scriptText)

                scriptText = scriptText.substring(scriptText.indexOf('\n') + 1)

                isSignatureValid = true
            } catch (e: Throwable) {
                throw IllegalStateException("Failed to verify signature", e)
            }
        }

        this.isSignatureValid = isSignatureValid
        this.scriptText = scriptText

        val engineFlags = getMagicComment("engine_flags")?.split(",")?.toTypedArray() ?: emptyArray()
        scriptEngine = NashornScriptEngineFactory().getScriptEngine(*engineFlags)

        // Global classes
        scriptEngine.put("Chat", StaticClass.forClass(Chat::class.java))
        scriptEngine.put("Setting", StaticClass.forClass(Setting::class.java))
        scriptEngine.put("Item", StaticClass.forClass(Item::class.java))

        // Global instances
        scriptEngine.put("mc", mc)
        scriptEngine.put("moduleManager", LiquidBounce.moduleManager)
        scriptEngine.put("commandManager", LiquidBounce.commandManager)
        scriptEngine.put("scriptManager", LiquidBounce.scriptManager)

        // Global functions
        scriptEngine.put("registerScript", RegisterScript())

        supportLegacyScripts()
    }

    fun initScript() {
        scriptEngine.eval(scriptText)

        callEvent("load")

        ClientUtils.getLogger().info("[ScriptAPI] Successfully loaded script '${scriptFile.name}'.")
    }

    @Suppress("UNCHECKED_CAST")
    inner class RegisterScript : Function<JSObject, Script> {
        /**
         * Global function 'registerScript' which is called to register a script.
         * @param scriptObject JavaScript object containing information about the script.
         * @return The instance of this script.
         */
        override fun apply(scriptObject: JSObject): Script {
            scriptName = scriptObject.getMember("name") as String
            scriptVersion = scriptObject.getMember("version") as String
            scriptAuthors = ScriptUtils.convert(scriptObject.getMember("authors"), Array<String>::class.java) as Array<String>

            return this@Script
        }
    }

    /**
     * Registers a new script module.
     * @param moduleObject JavaScript object containing information about the module.
     * @param callback JavaScript function to which the corresponding instance of [ScriptModule] is passed.
     * @see ScriptModule
     */
    @Suppress("unused")
    fun registerModule(moduleObject: JSObject, callback: JSObject) {
        val module = ScriptModule(moduleObject)
        LiquidBounce.moduleManager.registerModule(module)
        registeredModules += module
        callback.call(moduleObject, module)
    }

    /**
     * Registers a new script command.
     * @param commandObject JavaScript object containing information about the command.
     * @param callback JavaScript function to which the corresponding instance of [ScriptCommand] is passed.
     * @see ScriptCommand
     */
    @Suppress("unused")
    fun registerCommand(commandObject: JSObject, callback: JSObject) {
        val command = ScriptCommand(commandObject)
        LiquidBounce.commandManager.registerCommand(command)
        registeredCommands += command
        callback.call(commandObject, command)
    }

    /**
     * Registers a new creative inventory tab.
     * @param tabObject JavaScript object containing information about the tab.
     * @see ScriptTab
     */
    @Suppress("unused")
    fun registerTab(tabObject: JSObject) {
        ScriptTab(tabObject)
    }

    /**
     * Gets the value of a magic comment from the script. Used for specifying additional information about the script.
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
     * Adds support for scripts made for LiquidBounce's original script API.
     */
    private fun supportLegacyScripts() {
        if (getMagicComment("api_version") != "2") {
            ClientUtils.getLogger().info("[ScriptAPI] Running script '${scriptFile.name}' with legacy support.")
            val legacyScript = LiquidBounce::class.java.getResource("/assets/minecraft/liquidbounce/scriptapi/legacy.js").readText()
            scriptEngine.eval(legacyScript)
        }
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: JSObject) {
        events[eventName] = handler
    }

    /**
     * Called when the client enables the script.
     */
    fun onEnable() {
        if (state) return

        callEvent("enable")
        state = true
    }

    /**
     * Called when the client disables the script. Handles unregistering all modules and commands
     * created with this script.
     */
    fun onDisable() {
        if (!state) return

        registeredModules.forEach { LiquidBounce.moduleManager.unregisterModule(it) }
        registeredCommands.forEach { LiquidBounce.commandManager.unregisterCommand(it) }

        callEvent("disable")
        state = false
    }

    /**
     * Imports another JavaScript file into the context of this script.
     * @param scriptFile Path to the file to be imported.
     */
    fun import(scriptFile: String) {
        val fileName = if (isSignatureValid) {
            if (scriptFile.contains(".")) {
                val idx = scriptFile.lastIndexOf('.')
                scriptFile.substring(0, idx) + ".signed" + scriptFile.substring(idx)
            } else {
                "$scriptFile.signed"
            }
        } else {
            scriptFile
        }

        val scriptText = File(LiquidBounce.scriptManager.scriptsFolder, fileName).readText()

        scriptEngine.eval(if (isSignatureValid) checkSignature(scriptText) else scriptText)
    }

    /**
     * Calls the handler of a registered event.
     * @param eventName Name of the event to be called.
     */
    private fun callEvent(eventName: String) {
        try {
            events[eventName]?.call(null)
        } catch (throwable: Throwable) {
            ClientUtils.getLogger().error("[ScriptAPI] Exception in script '$scriptName'!", throwable)
        }
    }
}