/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.block.BlockBush
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.network.play.server.S2EPacketCloseWindow

object InventoryUtils : MinecraftInstance(), Listenable {

    // What slot is selected on server-side?
    var serverSlot = -1
        private set

    // Is inventory open on server-side?
    var serverOpenInventory = false
        private set

    val CLICK_TIMER = MSTimer()

    val BLOCK_BLACKLIST = listOf(
        Blocks.chest,
        Blocks.ender_chest,
        Blocks.trapped_chest,
        Blocks.anvil,
        Blocks.sand,
        Blocks.web,
        Blocks.torch,
        Blocks.crafting_table,
        Blocks.furnace,
        Blocks.waterlily,
        Blocks.dispenser,
        Blocks.stone_pressure_plate,
        Blocks.wooden_pressure_plate,
        Blocks.noteblock,
        Blocks.dropper,
        Blocks.tnt,
        Blocks.standing_banner,
        Blocks.wall_banner,
        Blocks.redstone_torch
    )

    fun findItem(startInclusive: Int, endInclusive: Int, item: Item): Int? {
        for (i in startInclusive..endInclusive)
            if (mc.thePlayer.inventoryContainer.getSlot(i).stack?.item == item)
                return i

        return null
    }

    fun hasSpaceHotbar(): Boolean {
        for (i in 36..44)
            mc.thePlayer.inventoryContainer.getSlot(i).stack ?: return true

        return false
    }

    fun findBlockInHotbar(): Int? {
        val player = mc.thePlayer ?: return null
        val inventory = player.inventoryContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is ItemBlock) (stack.item as ItemBlock).block else return@filter false

            stack.item is ItemBlock && stack.stackSize > 0 && block !in BLOCK_BLACKLIST && block !is BlockBush
        }.minByOrNull { (inventory.getSlot(it).stack.item as ItemBlock).block.isFullCube }
    }

    fun findLargestBlockStackInHotbar(): Int? {
        val player = mc.thePlayer ?: return null
        val inventory = player.inventoryContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is ItemBlock) (stack.item as ItemBlock).block else return@filter false

            stack.item is ItemBlock && stack.stackSize > 0 && block.isFullCube && block !in BLOCK_BLACKLIST && block !is BlockBush
        }.maxByOrNull { inventory.getSlot(it).stack.stackSize }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.isCancelled) return

        when (val packet = event.packet) {
            is C08PacketPlayerBlockPlacement -> CLICK_TIMER.reset()

            is C16PacketClientStatus ->
                if (packet.status == OPEN_INVENTORY_ACHIEVEMENT) {
                    if (serverOpenInventory) event.cancelEvent()
                    else serverOpenInventory = true
                }

            is C0DPacketCloseWindow, is S2EPacketCloseWindow -> serverOpenInventory = false

            is C09PacketHeldItemChange -> {
                // Support for Singleplayer
                // (client packets get sent and received, duplicates would get cancelled, making slot changing impossible)
                if (event.eventType == EventState.RECEIVE) return

                if (packet.slotId == serverSlot) event.cancelEvent()
                else serverSlot = packet.slotId
            }

            is S09PacketHeldItemChange -> serverSlot = packet.heldItemHotbarIndex
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // Prevent desync
        serverOpenInventory = false
    }

    override fun handleEvents() = true
}
