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
package net.ccbluex.liquidbounce.utils.item

import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.inventory.ALL_SLOTS_IN_INVENTORY
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.component.type.FoodComponent
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.AxeItem
import net.minecraft.item.HoeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.PickaxeItem
import net.minecraft.item.ShovelItem
import net.minecraft.item.ToolItem
import net.minecraft.registry.entry.RegistryEntry
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Create item with NBT tags
 *
 * @docs https://minecraft.gamepedia.com/Commands/give
 */
fun createItem(stack: String, amount: Int = 1): ItemStack =
    ItemStringReader(mc.world!!.registryManager).consume(StringReader(stack)).let {
        ItemStackArgument(it.item, it.components).createStack(amount, false)
    }

fun createSplashPotion(name: String, vararg effects: StatusEffectInstance): ItemStack {
    val itemStack = ItemStack(Items.SPLASH_POTION)

    itemStack.set(DataComponentTypes.CUSTOM_NAME, regular(name))
    itemStack.set<PotionContentsComponent>(
        DataComponentTypes.POTION_CONTENTS,
        PotionContentsComponent(Optional.empty(), Optional.empty(), effects.asList())
    )

    return itemStack
}


fun findHotbarSlot(item: Item): Int? = findHotbarSlot { it.item == item }

fun findHotbarSlot(predicate: (ItemStack) -> Boolean): Int? {
    return (0..8).firstOrNull { predicate(player.inventory.getStack(it)) }
}

fun findInventorySlot(item: Item): ItemSlot? = findInventorySlot { it.item == item }

fun findInventorySlot(predicate: (ItemStack) -> Boolean): ItemSlot? {
    if (mc.player == null) {
        return null
    }

    return ALL_SLOTS_IN_INVENTORY.find { predicate(it.itemStack) }
}

/**
 * Check if a stack is nothing (means empty slot)
 */
@OptIn(ExperimentalContracts::class)
fun ItemStack?.isNothing(): Boolean {
    contract {
        returns(true) implies (this@isNothing != null)
    }

    return this?.isEmpty == true
}

fun ItemStack?.getEnchantmentCount(): Int {
    val enchantments = this?.get(DataComponentTypes.ENCHANTMENTS) ?: return 0

    return enchantments.size
}

fun ItemStack?.getEnchantment(enchantment: Enchantment): Int {
    val enchantments = this?.get(DataComponentTypes.ENCHANTMENTS) ?: return 0

    return enchantments.getLevel(enchantment)
}

val ItemStack.isFood: Boolean
    get() = this.foodComponent != null
val ItemStack.foodComponent: FoodComponent?
    get() = this.get(DataComponentTypes.FOOD)

fun isHotbarSlot(slot: Int) = slot == 45 || slot in 36..44

val ToolItem.type: Int
    get() = when (this) {
        is AxeItem -> 0
        is PickaxeItem -> 1
        is ShovelItem -> 2
        is HoeItem -> 3
        else -> error("Unknown tool item $this (WTF?)")
    }

val ItemStack.attackDamage: Float
    get() {
        return player.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
            .toFloat() + EnchantmentHelper.getAttackDamage(
            this,
            null
        ) + item.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
    }

val ItemStack.attackSpeed: Float
    get() = item.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED)

private fun Item.getAttributeValue(attribute: RegistryEntry<EntityAttribute>): Float {
    val attribInstance = EntityAttributeInstance(attribute) {}

    this.components
        .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
        .applyModifiers(EquipmentSlot.MAINHAND) { attrib, modifier ->
            if (attrib != attribute) {
                return@applyModifiers
            }

            attribInstance.addTemporaryModifier(modifier)
        }

    return attribInstance.value.toFloat()
}
