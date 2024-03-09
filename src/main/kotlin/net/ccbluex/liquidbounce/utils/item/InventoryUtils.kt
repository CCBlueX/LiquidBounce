/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.item

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Type
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.yAxisMovement
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.util.Hand
import kotlin.math.abs

/**
 * Contains all container slots in inventory. (hotbar, offhand, inventory, armor)
 */
val ALL_SLOTS_IN_INVENTORY: List<ItemSlot> = run {
    val hotbarSlots = Hotbar.slots
    val offHandItem = listOf(OffHandSlot)
    val inventoryItems = (0 until 27).map { InventoryItemSlot(it) }
    val armorItems = (0 until 4).map { ArmorItemSlot(it) }

    return@run hotbarSlots + offHandItem + inventoryItems + armorItems
}



object Hotbar {

    /**
     * Contains all hotbar slots in inventory.
     */
    val slots = run {
        return@run (0 until 9).map { HotbarItemSlot(it) }
    }
    fun findClosestItem(items: Array<Item>): HotbarItemSlot? {
        return slots.filter { it.itemStack.item in items }
            .minByOrNull { abs(player.inventory.selectedSlot - it.hotbarSlotForServer) }
    }

    val slots = (0 until 9).map { HotbarItemSlot(it) }

    val items
        get() = (0..8).map { player.inventory.getStack(it).item }

    fun findBestItem(
        validator: (Int, ItemStack) -> Boolean,
        sort: (Int, ItemStack) -> Int = { slot, _ -> abs(player.inventory.selectedSlot - slot) }
    ) =
        slots
            .map {slot -> Pair (slot.hotbarSlotForServer, slot.itemStack) }
            .filter { (slot, itemStack) -> validator (slot, itemStack) }
            .maxByOrNull { (slot, itemStack) -> sort (slot, itemStack) }


    fun findBestItem(min: Int, sort: (Int, ItemStack) -> Int) =
        slots
            .map {slot -> Pair (slot.hotbarSlotForServer, slot.itemStack) }
            .maxByOrNull { (slot, itemStack) -> sort(slot, itemStack) }
            ?.takeIf {  (slot, itemStack) -> sort(slot, itemStack) >= min }

}

fun hasInventorySpace() = player.inventory.main.any { it.isEmpty }


fun findNonEmptySlotsInInventory(): List<ItemSlot> {
    return ALL_SLOTS_IN_INVENTORY.filter { !it.itemStack.isEmpty }
}


fun convertClientSlotToServerSlot(slot: Int, screen: GenericContainerScreen? = null): Int {
    if (screen == null) {
        return when (slot) {
            in 0..8 -> 36 + slot
            in 9..35 -> slot
            in 36..39 -> 39 - slot + 5
            40 -> 45
            else -> throw IllegalArgumentException("Invalid slot $slot")
        }
    } else {
        val stacks = screen.screenHandler.rows * 9

        return when (slot) {
            in 0..8 -> stacks + 27 + slot
            in 9..35 -> stacks + slot - 9
            else -> throw IllegalArgumentException("Invalid slot $slot")
        }
    }
}

/**
 * Sends an open inventory packet with the help of ViaFabricPlus. This is only for older versions.
 */

// https://github.com/ViaVersion/ViaFabricPlus/blob/ecd5d188187f2ebaaad8ded0ffe53538911f7898/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/MixinMinecraftClient.java#L124-L130
fun openInventorySilently() {
    if (InventoryTracker.isInventoryOpenServerSide) {
        return
    }

    runCatching {
        val isViaFabricPlusLoaded = FabricLoader.getInstance().isModLoaded("viafabricplus")

        if (!isViaFabricPlusLoaded) {
            return
        }

        val viaConnection = Via.getManager().connectionManager.connections.firstOrNull() ?: return

        if (viaConnection.protocolInfo.pipeline.contains(Protocol1_12To1_11_1::class.java)) {
            val clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, viaConnection)
            clientStatus.write(Type.VAR_INT, 2) // Open Inventory Achievement

            runCatching {
                clientStatus.scheduleSendToServer(Protocol1_12To1_11_1::class.java)
            }.onSuccess {
                InventoryTracker.isInventoryOpenServerSide = true
            }.onFailure {
                chat("§cFailed to open inventory using ViaFabricPlus, report to developers!")
                it.printStackTrace()
            }
        }
    }
}

inline fun runWithOpenedInventory(closeInventory: () -> Boolean = { true }) {
    val isInInventory = InventoryTracker.isInventoryOpenServerSide

    if (!isInInventory) {
        openInventorySilently()
    }

    val shouldClose = closeInventory()

    if (shouldClose) {
        mc.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(0))
    }
}

fun useHotbarSlotOrOffhand(item: HotbarItemSlot) {
    // We assume whatever called this function passed the isHotBar check
    when (item) {
        OffHandSlot -> {
            interactItem(Hand.OFF_HAND)
        }

        else -> {
            interactItem(Hand.MAIN_HAND) {
                SilentHotbar.selectSlotSilently(null, item.hotbarSlotForServer, 1)
            }
        }
    }
}

fun interactItem(hand: Hand, preInteraction: () -> Unit = { }) {
    val player = mc.player ?: return
    val interaction = mc.interactionManager ?: return

    preInteraction()

    interaction.interactItem(player, hand).let {
        if (it.shouldSwingHand()) {
            player.swingHand(hand)
        }
    }
}

fun findBlocksEndingWith(vararg targets: String) =
    Registries.BLOCK.filter { block -> targets.any { Registries.BLOCK.getId(block).path.endsWith(it.lowercase()) } }

/**
 * A list of blocks which may not be placed (apart from the usual checks), so inv cleaner and scaffold
 * won't count them as blocks
 */
var DISALLOWED_BLOCKS_TO_PLACE = hashSetOf(
    Blocks.TNT,
    Blocks.COBWEB,
    Blocks.NETHER_PORTAL,
)

/**
 * see [ModuleScaffold.isBlockUnfavourable]
 */
val UNFAVORABLE_BLOCKS_TO_PLACE = hashSetOf(
    Blocks.CRAFTING_TABLE,
    Blocks.JIGSAW,
    Blocks.SMITHING_TABLE,
    Blocks.FLETCHING_TABLE,
    Blocks.ENCHANTING_TABLE,
    Blocks.CAULDRON,
    Blocks.MAGMA_BLOCK,
)

/**
 * Configurable to configure the dynamic rotation engine
 */
class InventoryConstraintsConfigurable : Configurable("InventoryConstraints") {
    internal val startDelay by intRange("StartDelay", 1..2, 0..20, "ticks")
    internal val clickDelay by intRange("ClickDelay", 2..4, 0..20, "ticks")
    internal val closeDelay by intRange("CloseDelay", 1..2, 0..20, "ticks")
    internal val invOpen by boolean("InvOpen", false)
    internal val noMove by boolean("NoMove", false)
    internal val noRotation by boolean("NoRotation", false) // This should be visible only when NoMove is enabled

    val violatesNoMove
        get() = noMove && (mc.player?.moving == true || mc.player?.input?.yAxisMovement != 0f ||
            noRotation && !RotationManager.rotationMatchesPreviousRotation())
}

data class ItemStackWithSlot(val slot: Int, val itemStack: ItemStack)
