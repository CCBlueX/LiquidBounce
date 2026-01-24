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

import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Tool

val ItemStack.isConsumable: Boolean
    get() = this.isFood || this.item == Items.POTION || this.item == Items.MILK_BUCKET

val ItemStack.isFood: Boolean
    get() = foodComponent != null && this.useAnimation == ItemUseAnimation.EAT

val ItemStack.foodComponent: FoodProperties?
    get() = this.get(DataComponents.FOOD)

val ItemStack.toolComponent: Tool?
    get() = this.get(DataComponents.TOOL)

val ItemStack.isBundle
    get() = this.`is`(ItemTags.BUNDLES)

// Tools

val ItemStack.isSword
    get() = this.`is`(ItemTags.SWORDS)

val ItemStack.isSpear
    get() = this.`is`(ItemTags.SPEARS)

val ItemStack.isPickaxe
    get() = this.`is`(ItemTags.PICKAXES)

val ItemStack.isAxe
    get() = this.`is`(ItemTags.AXES)

val ItemStack.isShovel
    get() = this.`is`(ItemTags.SHOVELS)

val ItemStack.isHoe
    get() = this.`is`(ItemTags.HOES)

/**
 * Replacement of 1.21.4 `MiningToolItem`
 */
val ItemStack.isMiningTool
    get() = isAxe || isPickaxe || isShovel || isHoe

// Armors

val ItemStack.isFootArmor
    get() = this.`is`(ItemTags.FOOT_ARMOR)

val ItemStack.isLegArmor
    get() = this.`is`(ItemTags.LEG_ARMOR)

val ItemStack.isChestArmor
    get() = this.`is`(ItemTags.CHEST_ARMOR)

val ItemStack.isHeadArmor
    get() = this.`is`(ItemTags.HEAD_ARMOR)

val ItemStack.isPlayerArmor
    get() = isFootArmor || isLegArmor || isChestArmor || isHeadArmor

val ItemStack.equippableComponent
    get() = this.get(DataComponents.EQUIPPABLE)

val ItemStack.equipmentSlot
    get() = this.equippableComponent?.slot

val ItemStack.armorToughness
    get() = this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)

val ItemStack.armorValue
    get() = this.getAttributeValue(Attributes.ARMOR)

val ItemStack.armorKnockbackResistance
    get() = this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)

// Shield

/**
 * @see Items.SHIELD
 */
val ItemStack.blocksAttacksComponent
    get() = this.get(DataComponents.BLOCKS_ATTACKS)
