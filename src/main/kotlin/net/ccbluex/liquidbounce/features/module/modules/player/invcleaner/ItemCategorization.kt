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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.*
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.fluid.LavaFluid
import net.minecraft.fluid.WaterFluid
import net.minecraft.item.*

val PREFER_ITEMS_IN_HOTBAR: (o1: WeightedItem, o2: WeightedItem) -> Int =
    { o1, o2 -> compareByCondition(o1, o2, WeightedItem::isInHotbar) }
val STABILIZE_COMPARISON: (o1: WeightedItem, o2: WeightedItem) -> Int =
    { o1, o2 -> o1.itemStack.hashCode().compareTo(o2.itemStack.hashCode()) }

data class ItemCategory(val type: ItemType, val subtype: Int)

enum class ItemType(val allowOnlyOne: Boolean) {
    ARMOR(true),
    SWORD(true),
    BOW(true),
    CROSSBOW(true),
    ARROW(true),
    TOOL(true),
    ROD(true),
    SHIELD(true),
    FOOD(false),
    BUCKET(false),
    PEARL(false),
    GAPPLE(false),
    BLOCK(false),
    NONE(false),
}

enum class ItemSortChoice(
    override val choiceName: String,
    val category: ItemCategory?,
    val satisfactionCheck: ((ItemStack) -> Boolean)? = null,
) : NamedChoice {
    SWORD("Sword", ItemCategory(ItemType.SWORD, 0)),
    BOW("Bow", ItemCategory(ItemType.BOW, 0)),
    CROSSBOW("Crossbow", ItemCategory(ItemType.CROSSBOW, 0)),
    AXE("Axe", ItemCategory(ItemType.TOOL, 0)),
    PICKAXE("Pickaxe", ItemCategory(ItemType.TOOL, 1)),
    ROD("Rod", ItemCategory(ItemType.ROD, 0)),
    SHIELD("Shield", ItemCategory(ItemType.SHIELD, 0)),
    WATER("Water", ItemCategory(ItemType.BUCKET, 0)),
    LAVA("Lava", ItemCategory(ItemType.BUCKET, 1)),
    MILK("Milk", ItemCategory(ItemType.BUCKET, 2)),
    PEARL("Pearl", ItemCategory(ItemType.PEARL, 0), { it.item == Items.ENDER_PEARL }),
    GAPPLE(
        "Gapple",
        ItemCategory(ItemType.GAPPLE, 0),
        { it.item == Items.GOLDEN_APPLE || it.item == Items.ENCHANTED_GOLDEN_APPLE },
    ),
    FOOD("Food", ItemCategory(ItemType.FOOD, 0), { it.item.foodComponent != null }),
    BLOCK("Block", ItemCategory(ItemType.BLOCK, 0), { it.item is BlockItem }),
    IGNORE("Ignore", null),
    NONE("None", null),
}

object ItemCategorization {
    /**
     * Returns a list of facets an item represents. For example an axe is an axe, but also a sword:
     * - (SANDSTONE_BLOCK, 64) => `[Block(SANDSTONE_BLOCK, 64)]`
     * - (DIAMOND_AXE, 1) => `[Axe(DIAMOND_AXE, 1), Tool(DIAMOND_AXE, 1)]`
     */
    @Suppress("CyclomaticComplexMethod")
    fun getItemFacets(slot: ItemSlot): Array<WeightedItem> {
        if (slot.itemStack.isNothing()) {
            return emptyArray()
        }

        return when (val item = slot.itemStack.item) {
            is ArmorItem -> arrayOf(WeightedArmorItem(slot))
            is SwordItem -> arrayOf(WeightedSwordItem(slot))
            is BowItem -> arrayOf(WeightedBowItem(slot))
            is CrossbowItem -> arrayOf(WeightedCrossbowItem(slot))
            is ArrowItem -> arrayOf(WeightedArrowItem(slot))
            is ToolItem -> {
                arrayOf(
                    // todo: add weapon type
                    // WeightedSwordItem(slot),
                    WeightedToolItem(slot),
                )
            }
            is FishingRodItem -> arrayOf(WeightedRodItem(slot))
            is ShieldItem -> arrayOf(WeightedShieldItem(slot))
            is BlockItem -> arrayOf(WeightedBlockItem(slot))
            is MilkBucketItem -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 2)))
            is BucketItem -> {
                when (item.fluid) {
                    is WaterFluid -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 0)))
                    is LavaFluid -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 1)))
                    else -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.BUCKET, 3)))
                }
            }
            is EnderPearlItem -> arrayOf(WeightedPrimitiveItem(slot, ItemCategory(ItemType.PEARL, 0)))
            Items.GOLDEN_APPLE -> {
                arrayOf(
                    WeightedFoodItem(slot),
                    WeightedPrimitiveItem(slot, ItemCategory(ItemType.GAPPLE, 0)),
                )
            }
            Items.ENCHANTED_GOLDEN_APPLE -> {
                arrayOf(
                    WeightedFoodItem(slot),
                    WeightedPrimitiveItem(slot, ItemCategory(ItemType.GAPPLE, 0), 1),
                )
            }
            else -> {
                if (slot.itemStack.isFood) {
                    arrayOf(WeightedFoodItem(slot))
                } else {
                    arrayOf(WeightedItem(slot))
                }
            }
        }
    }
}
