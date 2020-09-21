/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance

abstract class Command(val command: String, vararg val alias: String) : MinecraftInstance() {
    /**
     * Execute commands with provided [args]
     */
    abstract fun execute(args: Array<String>)

    /**
     * Returns a list of command completions based on the provided [args].
     * If a command does not implement [tabComplete] an [EmptyList] is returned by default.
     *
     * @param args an array of command arguments that the player has passed to the command so far
     * @return a list of matching completions for the command the player is trying to autocomplete
     * @author NurMarvin
     */
    open fun tabComplete(args: Array<String>): List<String> {
        return emptyList()
    }

    /**
     * Print [msg] to chat
     */
    protected fun chat(msg: String) = ClientUtils.displayChatMessage("§a[§c§l${LiquidBounce.NEW_NAME}§a] §b$msg")

    /**
     * Print [syntax] of command to chat
     */
    protected fun chatSyntax(syntax: String) = ClientUtils.displayChatMessage("§a[§c§l${LiquidBounce.NEW_NAME}§a] §bSyntax: §7${LiquidBounce.commandManager.prefix}$syntax")

    /**
     * Print [syntaxes] of command to chat
     */
    protected fun chatSyntax(syntaxes: Array<String>) {
        ClientUtils.displayChatMessage("§a[§c§l${LiquidBounce.NEW_NAME}§a] §bSyntax:")

        for (syntax in syntaxes)
            ClientUtils.displayChatMessage("§8> §7${LiquidBounce.commandManager.prefix}$command ${syntax.toLowerCase()}")
    }

    /**
     * Print a syntax error to chat
     */
    protected fun chatSyntaxError() = ClientUtils.displayChatMessage("§a[§c§l${LiquidBounce.NEW_NAME}§a] §bSyntax error")

    /**
     * Play edit sound
     */
    protected fun playEdit() = mc.soundHandler.playSound("random.anvil_use", 1F)
}