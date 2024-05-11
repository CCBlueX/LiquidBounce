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
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ArmorEvaluation
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.sorting.compareValueByCondition
import net.minecraft.entity.EquipmentSlot
import net.minecraft.fluid.LavaFluid
import net.minecraft.fluid.WaterFluid
import net.minecraft.item.*

val PREFER_ITEMS_IN_HOTBAR: (o1: ItemFacet, o2: ItemFacet) -> Int =
    { o1, o2 -> compareValueByCondition(o1, o2, ItemFacet::isInHotbar) }
val STABILIZE_COMPARISON: (o1: ItemFacet, o2: ItemFacet) -> Int =
    { o1, o2 -> o1.itemStack.hashCode().compareTo(o2.itemStack.hashCode()) }

data class ItemCategory(val type: ItemType, val subtype: Int)

enum class ItemType(
    val allowOnlyOne: Boolean,
    /**
     * Higher priority means the item category is filled in first.
     *
     * This is important for example for specializations. If we have a weapon slot and an axe slot, an axe would
     * fit in both slots, but because the player specifically requested an axe, the best axe should be filled in first
     * with the best available axe.
     *
     * ## Used values
     * - Specialization (see above): 10 per level
     */
    val allocationPriority: Int = 0,
) {
    ARMOR(true, allocationPriority = 20),
    SWORD(true, allocationPriority = 10),
    WEAPON(true, allocationPriority = -1),
    BOW(true),
    CROSSBOW(true),
    ARROW(true),
    TOOL(true, allocationPriority = 10),
    ROD(true),
    SHIELD(true),
    FOOD(false),
    BUCKET(false),
    PEARL(false),
    GAPPLE(false),
    POTION(false),
    BLOCK(false),
    NONE(false),
}

enum class ItemSortChoice(
    override val choiceName: String,
    val category: ItemCategory?,
    /**
     * This is the function that is used for the greedy check.
     *
     * IF IT WAS IMPLEMENTED
     */
    val satisfactionCheck: ((ItemStack) -> Boolean)? = null,
) : NamedChoice {
    SWORD("Sword", ItemCategory(ItemType.SWORD, 0)),
    WEAPON("Weapon", ItemCategory(ItemType.WEAPON, 0)),
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
    FOOD("Food", ItemCategory(ItemType.FOOD, 0), { it.foodComponent != null }),
    POTION("Potion", ItemCategory(ItemType.POTION, 0)),
    BLOCK("Block", ItemCategory(ItemType.BLOCK, 0), { it.item is BlockItem }),
    IGNORE("Ignore", null),
    NONE("None", null),
}

/**
 * @param expectedFullArmor what is the expected armor material when we have full armor (full iron, full dia, etc.)
 */
class ItemCategorization(
    availableItems: List<ItemSlot>,
    expectedFullArmor: ArmorMaterial = ArmorMaterials.DIAMOND.value()
) {
    private val bestPiecesIfFullArmor: List<ItemSlot>
    private val armorComparator: ArmorComparator

    init {
        val findBestArmorPieces = ArmorEvaluation.findBestArmorPieces(slots = availableItems)

        this.armorComparator = ArmorEvaluation.getArmorComparatorFor(findBestArmorPieces)

        val fullProtection = ArmorItem.Type.entries.sumOf { expectedFullArmor.getProtection(it) }
        val fullToughness = ArmorItem.Type.entries.size * expectedFullArmor.toughness

        val armorSlots = arrayOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

        val armorParameterForSlot = armorSlots.zip(ArmorItem.Type.entries).associate { (slotType, armorType) ->
            val armorParameter = ArmorParameter(
                defensePoints = (fullProtection - expectedFullArmor.getProtection(armorType)).toFloat(),
                toughness = fullToughness,
            )

            slotType to armorParameter
        }

        val armorComparatorForFullArmor = ArmorEvaluation.getArmorComparatorForParameters(armorParameterForSlot)

        this.bestPiecesIfFullArmor = ArmorEvaluation.findBestArmorPiecesWithComparator(
            availableItems,
            armorComparatorForFullArmor
        ).values.mapNotNull { it?.itemSlot }
    }

    /**
     * Returns a list of facets an item represents. For example an axe is an axe, but also a sword:
     * - (SANDSTONE_BLOCK, 64) => `[Block(SANDSTONE_BLOCK, 64)]`
     * - (DIAMOND_AXE, 1) => `[Axe(DIAMOND_AXE, 1), Tool(DIAMOND_AXE, 1)]`
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun getItemFacets(slot: ItemSlot): Array<ItemFacet> {
        if (slot.itemStack.isNothing()) {
            return emptyArray()
        }

        val specificItemFacets: Array<ItemFacet> = when (val item = slot.itemStack.item) {
            is ArmorItem -> arrayOf(ArmorItemFacet(slot, this.bestPiecesIfFullArmor, this.armorComparator))
            is SwordItem -> arrayOf(SwordItemFacet(slot))
            is BowItem -> arrayOf(BowItemFacet(slot))
            is CrossbowItem -> arrayOf(CrossbowItemFacet(slot))
            is ArrowItem -> arrayOf(ArrowItemFacet(slot))
            is ToolItem -> arrayOf(ToolItemFacet(slot))
            is FishingRodItem -> arrayOf(RodItemFacet(slot))
            is ShieldItem -> arrayOf(ShieldItemFacet(slot))
            is BlockItem -> {
                if (ScaffoldBlockItemSelection.isValidBlock(slot.itemStack)
                    && !ScaffoldBlockItemSelection.isBlockUnfavourable(slot.itemStack)
                ) {
                    arrayOf(BlockItemFacet(slot))
                } else {
                    arrayOf(ItemFacet(slot))
                }
            }
            is MilkBucketItem -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(ItemType.BUCKET, 2)))
            is BucketItem -> {
                when (item.fluid) {
                    is WaterFluid -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(ItemType.BUCKET, 0)))
                    is LavaFluid -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(ItemType.BUCKET, 1)))
                    else -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(ItemType.BUCKET, 3)))
                }
            }
            is PotionItem -> {
                val areAllEffectsGood =
                    slot.itemStack.getPotionEffects()
                        .all { it.effectType in PotionItemFacet.GOOD_STATUS_EFFECTS }

                if (areAllEffectsGood) {
                    arrayOf(PotionItemFacet(slot))
                } else {
                    arrayOf(ItemFacet(slot))
                }
            }
            is EnderPearlItem -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(ItemType.PEARL, 0)))
            Items.GOLDEN_APPLE -> {
                arrayOf(
                    FoodItemFacet(slot),
                    PrimitiveItemFacet(slot, ItemCategory(ItemType.GAPPLE, 0)),
                )
            }
            Items.ENCHANTED_GOLDEN_APPLE -> {
                arrayOf(
                    FoodItemFacet(slot),
                    PrimitiveItemFacet(slot, ItemCategory(ItemType.GAPPLE, 0), 1),
                )
            }
            else -> {
                if (slot.itemStack.isFood) {
                    arrayOf(FoodItemFacet(slot))
                } else {
                    arrayOf(ItemFacet(slot))
                }
            }
        }

        // Everything could be a weapon (i.e. a stick with Knochback II should be considered a weapon)
        return specificItemFacets + arrayOf(WeaponItemFacet(slot))
    }
}
