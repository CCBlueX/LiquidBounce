/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
@file:Suppress("TooManyFunctions", "WildcardImport")

package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.useItem
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.input.shouldSwingHand
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.Block
import java.util.SortedSet

fun hasInventorySpace() = player.inventory.nonEquipmentItems.any { it.isEmpty }

fun findEmptyStorageSlotsInInventory(): List<ItemSlot> {
    return (Slots.Inventory + Slots.Hotbar).filter { it.itemStack.isEmpty }
}

fun findNonEmptyStorageSlotsInInventory(): List<ItemSlot> {
    return (Slots.Inventory + Slots.Hotbar).filter { !it.itemStack.isEmpty }
}

fun findNonEmptySlotsInInventory(): List<ItemSlot> {
    return Slots.All.filter { !it.itemStack.isEmpty }
}

fun AbstractContainerScreen<*>.getSlotsInContainer(): List<ContainerItemSlot> =
    this.menu.slots
        .filter { it.container !== player.inventory }
        .map { ContainerItemSlot(it.index) }

fun AbstractContainerScreen<*>.findItemsInContainer(): List<ContainerItemSlot> =
    this.menu.slots
        .filter { !it.item.isEmpty && it.container !== player.inventory }
        .map { ContainerItemSlot(it.index) }

@JvmOverloads
context(requester: EventListener)
fun useHotbarSlotOrOffhand(
    slot: HotbarItemSlot,
    ticksUntilReset: Int = 1,
    yRot: Float = RotationManager.currentRotation?.yRot ?: player.yRot,
    xRot: Float = RotationManager.currentRotation?.xRot ?: player.xRot,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): InteractionResult {
    SilentHotbar.selectSlotSilently(requester, slot, ticksUntilReset)
    return useItem(slot.useHand, yRot, xRot, swingMode)
}

@JvmOverloads
fun useItem(
    hand: InteractionHand,
    yRot: Float = RotationManager.currentRotation?.yRot ?: player.yRot,
    xRot: Float = RotationManager.currentRotation?.xRot ?: player.xRot,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): InteractionResult {
    val result = interaction.useItem(player, hand, yRot, xRot)

    if (result.consumesAction()) {
        if (result.shouldSwingHand()) {
            swingMode.accept(hand)
        }

        mc.gameRenderer.itemInHandRenderer.itemUsed(hand)
    }

    return result
}

internal fun findBlocksEndingWith(vararg targets: String): SortedSet<Block> =
    BuiltInRegistries.BLOCK.filterTo(blockSortedSetOf()) { block ->
        targets.any { BuiltInRegistries.BLOCK.getKey(block).path.endsWith(it.lowercase()) }
    }

val AbstractContainerMenu.typeOrNull: MenuType<*>?
    get() = try { type } catch (_: UnsupportedOperationException) { null }
