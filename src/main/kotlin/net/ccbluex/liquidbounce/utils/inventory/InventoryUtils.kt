/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.utils.inventory

//import com.viaversion.viaversion.api.Via
//import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
//import com.viaversion.viaversion.api.type.Type
//import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1
//import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.item.isNothing
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
 * Constraints for inventory actions.
 * This can be used to ensure that the player is not moving or rotating while interacting with the inventory.
 * Also allows to set delays for opening, clicking and closing the inventory.
 */
open class InventoryConstraints : Configurable("Constraints") {

    internal val startDelay by intRange("StartDelay", 1..2, 0..20, "ticks")
    internal val clickDelay by intRange("ClickDelay", 2..4, 0..20, "ticks")
    internal val closeDelay by intRange("CloseDelay", 1..2, 0..20, "ticks")
    internal val missChance by intRange("MissChance", 0..0, 0..100, "%")

    private val requiresNoMovement by boolean("RequiresNoMovement", false)
    private val requiresNoRotation by boolean("RequiresNoRotation", false)

    /**
     * Whether the constraints are met, this will be checked before any inventory actions are performed.
     * This can be overridden by [PlayerInventoryConstraints] which introduces additional requirements.
     */
    open fun passesRequirements(action: InventoryAction) =
        (!requiresNoMovement || player.input.movementForward == 0.0f && player.input.movementSideways == 0.0f) &&
            (!requiresNoRotation || RotationManager.rotationMatchesPreviousRotation())

}

/**
 * Additional constraints for the player inventory. This should be used when interacting with the player inventory
 * instead of a generic container.
 */
class PlayerInventoryConstraints : InventoryConstraints() {

    /**
     * When this option is not enabled, the inventory will be opened silently
     * depending on the Minecraft version chosen using ViaFabricPlus.
     *
     * If the protocol contains [Protocol1_12To1_11_1] and the client status packet is supported,
     * the inventory will be opened silently using [openInventorySilently].
     * Otherwise, the inventory will not have any open tracking and
     * the server will only know when clicking in the inventory.
     *
     * Closing will still be required to be done for any version. Sad. :(
     */
    private val requiresOpenInvenotory by boolean("RequiresInventoryOpen", false)

    override fun passesRequirements(action: InventoryAction) =
        super.passesRequirements(action) &&
            (!action.requiresPlayerInventoryOpen() || !requiresOpenInvenotory ||
                InventoryManager.isInventoryOpenServerSide)

}

val HOTBAR_SLOTS = (0 until 9).map { HotbarItemSlot(it) }
val INVENTORY_SLOTS: List<ItemSlot> =
    (0 until 27).map { InventoryItemSlot(it) }
val OFFHAND_SLOT = OffHandSlot
val ARMOR_SLOTS = (0 until 4).map { ArmorItemSlot(it) }

/**
 * Contains all container slots in inventory. (hotbar, offhand, inventory, armor)
 */
val ALL_SLOTS_IN_INVENTORY: List<ItemSlot> =
    HOTBAR_SLOTS + OFFHAND_SLOT + INVENTORY_SLOTS + ARMOR_SLOTS

object Hotbar {

    fun findClosestItem(items: Array<Item>): HotbarItemSlot? {
        return HOTBAR_SLOTS.filter { it.itemStack.item in items }
            .minByOrNull { abs(player.inventory.selectedSlot - it.hotbarSlotForServer) }
    }

    val items
        get() = (0..8).map { player.inventory.getStack(it).item }

    fun findBestItem(min: Int, sort: (Int, ItemStack) -> Int) =
        HOTBAR_SLOTS
            .map {slot -> Pair (slot.hotbarSlotForServer, slot.itemStack) }
            .maxByOrNull { (slot, itemStack) -> sort(slot, itemStack) }
            ?.takeIf {  (slot, itemStack) -> sort(slot, itemStack) >= min }

}

fun hasInventorySpace() = player.inventory.main.any { it.isEmpty }

fun findEmptyStorageSlotsInInventory(): List<ItemSlot> {
    return (INVENTORY_SLOTS + HOTBAR_SLOTS).filter { it.itemStack.isEmpty }
}

fun findNonEmptySlotsInInventory(): List<ItemSlot> {
    return ALL_SLOTS_IN_INVENTORY.filter { !it.itemStack.isEmpty }
}

/**
 * Sends an open inventory packet with the help of ViaFabricPlus. This is only for older versions.
 */

// https://github.com/ViaVersion/ViaFabricPlus/blob/ecd5d188187f2ebaaad8ded0ffe53538911f7898/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/MixinMinecraftClient.java#L124-L130
fun openInventorySilently() {
    if (InventoryManager.isInventoryOpenServerSide) {
        return
    }

    // TODO: Fix this when ViaFabricPlus is updated
//    runCatching {
//        val isViaFabricPlusLoaded = FabricLoader.getInstance().isModLoaded("viafabricplus")
//
//        if (!isViaFabricPlusLoaded) {
//            return
//        }
//
//        val viaConnection = Via.getManager().connectionManager.connections.firstOrNull() ?: return
//
//        if (viaConnection.protocolInfo.pipeline.contains(Protocol1_12To1_11_1::class.java)) {
//            val clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, viaConnection)
//            clientStatus.write(Type.VAR_INT, 2) // Open Inventory Achievement
//
//            runCatching {
//                clientStatus.scheduleSendToServer(Protocol1_12To1_11_1::class.java)
//            }.onSuccess {
//                InventoryManager.isInventoryOpenServerSide = true
//            }.onFailure {
//                chat("§cFailed to open inventory using ViaFabricPlus, report to developers!")
//                it.printStackTrace()
//            }
//        }
//    }
}

fun closeInventorySilently() {
    network.sendPacket(CloseHandledScreenC2SPacket(0))
}

fun getSlotsInContainer(screen: GenericContainerScreen) =
    screen.screenHandler.slots
        .filter { it.inventory === screen.screenHandler.inventory }
        .map { ContainerItemSlot(it.id) }

fun findItemsInContainer(screen: GenericContainerScreen) =
    screen.screenHandler.slots
        .filter { !it.stack.isNothing() && it.inventory === screen.screenHandler.inventory }
        .map { ContainerItemSlot(it.id) }

fun useHotbarSlotOrOffhand(item: HotbarItemSlot) = when (item) {
    OffHandSlot -> interactItem(Hand.OFF_HAND)
    else -> interactItem(Hand.MAIN_HAND) {
        SilentHotbar.selectSlotSilently(null, item.hotbarSlotForServer, 1)
    }
}

fun interactItem(hand: Hand, preInteraction: () -> Unit = { }) {
    preInteraction()

    interaction.interactItem(player, hand).takeIf { it.isAccepted }?.let {
        if (it.shouldSwingHand()) {
            player.swingHand(hand)
        }

        mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand)
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
