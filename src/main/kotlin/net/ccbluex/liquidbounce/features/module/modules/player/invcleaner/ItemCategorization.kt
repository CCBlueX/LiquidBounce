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
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ArmorItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ArrowItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.BlockItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.BowItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.CrossbowItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.FoodItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.GodAxeFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.KnockbackItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MaceItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MiningToolItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PotionItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PrimitiveItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.RodItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.SharpAxeFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ShieldItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.SpearItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.SwordItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ThrowableItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeaponItemFacet
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot.Type
import net.ccbluex.liquidbounce.utils.inventory.VirtualItemSlot
import net.ccbluex.liquidbounce.utils.item.armor.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.armor.ArmorComparatorMode
import net.ccbluex.liquidbounce.utils.item.armor.ArmorEvaluation
import net.ccbluex.liquidbounce.utils.item.armor.ArmorKitParameters
import net.ccbluex.liquidbounce.utils.item.armor.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isFood
import net.ccbluex.liquidbounce.utils.item.isMiningTool
import net.ccbluex.liquidbounce.utils.item.isPlayerArmor
import net.ccbluex.liquidbounce.utils.item.isSpear
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.core.component.DataComponents
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
import net.minecraft.world.item.LingeringPotionItem
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.PotionItem
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.item.SnowballItem
import net.minecraft.world.item.SplashPotionItem
import net.minecraft.world.item.WindChargeItem
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.material.Fluid

/**
 * Describes what an item is, and is used as the key when facets are grouped by category as well as when a slot asks
 * for something.
 */
data class ItemCategory(val type: GenericItemType, val subtype: ItemSubtype = ItemSubtype.None)

/**
 * The distinction within a [GenericItemType]. Facets that share a category are compared with each other, so they have
 * to be mutually comparable.
 */
sealed interface ItemSubtype {
    /**
     * The type alone describes the item.
     */
    data object None : ItemSubtype

    /**
     * One exact item.
     */
    data class SpecificItem(val item: Item) : ItemSubtype

    /**
     * The tool types an item serves, as a bitmask of the masks of [MiningToolItemFacet].
     */
    data class ToolTypes(val mask: Int) : ItemSubtype

    /**
     * One armor slot, identified by the entity slot id of the [ArmorPiece].
     */
    data class ArmorSlot(val entitySlotId: Int) : ItemSubtype

    /**
     * The fluid a bucket holds.
     */
    data class BucketFluid(val fluid: Fluid) : ItemSubtype

    /**
     * The milk bucket, which does not hold a fluid.
     */
    data object MilkBucket : ItemSubtype
}

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
    val allocationPriority: Priority = Priority.NORMAL,
) {
    ARMOR(true, allocationPriority = Priority.IMPORTANT_FOR_PLAYER_LIFE),
    SWORD(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_3),
    WEAPON(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_2),
    SPEAR(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_3),
    MACE(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_2),

    /**
     * Not [ItemFunction.WEAPON_LIKE] and below the weapon types, so the main weapon is picked first and a separate
     * knockback item (e.g. a knockback stick) still gets its own slot.
     */
    KNOCKBACK(true, allocationPriority = Priority.NORMAL),
    BOW(true),
    CROSSBOW(true),
    ARROW(true),
    TOOL(true, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    ROD(true),
    SHIELD(true),
    THROWABLE(false),
    FOOD(false),
    BUCKET(false),
    PEARL(false, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    GAPPLE(false, allocationPriority = Priority.IMPORTANT_FOR_USAGE_1),
    TOTEM(false),
    POTION(false),
    BLOCK(false),
    /**
     * What nobody keeps, such as an empty bottle or a potion that only harms the one who drinks it.
     */
    JUNK(false),
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
            return ArmorPiece(VirtualItemSlot(item.defaultInstance, Type.ARMOR, id))
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

        /**
         * Note: this must be initialized AFTER [diamondArmorPieces], because the [ItemCategorization] constructor
         * reads [diamondArmorPieces] during initialization. Companion-object properties are initialized in
         * declaration order, so declaring this first would pass a null map to
         * [ArmorKitParameters.getParametersForSlots] and crash the whole class's static initialization.
         */
        @JvmField
        val Default = ItemCategorization(emptyList())
    }

    /**
     * Sometimes there are situations where armor pieces are not the best ones with the current armor, but become
     * the best ones as soon as we upgrade one of the other armor pieces.
     * In those cases, we don't want to miss out on this armor piece in the future thus we keep it.
     */
    private val futureArmorToKeep: List<ItemSlot>
    private val armorComparator: ArmorComparator

    /**
     * Knockback of the best real weapon, which is the one kept as main weapon. A dedicated knockback item has to
     * knock back harder than it.
     */
    private val mainWeaponKnockback: Int =
        availableItems.asSequence()
            .filter { slot ->
                val stack = slot.itemStack
                stack.isSword || stack.isSpear || stack.item is MaceItem || stack.isAxe
            }
            .map(::WeaponItemFacet)
            .maxOrNull()
            ?.itemStack?.getEnchantment(Enchantments.KNOCKBACK)
            ?: 0

    init {
        val armorSlots = availableItems.filter(ModuleAutoArmor::isAllowedByCurseFilter)
        val comparatorMode = ModuleAutoArmor.activeComparatorMode
        val durabilityThreshold = ModuleAutoArmor.activeDurabilityThreshold
        val considerProjectileProtection = ModuleAutoArmor.activeConsiderProjectileProtection

        val findBestArmorPieces = ArmorEvaluation.findBestArmorPieces(
            slots = armorSlots,
            durabilityThreshold = durabilityThreshold,
            mode = comparatorMode,
            considerProjectileProtection = considerProjectileProtection,
        )

        this.armorComparator = ArmorEvaluation.getArmorComparatorFor(
            findBestArmorPieces,
            durabilityThreshold = durabilityThreshold,
            mode = comparatorMode,
            considerProjectileProtection = considerProjectileProtection,
        )

        this.futureArmorToKeep = if (comparatorMode == ArmorComparatorMode.RAW_DEFENSE) {
            emptyList()
        } else {
            val armorParameterForSlot = ArmorKitParameters.getParametersForSlots(diamondArmorPieces)
            val armorComparatorForFullArmor = ArmorEvaluation.getArmorComparatorForParameters(
                armorParameterForSlot,
                durabilityThreshold = durabilityThreshold,
                mode = comparatorMode,
                considerProjectileProtection = considerProjectileProtection,
            )

            ArmorEvaluation.findBestArmorPiecesWithComparator(
                armorSlots,
                armorComparatorForFullArmor
            ).values.mapNotNull { it?.itemSlot }
        }
    }

    /**
     * Returns a list of facets an item represents. For example an axe is an axe, but also a sword:
     * - (SANDSTONE_BLOCK, 64) => `[Block(SANDSTONE_BLOCK, 64)]`
     * - (DIAMOND_AXE, 1) => `[Axe(DIAMOND_AXE, 1), Tool(DIAMOND_AXE, 1)]`
     */
    @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod", "LongMethod")
    fun getItemFacets(slot: ItemSlot): List<ItemFacet> {
        if (slot.itemStack.isEmpty) {
            return emptyList()
        }

        return buildList {
            val item = slot.itemStack.item

            if (slot.itemStack.getEnchantment(Enchantments.KNOCKBACK) > mainWeaponKnockback) {
                add(KnockbackItemFacet(slot))
            }

            this += PrimitiveItemFacet(
                slot,
                ItemCategory(GenericItemType.ANY_ITEM, ItemSubtype.SpecificItem(item))
            )
            // Everything could be a weapon (i.e. a stick with Knockback II should be preferred over a stick)
            WeaponItemFacet.createIfUsefulAsWeapon(slot)?.let { this += it }

            when {
                // Treat animal armor as a normal item
                slot.itemStack.isPlayerArmor && ModuleAutoArmor.isAllowedByCurseFilter(slot) ->
                    add(ArmorItemFacet(slot, futureArmorToKeep, armorComparator))
                slot.itemStack.isSword -> add(SwordItemFacet(slot))
                item is BowItem -> add(BowItemFacet(slot))
                item is CrossbowItem -> add(CrossbowItemFacet(slot))
                item is ArrowItem -> add(ArrowItemFacet(slot))
                item is FishingRodItem -> add(RodItemFacet(slot))
                item is ShieldItem -> add(ShieldItemFacet(slot))
                slot.itemStack.isSpear -> add(SpearItemFacet(slot))
                item is MaceItem -> add(MaceItemFacet(slot))
                slot.itemStack.isAxe -> {
                    val sharpnessLevel = slot.itemStack.getEnchantment(Enchantments.SHARPNESS)
                    when {
                        sharpnessLevel >= 100 -> add(GodAxeFacet(slot))
                        sharpnessLevel >= 5 -> add(SharpAxeFacet(slot))
                        else -> add(MiningToolItemFacet(slot))
                    }
                }
                slot.itemStack.isMiningTool -> add(MiningToolItemFacet(slot))
                item is BlockItem -> {
                    val isUsableBlock = (ScaffoldBlockItemSelection.isValidBlock(slot.itemStack)
                        && !ScaffoldBlockItemSelection.isBlockUnfavourable(slot.itemStack))

                    if (isUsableBlock) {
                        add(BlockItemFacet(slot))
                    }
                }
                item is PotionItem -> {
                    val effects = slot.itemStack.getPotionEffects().toList()
                    val isUsefulPotion =
                        effects.isNotEmpty() && effects.all { it.effect in PotionItemFacet.GOOD_STATUS_EFFECTS }

                    if (isUsefulPotion) {
                        add(PotionItemFacet(slot))
                    } else if (!slot.itemStack.has(DataComponents.CUSTOM_NAME) && (effects.isEmpty() ||
                            (item !is SplashPotionItem && item !is LingeringPotionItem &&
                                effects.any { it.effect in PotionItemFacet.BAD_STATUS_EFFECTS }))
                    ) {
                        add(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.JUNK)))
                    }
                }
                item == Items.GLASS_BOTTLE -> {
                    if (!slot.itemStack.has(DataComponents.CUSTOM_NAME)) {
                        add(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.JUNK)))
                    }
                }
                item == Items.TOTEM_OF_UNDYING -> add(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.TOTEM)))
                item is EggItem || item is SnowballItem || item is WindChargeItem -> {
                    add(ThrowableItemFacet(slot))
                }
                item == Items.MILK_BUCKET -> add(
                    PrimitiveItemFacet(slot, ItemCategory(GenericItemType.BUCKET, ItemSubtype.MilkBucket))
                )
                item is BucketItem -> add(
                    PrimitiveItemFacet(
                        slot,
                        ItemCategory(GenericItemType.BUCKET, ItemSubtype.BucketFluid(item.content))
                    )
                )
                item is EnderpearlItem -> add(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.PEARL)))
                item == Items.GOLDEN_APPLE -> {
                    add(FoodItemFacet(slot))
                    add(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.GAPPLE)))
                }
                item == Items.ENCHANTED_GOLDEN_APPLE -> {
                    add(FoodItemFacet(slot))
                    add(PrimitiveItemFacet(slot, ItemCategory(GenericItemType.GAPPLE), 1))
                }
                else -> {
                    if (slot.itemStack.isFood) {
                        add(FoodItemFacet(slot))
                    }
                }
            }
        }
    }
}
