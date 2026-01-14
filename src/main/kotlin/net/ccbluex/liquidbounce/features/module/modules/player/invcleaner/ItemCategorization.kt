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

import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ArmorEvaluation
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot.ItemSlotType
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.*

val PREFER_ITEMS_IN_HOTBAR: Comparator<ItemFacet> = compareByCondition(ItemFacet::isInHotbar)
val STABILIZE_COMPARISON: Comparator<ItemFacet> = Comparator.comparingInt {
    it.itemStack.hashCode()
}

val PREFER_BETTER_DURABILITY: Comparator<ItemFacet> = Comparator.comparingInt {
    it.itemStack.maxDamage - it.itemStack.damage
}

val DEFAULT_TIE_BREAK: Array<Comparator<ItemFacet>> = arrayOf(
    PREFER_ITEMS_IN_HOTBAR,
    STABILIZE_COMPARISON,
)

data class ItemCategory(val type: GenericItemType, val subtype: Any = Unit)

enum class GenericItemType(
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
    val allocationPriority: Priority = Priority.NORMAL
) {
    ARMOR(true, allocationPriority = Priority.IMPORTANT_FOR_PLAYER_LIFE),
    SWORD(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_3),
    WEAPON(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_2),
    BOW(true),
    CROSSBOW(true),
    ARROW(true),
    TOOL(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    THROWABLE(false),
    FOOD(false),
    POTION(false),
    BLOCK(false),
    /**
     * Represents any item. Every item in the inventory has this type.
     */
    ANY_ITEM(true),
}

enum class ItemFunction {
    WEAPON_LIKE,

    /**
     * Crossbows and bows.
     */
    BOW_LIKE,
    FOOD,
}

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
     * Sometimes there are situations where armor pieces aren’t the best ones with the current armor, but become
     * the best ones as soon as we upgrade one of the other armor pieces.
     * In those cases, we don't want to miss out on this armor piece in the future, thus we keep it.
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

        val item = slot.itemStack.item

        val specificItemFacets: Array<ItemFacet> = when (item) {
            // Treat animal armor as a normal item
            is ArmorItem -> arrayOf(ArmorItemFacet(slot, this.futureArmorToKeep, this.armorComparator))
            is SwordItem -> arrayOf(SwordItemFacet(slot))
            is BowItem -> arrayOf(BowItemFacet(slot))
            is CrossbowItem -> arrayOf(CrossbowItemFacet(slot))
            is ArrowItem -> arrayOf(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.ARROW)))
            is MiningToolItem -> arrayOf(MiningToolItemFacet(slot))
            is BlockItem -> {
                val isUsableBlock = (ScaffoldBlockItemSelection.isValidBlock(slot.itemStack)
                    && !ScaffoldBlockItemSelection.isBlockUnfavourable(slot.itemStack))

                if (isUsableBlock) {
                    arrayOf(BlockItemFacet(slot))
                } else {
                    emptyArray()
                }
            }
            is PotionItem -> {
                val areAllEffectsGood =
                    slot.itemStack.getPotionEffects()
                        .all { it.effectType in PotionItemFacet.GOOD_STATUS_EFFECTS }

                if (areAllEffectsGood) {
                    arrayOf(PotionItemFacet(slot))
                } else {
                    emptyArray()
                }
            }
            Items.SNOWBALL, Items.EGG, Items.WIND_CHARGE -> arrayOf(ThrowableItemFacet(slot))
            else -> {
                if (slot.itemStack.isFood) {
                    arrayOf(FoodItemFacet(slot))
                } else {
                    emptyArray()
                }
            }
        }

        val commonFacets = listOfNotNull(
            PrimitiveItemFacet(slot, ItemCategory(GenericItemType.ANY_ITEM, item)),
            // Everything could be a weapon (i.e. a stick with Knockback II should be preferred over a stick)
            WeaponItemFacet.createIfUsefulAsWeapon(slot)
        )

        return specificItemFacets + commonFacets
    }
}
