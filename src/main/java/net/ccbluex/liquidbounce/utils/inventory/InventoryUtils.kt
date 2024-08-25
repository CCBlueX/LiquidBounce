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
import net.minecraft.block.Blocks
import net.minecraft.block.DeadBushBlock
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.network.packet.s2c.play.HeldItemChangeS2CPacket

object InventoryUtils : MinecraftInstance(), Listenable {

    // What slot is selected on server-side?
    // TODO: Is this equal to mc.interactionManager.currentPlayerItem?
    var serverSlot
        get() = _serverSlot
        set(value) {
            if (value != _serverSlot) {
                sendPacket(UpdateSelectedSlotC2SPacket(value))

                _serverSlot = value
            }
        }

    // Is inventory open on server-side?
    var serverOpenInventory
        get() = _serverOpenInventory
        set(value) {
            if (value != _serverOpenInventory) {
                sendPacket(
                    if (value) ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.OPEN_INVENTORY_ACHIEVEMENT)
                    else CloseWindowC2SPacket(mc.player?.playerScreenHandler?.syncId ?: 0)
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

    val CLICK_TIMER = MSTimer()

    val BLOCK_BLACKLIST = listOf(
        Blocks.CHEST,
        Blocks.ENDERCHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.ANVIL,
        Blocks.SAND,
        Blocks.COBWEB,
        Blocks.TORCH,
        Blocks.CRAFTING_TABLE,
        Blocks.FURNACE,
        Blocks.LILY_PAD,
        Blocks.DISPENSER,
        Blocks.STONE_PRESSURE_PLATE,
        Blocks.WOODEN_PRESSURE_PLATE,
        Blocks.NOTEBLOCK,
        Blocks.DROPPER,
        Blocks.TNT,
        Blocks.STANDING_BANNER,
        Blocks.WALL_BANNER,
        Blocks.REDSTONE_TORCH,
        Blocks.LADDER
    )

    fun findItemArray(startInclusive: Int, endInclusive: Int, items: Array<Item>): Int? {
        for (i in startInclusive..endInclusive)
            if (mc.player.playerScreenHandler.getSlot(i).stack?.item in items)
                return i

        return null
    }

    fun findItem(startInclusive: Int, endInclusive: Int, item: Item): Int? {
        for (i in startInclusive..endInclusive)
            if (mc.player.playerScreenHandler.getSlot(i).stack?.item == item)
                return i

        return null
    }

    fun hasSpaceInHotbar(): Boolean {
        for (i in 36..44)
            mc.player.playerScreenHandler.getSlot(i).stack ?: return true

        return false
    }

    fun hasSpaceInInventory() = mc.player?.inventory?.firstEmptyStack != -1

    fun countSpaceInInventory() = mc.player.inventory.main.count { it.isEmpty() }

    fun findBlockInHotbar(): Int? {
        val player = mc.player ?: return null
        val inventory = player.playerScreenHandler

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is BlockItem) (stack.item as BlockItem).block else return@filter false

            stack.item is BlockItem && stack.count > 0 && block !in BLOCK_BLACKLIST && block !is DeadBushBlock
        }.minByOrNull { (inventory.getSlot(it).stack.item as BlockItem).block.isFullCube }
    }

    fun findLargestBlockStackInHotbar(): Int? {
        val player = mc.player ?: return null
        val inventory = player.playerScreenHandler

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is BlockItem) (stack.item as BlockItem).block else return@filter false

            stack.item is BlockItem && stack.count > 0 && block.isFullCube && block !in BLOCK_BLACKLIST && block !is DeadBushBlock
        }.maxByOrNull { inventory.getSlot(it).stack.count }
    }

    fun findBlockStackInHotbarGreaterThan(amount:Int): Int? {
        val player = mc.player ?: return null
        val inventory = player.playerScreenHandler

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is BlockItem) (stack.item as BlockItem).block else return@filter false

            stack.item is BlockItem && stack.count > amount && block.isFullCube && block !in BLOCK_BLACKLIST && block !is DeadBushBlock
        }.minByOrNull { (inventory.getSlot(it).stack.item as BlockItem).block.isFullCube }
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
            is C08PacketPlayerBlockPlacement, is ClickWindowC2SPacket -> {
                CLICK_TIMER.reset()

                if (packet is ClickWindowC2SPacket)
                    isFirstInventoryClick = false
            }

            is ClientStatusC2SPacket ->
                if (packet.mode == ClientStatusC2SPacket.Mode.OPEN_INVENTORY_ACHIEVEMENT) {
                    if (_serverOpenInventory) event.cancelEvent()
                    else {
                        isFirstInventoryClick = true
                        _serverOpenInventory = true
                    }
                }

            is CloseWindowC2SPacket, is CloseWindowS2CPacket, is OpenWindowS2CPacket -> {
                isFirstInventoryClick = false
                _serverOpenInventory = false
                serverOpenContainer = false

                if (packet is OpenWindowS2CPacket) {
                    if (packet.guiId == "minecraft:chest" || packet.guiId == "minecraft:container")
                        serverOpenContainer = true
                } else
                    ChestAura.tileTarget = null
            }

            is UpdateSelectedSlotC2SPacket -> {
                // Support for Singleplayer
                // (client packets get sent and received, duplicates would get cancelled, making slot changing impossible)
                if (event.eventType == EventState.RECEIVE) return

                if (packet.selectedSlot == _serverSlot) event.cancelEvent()
                else _serverSlot = packet.selectedSlot
            }

            is HeldItemChangeS2CPacket -> {
                if (_serverSlot == packet.slot)
                    return

                val prevSlot = _serverSlot

                _serverSlot = packet.slot

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
