/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class EnchantCommand : Command("enchant")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 2)
		{
			if (mc.playerController.isNotCreative)
			{
				chat(thePlayer, "\u00A7c\u00A7lError: \u00A73You need to be in creative mode.")
				return
			}

			val item = mc.thePlayer?.heldItem

			if (item?.item == null)
			{
				chat(thePlayer, "\u00A7c\u00A7lError: \u00A73You need to hold an item.")
				return
			}

			val enchantID: Int = try
			{
				args[1].toInt()
			}
			catch (e: NumberFormatException)
			{
				val enchantment = functions.getEnchantmentByLocation(args[1])

				if (enchantment == null)
				{
					chat(thePlayer, "There is no enchantment with the name '${args[1]}'")
					return
				}

				enchantment.effectId
			}

			val enchantment = functions.getEnchantmentById(enchantID)

			if (enchantment == null)
			{
				chat(thePlayer, "There is no enchantment with the ID '$enchantID'")
				return
			}

			val level = try
			{
				args[2].toInt()
			}
			catch (e: NumberFormatException)
			{
				chatSyntaxError(thePlayer)
				return
			}

			item.addEnchantment(enchantment, level)
			mc.netHandler.addToSendQueue(classProvider.createCPacketCreativeInventoryAction(36 + mc.thePlayer!!.inventory.currentItem, item))
			chat(thePlayer, "${enchantment.getTranslatedName(level)} added to ${item.displayName}.")
			return
		}

		chatSyntax(thePlayer, "enchant <type> [level]")
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> return functions.getEnchantments().asSequence().map { it.resourcePath.toLowerCase() }.filter { it.startsWith(args[0], true) }.toList()

			else -> emptyList()
		}
	}
}
