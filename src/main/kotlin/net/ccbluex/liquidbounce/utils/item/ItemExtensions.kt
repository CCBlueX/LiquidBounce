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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.item

import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.minecraft.block.Block
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifiersComponent
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
import net.minecraft.util.math.BlockPos
import java.util.*

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
        PotionContentsComponent(Optional.empty(), Optional.empty(), effects.unmodifiable(), Optional.empty())
    )

    return itemStack
}

fun ItemStack?.getEnchantmentCount(): Int {
    val enchantments = this?.get(DataComponentTypes.ENCHANTMENTS) ?: return 0

    return enchantments.size
}

fun ItemStack?.getEnchantment(enchantment: RegistryKey<Enchantment>): Int {
    val enchantments = this?.get(DataComponentTypes.ENCHANTMENTS) ?: return 0

    return enchantments.getLevel(enchantment.toRegistryEntry())
}

/**
 * @return if this item stack has same [Item] and [net.minecraft.component.ComponentChanges]
 * with the other item stack
 */
fun ItemStack.isMergeable(other: ItemStack): Boolean {
    return this.item == other.item && this.componentChanges == other.componentChanges
}

fun ItemStack.canMerge(other: ItemStack): Boolean {
    return this.isMergeable(other) && this.count + other.count <= this.maxCount
}

fun ItemStack.getAttributeValue(attribute: RegistryEntry<EntityAttribute>) = item.components
    .getOrDefault(
        DataComponentTypes.ATTRIBUTE_MODIFIERS,
        AttributeModifiersComponent.DEFAULT
    )
    .modifiers
    .filter { modifier -> modifier.attribute == attribute }
    .firstNotNullOfOrNull { modifier -> modifier.modifier.value }

val ItemStack.attackDamage: Double
    get() {
        val entityBaseDamage = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE)
        val baseDamage = getAttributeValue(EntityAttributes.ATTACK_DAMAGE)
            ?: return 0.0

        /*
         * Client-side damage calculation for enchantments does not exist anymore
         * see https://bugs.mojang.com/browse/MC-196250
         *
         * We now use the following formula to calculate the damage:
         * https://minecraft.wiki/w/Sharpness
         * >= 1.9 -> 0.5 * level + 0.5
         * else -> 1.25 * level
         */
        return entityBaseDamage + baseDamage + getSharpnessDamage()
    }

val ItemStack.sharpnessLevel: Int
    get() = EnchantmentHelper.getLevel(Enchantments.SHARPNESS.toRegistryEntry(), this)

fun ItemStack.getSharpnessDamage(level: Int = sharpnessLevel): Double =
    if (!isOlderThanOrEqual1_8) {
        when (level) {
            0 -> 0.0
            else -> 0.5 * level + 0.5
        }
    } else {
        level * 1.25
    }

val ItemStack.attackSpeed: Double
    get() = item.getAttributeValue(EntityAttributes.ATTACK_SPEED)

val ItemStack.durability
    get() = this.maxDamage - this.damage

private fun Item.getAttributeValue(attribute: RegistryEntry<EntityAttribute>): Double {
    val attribInstance = EntityAttributeInstance(attribute) {}

    this.components
        .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
        .applyModifiers(EquipmentSlot.MAINHAND) { attrib, modifier ->
            if (attrib != attribute) {
                return@applyModifiers
            }

            attribInstance.addTemporaryModifier(modifier)
        }

    return attribInstance.value
}

fun RegistryKey<Enchantment>.toRegistryEntry(): RegistryEntry<Enchantment> {
    val world = mc.world
    requireNotNull(world) { "World is null" }

    val registry = world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
    return registry.getOptional(this).orElseThrow { IllegalArgumentException("Unknown enchantment key $this") }
}

fun ItemStack.getBlock(): Block? {
    val item = this.item
    if (item !is BlockItem) {
        return null
    }

   return item.block
}

fun ItemStack.isFullBlock(): Boolean {
    val block = this.getBlock() ?: return false
    return block.defaultState.isFullCube(mc.world!!, BlockPos.ORIGIN)
}
