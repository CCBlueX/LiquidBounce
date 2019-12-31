package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class GiveCommand : Command("give", arrayOf("item", "i", "get")) {
    override fun execute(args: Array<String>) {
        if (mc.playerController.isNotCreative) {
            chat("Creative mode only.")
            return
        }

        if (args.size > 1) {
            val itemStack = ItemUtils.createItem(StringUtils.toCompleteString(args, 1))

            if (itemStack == null) {
                chatSyntaxError()
                return
            }

            var emptySlot = -1
            for (i in 9..44) {
                mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue

                emptySlot = i
            }

            if (emptySlot != -1) {
                mc.netHandler.addToSendQueue(C10PacketCreativeInventoryAction(emptySlot, itemStack))
                chat("§7Given [§8${itemStack.displayName}§7] * §8${itemStack.stackSize}§7 to §8${mc.getSession().username}§7.")
            } else
                chat("Your inventory is full.")

            return
        }
        chatSyntax("give <item> [amount] [data] [datatag]")
    }
}