/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance

abstract class Command(val command: String, vararg val alias: String) : MinecraftInstance()
{
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
	open fun tabComplete(args: Array<String>): List<String> = emptyList()

	/**
	 * Print [msg] to chat
	 */
	protected fun chat(thePlayer: IEntityPlayerSP?, msg: String) = ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A79\u00A7l${LiquidBounce.CLIENT_NAME}\u00A78] \u00A73$msg")

	/**
	 * Print [syntax] of command to chat
	 */
	protected fun chatSyntax(thePlayer: IEntityPlayerSP?, syntax: String) = ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A79\u00A7l${LiquidBounce.CLIENT_NAME}\u00A78] \u00A73Syntax: \u00A77${LiquidBounce.commandManager.prefix}$syntax")

	/**
	 * Print [syntaxes] of command to chat
	 */
	protected fun chatSyntax(thePlayer: IEntityPlayerSP?, syntaxes: Array<String>)
	{
		ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A79\u00A7l${LiquidBounce.CLIENT_NAME}\u00A78] \u00A73Syntax:")

		val commandPrefix = LiquidBounce.commandManager.prefix

		syntaxes.map(String::toLowerCase).forEach { ClientUtils.displayChatMessage(thePlayer, "\u00A78> \u00A77$commandPrefix$command $it") }
	}

	/**
	 * Print a syntax error to chat
	 */
	protected fun chatSyntaxError(thePlayer: IEntityPlayerSP?) = ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A79\u00A7l${LiquidBounce.CLIENT_NAME}\u00A78] \u00A73Syntax error")

	/**
	 * Play edit sound
	 */
	protected fun playEdit() = mc.soundHandler.playSound("random.anvil_use", 1F)
}
