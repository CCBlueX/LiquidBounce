/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class GiveCommand : Command("give", "item", "i", "get")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer ?: return

		if (mc.playerController.isNotCreative)
		{
			chat(thePlayer, "\u00A7c\u00A7lError: \u00A73You need to be in creative mode.")
			return
		}

		if (args.size > 1)
		{
			val itemStack = ItemUtils.createItem(StringUtils.toCompleteString(args, 1))

			if (itemStack == null)
			{
				chatSyntaxError(thePlayer)
				return
			}

			val inventoryContainer = thePlayer.inventoryContainer

			var emptySlot = (36..44).firstOrNull { inventoryContainer.getSlot(it).stack == null } ?: -1

			if (emptySlot == -1) emptySlot = (9..44).firstOrNull { inventoryContainer.getSlot(it).stack == null } ?: -1

			if (emptySlot != -1)
			{
				mc.netHandler.addToSendQueue(classProvider.createCPacketCreativeInventoryAction(emptySlot, itemStack))
				chat(thePlayer, "\u00A77Given [\u00A78${itemStack.displayName}\u00A77] * \u00A78${itemStack.stackSize}\u00A77 to \u00A78${mc.session.username}\u00A77.")
			}
			else chat(thePlayer, "Your inventory is full.")

			return
		}

		chatSyntax(thePlayer, "give <item> [amount] [data] [datatag]")
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> return functions.getItemRegistryKeys().map { it.resourcePath.toLowerCase() }.filter { it.startsWith(args[0], true) }.toList()
			else -> emptyList()
		}
	}
}
