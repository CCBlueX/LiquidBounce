/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.EntityUtils

class TargetCommand : Command("target")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 1)
		{
			when (args[1].toLowerCase())
			{
				"players" ->
				{
					EntityUtils.targetPlayer = !EntityUtils.targetPlayer
					chat(thePlayer, "\u00A77Target player toggled ${if (EntityUtils.targetPlayer) "on" else "off"}.")
					playEdit()
					return
				}

				"mobs" ->
				{
					EntityUtils.targetMobs = !EntityUtils.targetMobs
					chat(thePlayer, "\u00A77Target mobs toggled ${if (EntityUtils.targetMobs) "on" else "off"}.")
					playEdit()
					return
				}

				"animals" ->
				{
					EntityUtils.targetAnimals = !EntityUtils.targetAnimals
					chat(thePlayer, "\u00A77Target animals toggled ${if (EntityUtils.targetAnimals) "on" else "off"}.")
					playEdit()
					return
				}

				"invisible" ->
				{
					EntityUtils.targetInvisible = !EntityUtils.targetInvisible
					chat(thePlayer, "\u00A77Target Invisible toggled ${if (EntityUtils.targetInvisible) "on" else "off"}.")
					playEdit()
					return
				}
			}
		}

		chatSyntax(thePlayer, "target <players/mobs/animals/invisible>")
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> listOf("players", "mobs", "animals", "invisible").filter { it.startsWith(args[0], true) }
			else -> emptyList()
		}
	}
}
