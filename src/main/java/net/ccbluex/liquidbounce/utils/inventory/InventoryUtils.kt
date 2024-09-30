/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.misc.NoSlotSet
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions
import net.minecraft.block.BlockBush
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow

object InventoryUtils : MinecraftInstance(), Listenable {

    // What slot is selected on server-side?
    // TODO: Is this equal to mc.playerController.currentPlayerItem?
    var serverSlot
        get() = _serverSlot
        set(value) {
            if (value != _serverSlot) {
                sendPacket(C09PacketHeldItemChange(value))

                _serverSlot = value
            }
        }

    // Is inventory open on server-side?
    var serverOpenInventory
        get() = _serverOpenInventory
        set(value) {
            if (value != _serverOpenInventory) {
                sendPacket(
                    if (value) C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT)
                    else C0DPacketCloseWindow(mc.thePlayer?.openContainer?.windowId ?: 0)
                )

                _serverOpenInventory = value
            }
        }

    var serverOpenContainer = false
        private set

    // Backing fields
    private var _serverSlot = 0
    private var _serverOpenInventory = false

    var isFirstInventoryClick = true

    var timeSinceClosedInventory = 0L

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
        Blocks.redstone_torch,
        Blocks.ladder
    )

    fun findItemArray(startInclusive: Int, endInclusive: Int, items: Array<Item>): Int? {
        for (i in startInclusive..endInclusive)
            if (mc.thePlayer.openContainer.getSlot(i).stack?.item in items)
                return i

        return null
    }

    fun findItem(startInclusive: Int, endInclusive: Int, item: Item): Int? {
        for (i in startInclusive..endInclusive)
            if (mc.thePlayer.openContainer.getSlot(i).stack?.item == item)
                return i

        return null
    }

    fun hasSpaceInHotbar(): Boolean {
        for (i in 36..44)
            mc.thePlayer.openContainer.getSlot(i).stack ?: return true

        return false
    }

    fun hasSpaceInInventory() = mc.thePlayer?.inventory?.firstEmptyStack != -1

    fun countSpaceInInventory() = mc.thePlayer.inventory.mainInventory.count { it.isEmpty() }

    fun findBlockInHotbar(): Int? {
        val player = mc.thePlayer ?: return null
        val inventory = player.openContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is ItemBlock) (stack.item as ItemBlock).block else return@filter false

            stack.item is ItemBlock && stack.stackSize > 0 && block !in BLOCK_BLACKLIST && block !is BlockBush
        }.minByOrNull { (inventory.getSlot(it).stack.item as ItemBlock).block.isFullCube }
    }

    fun findLargestBlockStackInHotbar(): Int? {
        val player = mc.thePlayer ?: return null
        val inventory = player.openContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is ItemBlock) (stack.item as ItemBlock).block else return@filter false

            stack.item is ItemBlock && stack.stackSize > 0 && block.isFullCube && block !in BLOCK_BLACKLIST && block !is BlockBush
        }.maxByOrNull { inventory.getSlot(it).stack.stackSize }
    }

    fun findBlockStackInHotbarGreaterThan(amount: Int): Int? {
        val player = mc.thePlayer ?: return null
        val inventory = player.openContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is ItemBlock) (stack.item as ItemBlock).block else return@filter false

            stack.item is ItemBlock && stack.stackSize > amount && block.isFullCube && block !in BLOCK_BLACKLIST && block !is BlockBush
        }.minByOrNull { (inventory.getSlot(it).stack.item as ItemBlock).block.isFullCube }
    }

    // Converts container slot to hotbar slot id, else returns null
    fun Int.toHotbarIndex(stacksSize: Int): Int? {
        val parsed = this - stacksSize + 9

        return if (parsed in 0..8) parsed else null
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {

        if (event.isCancelled) return

        when (val packet = event.packet) {
            is C08PacketPlayerBlockPlacement, is C0EPacketClickWindow -> {
                CLICK_TIMER.reset()

                if (packet is C0EPacketClickWindow)
                    isFirstInventoryClick = false
            }

            is C16PacketClientStatus ->
                if (packet.status == OPEN_INVENTORY_ACHIEVEMENT) {
                    if (_serverOpenInventory) event.cancelEvent()
                    else {
                        isFirstInventoryClick = true
                        _serverOpenInventory = true
                    }
                }

            is C0DPacketCloseWindow, is S2EPacketCloseWindow, is S2DPacketOpenWindow -> {
                isFirstInventoryClick = false
                _serverOpenInventory = false
                serverOpenContainer = false

                timeSinceClosedInventory = System.currentTimeMillis()

                if (packet is S2DPacketOpenWindow) {
                    if (packet.guiId == "minecraft:chest" || packet.guiId == "minecraft:container")
                        serverOpenContainer = true
                } else
                    ChestAura.tileTarget = null
            }

            is C09PacketHeldItemChange -> {
                // Support for Singleplayer
                // (client packets get sent and received, duplicates would get cancelled, making slot changing impossible)
                if (event.eventType == EventState.RECEIVE) return

                if (packet.slotId == _serverSlot) event.cancelEvent()
                else _serverSlot = packet.slotId
            }

            is S09PacketHeldItemChange -> {
                if (_serverSlot == packet.heldItemHotbarIndex)
                    return

                val prevSlot = _serverSlot

                _serverSlot = packet.heldItemHotbarIndex

                if (NoSlotSet.handleEvents()) {
                    TickedActions.TickScheduler(NoSlotSet) += {
                        serverSlot = prevSlot
                    }

                    event.cancelEvent()
                }
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // Reset flags to prevent de-sync
        serverSlot = 0
        if (NoSlotSet.handleEvents()) _serverSlot = 0
        _serverOpenInventory = false
        serverOpenContainer = false
    }

    override fun handleEvents() = true
}
