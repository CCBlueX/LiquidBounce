/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.minecraft.item.Item
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction

class GiveCommand : Command("give", "item", "i", "get") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val thePlayer = mc.thePlayer ?: return

        if (mc.playerController.isNotCreative) {
            chat("§c§lError: §3You need to be in creative mode.")
            return
        }

        if (args.size > 1) {
            val itemStack = ItemUtils.createItem(StringUtils.toCompleteString(args, 1))

            if (itemStack == null) {
                chatSyntaxError()
                return
            }

            var emptySlot = -1

            for (i in 36..44) {
                if (thePlayer.inventoryContainer.getSlot(i).stack == null) {
                    emptySlot = i
                    break
                }
            }

            if (emptySlot == -1) {
                for (i in 9..44) {
                    if (thePlayer.inventoryContainer.getSlot(i).stack == null) {
                        emptySlot = i
                        break
                    }
                }
            }

            if (emptySlot != -1) {
                mc.netHandler.addToSendQueue(C10PacketCreativeInventoryAction(emptySlot, itemStack))
                chat("§7Given [§8${itemStack.displayName}§7] * §8${itemStack.stackSize}§7 to §8${mc.session.username}§7.")
            } else
                chat("Your inventory is full.")
            return
        }

        chatSyntax("give <item> [amount] [data] [datatag]")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty())
            return emptyList()

        return when (args.size) {
            1 -> {
                return Item.itemRegistry.keys
                    .map { it.resourcePath.lowercase() }
                    .filter { it.startsWith(args[0], true) }
            }
            else -> emptyList()
        }
    }
}