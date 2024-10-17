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
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.*
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.UseAction
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

fun findInventorySlot(slots: List<ItemSlot>,  predicate: (ItemStack) -> Boolean): ItemSlot? {
    if (mc.player == null) {
        return null
    }

    return slots.find { predicate(it.itemStack) }
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

fun ItemStack?.getEnchantment(enchantment: RegistryKey<Enchantment>): Int {
    val enchantments = this?.get(DataComponentTypes.ENCHANTMENTS) ?: return 0

    return enchantments.getLevel(enchantment.toRegistryEntry())
}

val ItemStack.isConsumable: Boolean
    get() = this.isFood || this.item == Items.POTION || this.item == Items.MILK_BUCKET

val ItemStack.isFood: Boolean
    get() = foodComponent != null && this.useAction == UseAction.EAT
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

fun ItemStack.getAttributeValue(attribute: RegistryEntry<EntityAttribute>) = item.components
    .getOrDefault(
        DataComponentTypes.ATTRIBUTE_MODIFIERS,
        AttributeModifiersComponent.DEFAULT
    )
    .modifiers()
    .filter { modifier -> modifier.attribute() == attribute }
    .map { modifier -> modifier.modifier().value() }
    .firstOrNull()

val ItemStack.attackDamage: Double
    get() {
        val entityBaseDamage = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
        val baseDamage = getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
            ?: return 0.0

        /*
         * Client-side damage calculation for enchantments does not exist anymore
         * see https://bugs.mojang.com/browse/MC-196250
         *
         * We now use the following formula to calculate the damage:
         * https://minecraft.wiki/w/Sharpness -> 0.5 * level + 0.5.
         */
        return entityBaseDamage + baseDamage + getSharpnessDamage()
    }

val ItemStack.sharpnessLevel: Int
    get() = EnchantmentHelper.getLevel(Enchantments.SHARPNESS.toRegistryEntry(), this)

fun ItemStack.getSharpnessDamage(level: Int = sharpnessLevel) = if (level == 0) 0.0 else 0.5 * level + 0.5

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

fun RegistryKey<Enchantment>.toRegistryEntry(): RegistryEntry<Enchantment> {
    val world = mc.world
    requireNotNull(world) { "World is null" }

    val registry = world.registryManager.getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
    return registry.getOptional(this).orElseThrow { IllegalArgumentException("Unknown enchantment key $this") }
}
