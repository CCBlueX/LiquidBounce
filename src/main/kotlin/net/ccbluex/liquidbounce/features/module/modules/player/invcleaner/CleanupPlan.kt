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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.core.Holder
import net.minecraft.core.TypedInstance
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ItemLike

@JvmRecord
data class InventorySwap(val from: ItemSlot, val to: ItemSlot, val priority: Priority)

/**
 * Represents the "id" of [ItemStack].
 * [ItemStack]s with same [Item] and [DataComponentPatch] can be merged.
 */
@JvmRecord
data class ItemAndComponents @JvmOverloads constructor(
    val item: Item,
    val componentsPatch: DataComponentPatch = DataComponentPatch.EMPTY,
) : TypedInstance<Item> {
    constructor(itemStack: ItemStack) : this(itemStack.item, itemStack.componentsPatch)

    override fun typeHolder(): Holder<Item> = BuiltInRegistries.ITEM.wrapAsHolder(this.item)

    fun toItemStack(count: Int): ItemStack {
        return ItemStack(this.typeHolder(), count, componentsPatch)
    }
}

class InventoryCleanupPlan(
    val usefulItems: Set<ItemSlot>,
    val swaps: List<InventorySwap>,
    val mergeableItems: Map<ItemAndComponents, List<ItemSlot>>,
) {

    fun findItemsToThrowOut(
        itemSlots: List<ItemSlot>,
    ) = itemSlots.filter { it !in usefulItems }

}
