/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.input.shouldSwingHand
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.network.OpenInventorySilentlyPacket
import net.ccbluex.liquidbounce.utils.network.sendPacket
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.Hand

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
     * If the protocol contains [com.viaversion.viabackwards.protocol.v1_12to1_11_1.Protocol1_12To1_11_1]
     * and the client status packet is supported,
     * the inventory will be opened silently using [openInventorySilently].
     * Otherwise, the inventory will not have any open tracking and
     * the server will only know when clicking in the inventory.
     *
     * Closing will still be required to be done for any version.
     * Sad.
     * :(
     */
    private val requiresOpenInventory by boolean("RequiresInventoryOpen", false)

    override fun passesRequirements(action: InventoryAction) =
        super.passesRequirements(action) &&
            (!action.requiresPlayerInventoryOpen() || !requiresOpenInventory ||
                InventoryManager.isInventoryOpen)

}

fun hasInventorySpace() = player.inventory.main.any { it.isEmpty }

fun findEmptyStorageSlotsInInventory(): List<ItemSlot> {
    return (Slots.Inventory + Slots.Hotbar).filter { it.itemStack.isEmpty }
}

fun findNonEmptyStorageSlotsInInventory(): List<ItemSlot> {
    return (Slots.Inventory + Slots.Hotbar).filter { !it.itemStack.isEmpty }
}

fun findNonEmptySlotsInInventory(): List<ItemSlot> {
    return Slots.All.filter { !it.itemStack.isEmpty }
}

/**
 * Sends an open inventory packet with the help of ViaFabricPlus. This is only for older versions.
 */
fun openInventorySilently() {
    if (InventoryManager.isInventoryOpenServerSide || !usesViaFabricPlus) {
        return
    }

    network.sendPacket(
        OpenInventorySilentlyPacket(),
        onSuccess = { InventoryManager.isInventoryOpenServerSide = true },
        onFailure = { chat(markAsError("Failed to open inventory using ViaFabricPlus, report to developers!")) }
    )
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

fun useHotbarSlotOrOffhand(
    item: HotbarItemSlot,
    ticksUntilReset: Int = 1,
    yaw: Float = RotationManager.currentRotation?.yaw ?: player.yaw,
    pitch: Float = RotationManager.currentRotation?.yaw ?: player.pitch,
) = when (item) {
    OffHandSlot -> interactItem(Hand.OFF_HAND, yaw, pitch)
    else -> interactItem(Hand.MAIN_HAND, yaw, pitch) {
        SilentHotbar.selectSlotSilently(null, item.hotbarSlotForServer, ticksUntilReset)
    }
}

inline fun interactItem(
    hand: Hand,
    yaw: Float = RotationManager.currentRotation?.yaw ?: player.yaw,
    pitch: Float = RotationManager.currentRotation?.yaw ?: player.pitch,
    preInteraction: () -> Unit = { }
) {
    preInteraction()

    interaction.interactItem(player, hand, yaw, pitch).takeIf { it.isAccepted }?.let {
        if (it.shouldSwingHand()) {
            player.swingHand(hand)
        }

        mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand)
    }
}

fun findBlocksEndingWith(vararg targets: String) =
    Registries.BLOCK.filter { block -> targets.any { Registries.BLOCK.getId(block).path.endsWith(it.lowercase()) } }

/**
 * Get the color of the armor on the player
 */
fun getArmorColor() = Slots.Armor.firstNotNullOfOrNull { slot ->
    val itemStack = slot.itemStack
    val color = itemStack.getArmorColor() ?: return@firstNotNullOfOrNull null

    Pair(slot, color)
}

/**
 * Get the color of the armor on the item stack
 *
 * @see [net.minecraft.client.render.entity.feature.ArmorFeatureRenderer.renderArmor]
 */
fun ItemStack.getArmorColor(): Int? {
    return if (isIn(ItemTags.DYEABLE)) {
        DyedColorComponent.getColor(this, -6265536) // #FFA06540
    } else {
        null
    }
}

/**
 * A list of blocks which may not be placed (apart from the usual checks), so inv cleaner and scaffold
 * won't count them as blocks
 */
val DISALLOWED_BLOCKS_TO_PLACE = hashSetOf(
    Blocks.TNT,
    Blocks.COBWEB,
    Blocks.NETHER_PORTAL,
)

/**
 * @see [ModuleScaffold.isBlockUnfavourable]
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
