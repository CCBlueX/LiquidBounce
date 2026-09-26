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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Represents an item purely by its Knockback enchantment level, regardless of what kind of item it
 * is (a stick with Knockback II, a sword with Knockback, etc.).
 *
 * This lets the inventory cleaner keep the single best Knockback item — useful for knocking players
 * into the void — while still throwing away worse Knockback items. It deliberately uses a low
 * allocation priority (see [ItemType.KNOCKBACK]) so that a real weapon (e.g. a Knockback sword) is
 * still placed first as the player's main weapon, and a separate, lesser Knockback item (e.g. a
 * Knockback stick) is kept and sorted into its own slot rather than being discarded.
 */
class KnockbackItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<KnockbackItemFacet>(
                Comparator.comparingInt { it.knockbackLevel },
                PREFER_BETTER_DURABILITY,
                PREFER_ENCHANTABLE,
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    val knockbackLevel: Int
        get() = itemStack.getEnchantment(Enchantments.KNOCKBACK)

    override val category: ItemCategory
        get() = ItemType.KNOCKBACK.defaultCategory

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as KnockbackItemFacet)
    }
}
