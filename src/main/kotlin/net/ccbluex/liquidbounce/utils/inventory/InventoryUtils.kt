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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.getDestroySpeedWithEnchantment
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionResult
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.SortedSet
import java.util.function.BiPredicate
import java.util.function.ToDoubleFunction

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
    return net.ccbluex.liquidbounce.utils.entity.useItem(slot.useHand, yRot, xRot, swingMode)
}

internal fun findBlocksEndingWith(vararg targets: String): SortedSet<Block> =
    BuiltInRegistries.BLOCK.filterTo(blockSortedSetOf()) { block ->
        targets.any { BuiltInRegistries.BLOCK.getKey(block).path.endsWith(it.lowercase()) }
    }

val AbstractContainerMenu.typeOrNull: MenuType<*>?
    get() = try { type } catch (_: UnsupportedOperationException) { null }

/**
 * Finds the best slot in this iterable for mining [blockState] using `mc.player` as baseline.
 *
 * The result depends on current player context (e.g. creative state and durability filtering),
 * then ranks candidates by destroy speed and nearby-slot preference.
 */
fun <T : ItemSlot> Iterable<T>.findBestToolToMineBlock(
    blockState: BlockState,
    ignoreDurability: Boolean = true,
    predicate: BiPredicate<ItemStack, BlockState> = BiPredicate { _, _ -> true },
): T? {
    val player = mc.player ?: return null

    val candidates = filter {
        val stack = it.itemStack
        val durabilityCheck = (ignoreDurability || (stack.durability > 2 || stack.maxDamage <= 0))
        !player.isCreative && durabilityCheck && predicate.test(stack, blockState)
    }

    if (candidates.size > 1) {
        return candidates.maxWith(
            Comparator.comparingDouble<T>(ToDoubleFunction {
                it.itemStack.getDestroySpeedWithEnchantment(blockState).toDouble()
            }).thenDescending(ItemSlot.PREFER_NEARBY)
        )
    }

    return candidates.firstOrNull()
}
