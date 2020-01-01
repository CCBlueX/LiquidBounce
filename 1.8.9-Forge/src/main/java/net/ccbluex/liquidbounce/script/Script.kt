package net.ccbluex.liquidbounce.script

import jdk.internal.dynalink.beans.StaticClass
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.script.api.*
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.io.File
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * A nashorn based script
 *
 * @author CCBlueX
 */
class Script(val scriptFile: File) : MinecraftInstance() {

    private lateinit var scriptEngine : ScriptEngine
    private lateinit var invocable : Invocable

    lateinit var scriptName : String
    var scriptVersion : Double = 1.0
    lateinit var scriptAuthor : String

    private var state = false

    init {
        loadScript()
    }

    /**
     * Load script
     */
    fun loadScript() {
        scriptEngine = ScriptEngineManager().getEngineByName("nashorn")

        // Variables
        scriptEngine.put("mc", mc)
        scriptEngine.put("scriptManager", LiquidBounce.CLIENT.scriptManager)
        scriptEngine.put("script", this)

        scriptEngine.put("commandManager", StaticClass.forClass(CommandManager.javaClass))
        scriptEngine.put("moduleManager", StaticClass.forClass(ModuleManager.javaClass))
        scriptEngine.put("creativeTabs", StaticClass.forClass(CreativeTab.javaClass))
        scriptEngine.put("item", StaticClass.forClass(Item.javaClass))
        scriptEngine.put("value", StaticClass.forClass(Value.javaClass))
        scriptEngine.put("chat", StaticClass.forClass(Chat.javaClass))

        // Eval script
        val scriptText = scriptFile.readText()

        scriptEngine.eval(scriptText)

        // Cast script engine to invocable and set to variable
        invocable = scriptEngine as Invocable

        // Load script informations from js engine
        scriptName = scriptEngine.get("scriptName") as String
        scriptVersion = scriptEngine.get("scriptVersion") as Double
        scriptAuthor = scriptEngine.get("scriptAuthor") as String

        // Call on load
        onLoad()
    }

    /**
     * Load script
     */
    fun onLoad() = callFunction("onLoad")

    /**
     * Enable script
     */
    fun onEnable() {
        if(state)
            return

        callFunction("onEnable")
        state = true
    }

    /**
     * Disable script
     */
    fun onDisable() {
        if(!state)
            return

        callFunction("onDisable")
        state = false
    }

    /**
     * Import external script file into script engine
     */
    fun import(scriptFile : String) {
        scriptEngine.eval(File(LiquidBounce.CLIENT.scriptManager.scriptsFolder, scriptFile).readText())
    }

    private fun callFunction(functionName : String) {
        try {
            invocable.invokeFunction(functionName)
        } catch (ex: NoSuchMethodException) {
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("${scriptFile.name} caused an error.", ex)
        }
    }
}