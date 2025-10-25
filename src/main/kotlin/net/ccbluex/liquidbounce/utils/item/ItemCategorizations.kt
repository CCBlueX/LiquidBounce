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

package net.ccbluex.liquidbounce.utils.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.FoodComponent
import net.minecraft.component.type.ToolComponent
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.consume.UseAction
import net.minecraft.registry.tag.ItemTags

val ItemStack.isConsumable: Boolean
    get() = this.isFood || this.item == Items.POTION || this.item == Items.MILK_BUCKET

val ItemStack.isFood: Boolean
    get() = foodComponent != null && this.useAction == UseAction.EAT

val ItemStack.foodComponent: FoodComponent?
    get() = this.get(DataComponentTypes.FOOD)

val ItemStack.toolComponent: ToolComponent?
    get() = this.get(DataComponentTypes.TOOL)

val ItemStack.isBundle
    get() = this.isIn(ItemTags.BUNDLES)

// Tools

val ItemStack.isSword
    get() = this.isIn(ItemTags.SWORDS)

val ItemStack.isPickaxe
    get() = this.isIn(ItemTags.PICKAXES)

val ItemStack.isAxe
    get() = this.isIn(ItemTags.AXES)

val ItemStack.isShovel
    get() = this.isIn(ItemTags.SHOVELS)

val ItemStack.isHoe
    get() = this.isIn(ItemTags.HOES)

/**
 * Replacement of 1.21.4 `MiningToolItem`
 */
val ItemStack.isMiningTool
    get() = isAxe || isPickaxe || isShovel || isHoe

// Armors

val ItemStack.isFootArmor
    get() = this.isIn(ItemTags.FOOT_ARMOR)

val ItemStack.isLegArmor
    get() = this.isIn(ItemTags.LEG_ARMOR)

val ItemStack.isChestArmor
    get() = this.isIn(ItemTags.CHEST_ARMOR)

val ItemStack.isHeadArmor
    get() = this.isIn(ItemTags.HEAD_ARMOR)

val ItemStack.isPlayerArmor
    get() = isFootArmor || isLegArmor || isChestArmor || isHeadArmor

val ItemStack.equippableComponent
    get() = this.get(DataComponentTypes.EQUIPPABLE)

val ItemStack.equipmentSlot
    get() = this.equippableComponent?.slot

val ItemStack.armorToughness
    get() = this.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS)

val ItemStack.armorValue
    get() = this.getAttributeValue(EntityAttributes.ARMOR)

val ItemStack.armorKnockbackResistance
    get() = this.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE)
