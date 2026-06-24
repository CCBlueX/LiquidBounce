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

package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.utils.kotlin.contains
import net.minecraft.core.TypedInstance
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Tool

val ItemStack.isConsumable: Boolean
    get() = this.has(DataComponents.CONSUMABLE)

val ItemStack.isFood: Boolean
    get() = foodComponent != null

val DataComponentGetter.foodComponent: FoodProperties?
    get() = this.get(DataComponents.FOOD)

val DataComponentGetter.toolComponent: Tool?
    get() = this.get(DataComponents.TOOL)

val TypedInstance<Item>.isBundle
    get() = this.`is`(ItemTags.BUNDLES)

val TypedInstance<Item>.isAnyChest: Boolean
    get() = this.`is`(Items.CHEST)
        || this.`is`(Items.TRAPPED_CHEST)
        || this.`is`(Items.ENDER_CHEST)
        || this.typeHolder().value() in Items.COPPER_CHEST

// Tools

val TypedInstance<Item>.isSword
    get() = this.`is`(ItemTags.SWORDS)

val TypedInstance<Item>.isSpear
    get() = this.`is`(ItemTags.SPEARS)

val TypedInstance<Item>.isPickaxe
    get() = this.`is`(ItemTags.PICKAXES)

val TypedInstance<Item>.isAxe
    get() = this.`is`(ItemTags.AXES)

val TypedInstance<Item>.isShovel
    get() = this.`is`(ItemTags.SHOVELS)

val TypedInstance<Item>.isHoe
    get() = this.`is`(ItemTags.HOES)

/**
 * Replacement of 1.21.4 `MiningToolItem`
 */
val TypedInstance<Item>.isMiningTool
    get() = isAxe || isPickaxe || isShovel || isHoe

// Armors

val TypedInstance<Item>.isFootArmor
    get() = this.`is`(ItemTags.FOOT_ARMOR)

val TypedInstance<Item>.isLegArmor
    get() = this.`is`(ItemTags.LEG_ARMOR)

val TypedInstance<Item>.isChestArmor
    get() = this.`is`(ItemTags.CHEST_ARMOR)

val TypedInstance<Item>.isHeadArmor
    get() = this.`is`(ItemTags.HEAD_ARMOR)

val TypedInstance<Item>.isPlayerArmor
    get() = isFootArmor || isLegArmor || isChestArmor || isHeadArmor

val DataComponentGetter.equippableComponent
    get() = this.get(DataComponents.EQUIPPABLE)

val DataComponentGetter.equipmentSlot
    get() = this.equippableComponent?.slot

val DataComponentGetter.armorToughness
    get() = this.getAttributeValue(Attributes.ARMOR_TOUGHNESS, this.equipmentSlot)

val DataComponentGetter.armorValue
    get() = this.getAttributeValue(Attributes.ARMOR, this.equipmentSlot)

val DataComponentGetter.armorKnockbackResistance
    get() = this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE, this.equipmentSlot)

// Shield

/**
 * @see Items.SHIELD
 */
val DataComponentGetter.blocksAttacksComponent
    get() = this.get(DataComponents.BLOCKS_ATTACKS)
