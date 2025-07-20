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
 */
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ArmorEvaluation
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.entity.EquipmentSlot
import net.minecraft.fluid.LavaFluid
import net.minecraft.fluid.WaterFluid
import net.minecraft.item.*
import java.util.function.Predicate

val PREFER_ITEMS_IN_HOTBAR: Comparator<ItemFacet> = compareByCondition(ItemFacet::isInHotbar)
val STABILIZE_COMPARISON: Comparator<ItemFacet> = Comparator.comparingInt {
    it.itemStack.hashCode()
}
val PREFER_BETTER_DURABILITY: Comparator<ItemFacet> = Comparator.comparingInt {
    it.itemStack.maxDamage - it.itemStack.damage
}

data class ItemCategory(val type: ItemType, val subtype: Int)

enum class ItemType(
    val oneIsSufficient: Boolean,
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
    val allocationPriority: Priority = Priority.NORMAL,
    /**
     * The user maybe wants to filter the items by a specific type. But the we don't need all versions of the item.
     * To stop the invcleaner from keeping items of every type, we can specify what function a specific item serves.
     * If that function is already served, we can just ignore it.
     */
    val providedFunction: ItemFunction? = null
) {
    ARMOR(true, allocationPriority = Priority.IMPORTANT_FOR_PLAYER_LIFE),
    SWORD(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_3, providedFunction = ItemFunction.WEAPON_LIKE),
    WEAPON(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_2, providedFunction = ItemFunction.WEAPON_LIKE),
    BOW(true),
    CROSSBOW(true),
    ARROW(true),
    TOOL(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    ROD(true),
    THROWABLE(false),
    SHIELD(true),
    FOOD(false),
    BUCKET(false),
    PEARL(false, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    GAPPLE(false, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    POTION(false),
    BLOCK(false),
    NONE(false),
}

enum class ItemFunction {
    WEAPON_LIKE,
    FOOD,
}

enum class ItemSortChoice(
    override val choiceName: String,
    val category: ItemCategory?,
    /**
     * This is the function that is used for the greedy check.
     *
     * IF IT WAS IMPLEMENTED
     */
    val satisfactionCheck: Predicate<ItemStack>? = null,
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
    THROWABLES("Throwables", ItemCategory(ItemType.THROWABLE, 0)),
    IGNORE("Ignore", null),
    NONE("None", null),
}

/**
 * @param expectedFullArmor what is the expected armor material when we have full armor (full iron, full dia, etc.)
 */
class ItemCategorization(
    availableItems: List<ItemSlot>,
) {
    companion object {
        @JvmStatic
        private fun constructArmorPiece(item: Item, id: Int): ArmorPiece {
            return ArmorPiece(VirtualItemSlot(ItemStack(item, 1), ItemSlotType.ARMOR, id))
        }

        /**
         * We expect to be full armor to be diamond armor.
         */
        @JvmStatic
        private val diamondArmorPieces = mapOf(
            EquipmentSlot.HEAD to constructArmorPiece(Items.DIAMOND_HELMET, 0),
            EquipmentSlot.CHEST to constructArmorPiece(Items.DIAMOND_CHESTPLATE, 1),
            EquipmentSlot.LEGS to constructArmorPiece(Items.DIAMOND_LEGGINGS, 2),
            EquipmentSlot.FEET to constructArmorPiece(Items.DIAMOND_BOOTS, 3),
        )
    }

    /**
     * Sometimes there are situations where armor pieces are not the best ones with the current armor, but become
     * the best ones as soon as we upgrade one of the other armor pieces.
     * In those cases we don't want to miss out on this armor piece in the future thus we keep it.
     */
    private val futureArmorToKeep: List<ItemSlot>
    private val armorComparator: ArmorComparator

    init {
        val findBestArmorPieces = ArmorEvaluation.findBestArmorPieces(slots = availableItems)

        this.armorComparator = ArmorEvaluation.getArmorComparatorFor(findBestArmorPieces)

        val armorParameterForSlot = ArmorKitParameters.getParametersForSlots(diamondArmorPieces)

        val armorComparatorForFullArmor = ArmorEvaluation.getArmorComparatorForParameters(armorParameterForSlot)

        this.futureArmorToKeep = ArmorEvaluation.findBestArmorPiecesWithComparator(
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
            // Treat animal armor as a normal item
            is AnimalArmorItem -> arrayOf(ItemFacet(slot))
            is ArmorItem -> arrayOf(ArmorItemFacet(slot, this.futureArmorToKeep, this.armorComparator))
            is SwordItem -> arrayOf(SwordItemFacet(slot))
            is BowItem -> arrayOf(BowItemFacet(slot))
            is CrossbowItem -> arrayOf(CrossbowItemFacet(slot))
            is ArrowItem -> arrayOf(ArrowItemFacet(slot))
            is MiningToolItem -> arrayOf(MiningToolItemFacet(slot))
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
            Items.MILK_BUCKET -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(ItemType.BUCKET, 2)))
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
            Items.SNOWBALL, Items.EGG, Items.WIND_CHARGE -> arrayOf(ThrowableItemFacet(slot))
            else -> {
                if (slot.itemStack.isFood) {
                    arrayOf(FoodItemFacet(slot))
                } else {
                    arrayOf(ItemFacet(slot))
                }
            }
        }

        // Everything could be a weapon (i.e. a stick with Knochback II should be considered a weapon)
        return specificItemFacets + WeaponItemFacet(slot)
    }
}
