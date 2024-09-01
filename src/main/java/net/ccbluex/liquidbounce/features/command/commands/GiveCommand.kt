/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.minecraft.item.Item
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

object GiveCommand : Command("give", "item", "i", "get") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val player = mc.player ?: return
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <item> [amount] [data] [datatag]")
            return
        }

        if (!mc.interactionManager.currentGameMode.isCreative) {
            chat("§c§lError: §3You need to be in creative mode.")
            return
        }

        val itemStack = ItemUtils.createItem(StringUtils.toCompleteString(args, 1))

        if (itemStack == null) {
            chatSyntaxError()
            return
        }

        val emptySlot = player.inventory.firstEmptyStack

        if (emptySlot != -1) {
            sendPacket(CreativeInventoryActionC2SPacket(emptySlot, itemStack))
            chat("§7Given [§8${itemStack.displayName}§7] * §8${itemStack.count}§7 to §8${mc.session.username}§7.")
        } else {
            chat("Your inventory is full.")
        }

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