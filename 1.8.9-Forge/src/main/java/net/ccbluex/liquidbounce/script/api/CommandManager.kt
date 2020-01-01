package net.ccbluex.liquidbounce.script.api

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.JSType
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command

/**
 * A script api class for command support
 *
 * @author CCBlueX
 */
object CommandManager {

    /**
     * Register a java script based command
     *
     * @param scriptObjectMirror Java Script function for command (like a command class)
     */
    @JvmStatic
    fun registerCommand(scriptObjectMirror : ScriptObjectMirror) : Command {
        val command = object : Command(JSType.toString(scriptObjectMirror.callMember("getName")), JSType.toJavaArray(scriptObjectMirror.callMember("getAliases"), String::class.java) as Array<String>) {
            override fun execute(args : Array<String>) {
                scriptObjectMirror.callMember("execute", args as Any)
            }
        }

        LiquidBounce.CLIENT.commandManager.registerCommand(command)
        return command
    }

    /**
     * Unregister a java script based command
     *
     * @param command Instance of target command
     */
    @JvmStatic
    fun unregisterCommand(command : Command) {
        LiquidBounce.CLIENT.commandManager.unregisterCommand(command)
    }

    /**
     * Unregister a command
     *
     * @param scriptObjectMirror Java Script function for module (like a module class)
     */
    @JvmStatic
    fun unregisterCommand(scriptObjectMirror : ScriptObjectMirror) {
        val commandName = scriptObjectMirror.callMember("getName") as String
        val command = LiquidBounce.CLIENT.commandManager.getCommand(commandName)

        LiquidBounce.CLIENT.commandManager.unregisterCommand(command)
    }

    /**
     *  Call a command
     *
     *  @param command Instance of target command
     *  @param args Arguments of command execution
     */
    @JvmStatic
    fun executeCommand(command : Command, args : Array<String>) {
        command.execute(args)
    }

    /**
     * Call a command
     *
     * @param command Arguments for execution
     */
    @JvmStatic
    fun executeCommand(command : String) {
        LiquidBounce.CLIENT.commandManager.executeCommands(command)
    }
}