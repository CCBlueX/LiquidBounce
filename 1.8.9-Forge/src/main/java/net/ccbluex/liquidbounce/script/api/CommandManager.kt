/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
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

        LiquidBounce.commandManager.registerCommand(command)
        return command
    }

    /**
     * Unregister a java script based command
     *
     * @param command Instance of target command
     */
    @JvmStatic
    fun unregisterCommand(command : Command) {
        LiquidBounce.commandManager.unregisterCommand(command)
    }

    /**
     * Unregister a command
     *
     * @param scriptObjectMirror Java Script function for module (like a module class)
     */
    @JvmStatic
    fun unregisterCommand(scriptObjectMirror : ScriptObjectMirror) {
        val commandName = scriptObjectMirror.callMember("getName") as String
        val command = LiquidBounce.commandManager.getCommand(commandName)

        LiquidBounce.commandManager.unregisterCommand(command)
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
        LiquidBounce.commandManager.executeCommands(command)
    }
}