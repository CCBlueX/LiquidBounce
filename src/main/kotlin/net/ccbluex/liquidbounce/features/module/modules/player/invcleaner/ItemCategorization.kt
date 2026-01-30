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

import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ArmorEvaluation
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ArmorItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ArrowItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.BlockItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.BowItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.CrossbowItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.FoodItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MaceItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MiningToolItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PotionItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PrimitiveItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.RodItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ShieldItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.SpearItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.SwordItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ThrowableItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorKitParameters
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.foodComponent
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isFood
import net.ccbluex.liquidbounce.utils.item.isHoe
import net.ccbluex.liquidbounce.utils.item.isMiningTool
import net.ccbluex.liquidbounce.utils.item.isPickaxe
import net.ccbluex.liquidbounce.utils.item.isPlayerArmor
import net.ccbluex.liquidbounce.utils.item.isShovel
import net.ccbluex.liquidbounce.utils.item.isSpear
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArrowItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.CrossbowItem
import net.minecraft.world.item.EggItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.PotionItem
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.WindChargeItem
import net.minecraft.world.level.material.LavaFluid
import net.minecraft.world.level.material.WaterFluid
import java.util.function.Predicate

@JvmRecord
data class ItemCategory(val type: ItemType, val subtype: Int) {
    fun isEmpty(): Boolean = type == ItemType.NONE
}

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
     * The user maybe wants to filter the items by a specific type, but they don't always want all versions of the item.
     * To stop the invcleaner from keeping items of every type, we can specify what function a specific item serves.
     * If that function is already served, we can just ignore it.
     */
    val providedFunction: ItemFunction? = null
) {
    ARMOR(true, allocationPriority = Priority.IMPORTANT_FOR_PLAYER_LIFE),
    SWORD(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_3, providedFunction = ItemFunction.WEAPON_LIKE),
    WEAPON(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_2, providedFunction = ItemFunction.WEAPON_LIKE),
    SPEAR(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_3, providedFunction = ItemFunction.WEAPON_LIKE),
    MACE(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_2, providedFunction = ItemFunction.WEAPON_LIKE),
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
    NONE(false);

    val defaultCategory = ItemCategory(this, 0)
}

enum class ItemFunction {
    WEAPON_LIKE,
    FOOD,
}

enum class ItemSortChoice(
    override val tag: String,
    val category: ItemCategory,
    /**
     * This is the function that is used for the greedy check.
     *
     * IF IT WAS IMPLEMENTED
     */
    val satisfactionCheck: Predicate<ItemStack>? = null,
) : Tagged {
    SWORD("Sword", ItemType.SWORD.defaultCategory, { it.isSword }),
    WEAPON("Weapon", ItemType.WEAPON.defaultCategory),
    SPEAR("Spear", ItemType.SPEAR.defaultCategory, { it.isSpear }),
    MACE("Mace", ItemType.MACE.defaultCategory, { it.item is MaceItem }),
    BOW("Bow", ItemType.BOW.defaultCategory),
    CROSSBOW("Crossbow", ItemType.CROSSBOW.defaultCategory),
    AXE("Axe", ItemCategory(ItemType.TOOL, MiningToolItemFacet.MASK_AXE), { it.isAxe }),
    PICKAXE("Pickaxe", ItemCategory(ItemType.TOOL, MiningToolItemFacet.MASK_PICKAXE), { it.isPickaxe }),
    SHOVEL("Shovel", ItemCategory(ItemType.TOOL, MiningToolItemFacet.MASK_SHOVEL), { it.isShovel }),
    HOE("Hoe", ItemCategory(ItemType.TOOL, MiningToolItemFacet.MASK_HOE), { it.isHoe }),
    ROD("Rod", ItemType.ROD.defaultCategory),
    SHIELD("Shield", ItemType.SHIELD.defaultCategory),
    WATER("Water", ItemType.BUCKET.defaultCategory),
    LAVA("Lava", ItemCategory(ItemType.BUCKET, 1)),
    MILK("Milk", ItemCategory(ItemType.BUCKET, 2)),
    PEARL("Pearl", ItemType.PEARL.defaultCategory, { it.item == Items.ENDER_PEARL }),
    GAPPLE(
        "Gapple",
        ItemType.GAPPLE.defaultCategory,
        Predicate { it.item == Items.GOLDEN_APPLE || it.item == Items.ENCHANTED_GOLDEN_APPLE },
    ),
    FOOD("Food", ItemType.FOOD.defaultCategory, { it.foodComponent != null }),
    POTION("Potion", ItemType.POTION.defaultCategory),
    BLOCK("Block", ItemType.BLOCK.defaultCategory, { it.item is BlockItem }),
    THROWABLES("Throwables", ItemType.THROWABLE.defaultCategory),
    IGNORE("Ignore", ItemType.NONE.defaultCategory),
    NONE("None", ItemType.NONE.defaultCategory),
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
            return ArmorPiece(VirtualItemSlot(item.defaultInstance, ItemSlot.Type.ARMOR, id))
        }

        /**
         * We expect to be full armor to be diamond armor.
         */
        @JvmStatic
        private val diamondArmorPieces: Map<EquipmentSlot, ArmorPiece> = enumMapOf(
            EquipmentSlot.HEAD, constructArmorPiece(Items.DIAMOND_HELMET, 0),
            EquipmentSlot.CHEST, constructArmorPiece(Items.DIAMOND_CHESTPLATE, 1),
            EquipmentSlot.LEGS, constructArmorPiece(Items.DIAMOND_LEGGINGS, 2),
            EquipmentSlot.FEET, constructArmorPiece(Items.DIAMOND_BOOTS, 3),
        )
    }

    /**
     * Sometimes there are situations where armor pieces are not the best ones with the current armor, but become
     * the best ones as soon as we upgrade one of the other armor pieces.
     * In those cases, we don't want to miss out on this armor piece in the future thus we keep it.
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
    @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod", "LongMethod")
    fun getItemFacets(slot: ItemSlot): List<ItemFacet> {
        val itemStack = slot.itemStack
        if (itemStack.isEmpty) {
            return emptyList()
        }

        return buildList {
            // Everything could be a weapon (i.e. a stick with Knockback II should be considered a weapon)
            add(WeaponItemFacet(slot))

            when (val item = itemStack.item) {
                is BowItem -> add(BowItemFacet(slot))
                is CrossbowItem -> add(CrossbowItemFacet(slot))
                is ArrowItem -> add(ArrowItemFacet(slot))
                is FishingRodItem -> add(RodItemFacet(slot))
                is ShieldItem -> add(ShieldItemFacet(slot))
                is BlockItem -> {
                    if (ScaffoldBlockItemSelection.isValidBlock(itemStack)
                        && !ScaffoldBlockItemSelection.isBlockUnfavourable(itemStack)
                    ) {
                        add(BlockItemFacet(slot))
                    } else {
                        add(ItemFacet(slot))
                    }
                }

                Items.MILK_BUCKET -> add(PrimitiveItemFacet(slot, ItemSortChoice.MILK.category))
                is BucketItem -> {
                    val category = when (item.content) {
                        is WaterFluid -> ItemSortChoice.WATER.category
                        is LavaFluid -> ItemSortChoice.LAVA.category
                        else -> ItemCategory(ItemType.BUCKET, item.content.javaClass.hashCode())
                    }
                    add(PrimitiveItemFacet(slot, category))
                }
                is PotionItem -> {
                    val areAllEffectsGood =
                        itemStack.getPotionEffects()
                            .all { it.effect in PotionItemFacet.GOOD_STATUS_EFFECTS }

                    if (areAllEffectsGood) {
                        add(PotionItemFacet(slot))
                    } else {
                        add(ItemFacet(slot))
                    }
                }

                is EnderpearlItem -> add(PrimitiveItemFacet(slot, ItemType.PEARL.defaultCategory))

                Items.GOLDEN_APPLE -> {
                    add(FoodItemFacet(slot))
                    add(PrimitiveItemFacet(slot, ItemType.GAPPLE.defaultCategory))
                }

                Items.ENCHANTED_GOLDEN_APPLE -> {
                    add(FoodItemFacet(slot))
                    add(PrimitiveItemFacet(slot, ItemType.GAPPLE.defaultCategory, 1))
                }

                is EggItem, is SnowballItem, is WindChargeItem -> add(ThrowableItemFacet(slot))

                else -> when {
                    itemStack.isPlayerArmor -> add(ArmorItemFacet(slot, futureArmorToKeep, armorComparator))

                    itemStack.isSword -> add(SwordItemFacet(slot))

                    itemStack.isSpear -> add(SpearItemFacet(slot))

                    itemStack.item is MaceItem -> add(MaceItemFacet(slot))

                    itemStack.isMiningTool -> add(MiningToolItemFacet(slot))

                    itemStack.isFood -> add(FoodItemFacet(slot))

                    else -> add(ItemFacet(slot))
                }
            }

        }
    }
}
