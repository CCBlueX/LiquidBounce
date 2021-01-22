/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.exploit.Damage

class HurtCommand : Command("hurt")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		var damage = 1

		if (args.size > 1)
		{
			try
			{
				damage = args[1].toInt()
			} catch (ignored: NumberFormatException)
			{
				chatSyntaxError()
				return
			}
		}

		// Latest NoCheatPlus damage exploit
		Damage.damage(damage)

		// Output message
		chat("You were damaged.")
	}
}
