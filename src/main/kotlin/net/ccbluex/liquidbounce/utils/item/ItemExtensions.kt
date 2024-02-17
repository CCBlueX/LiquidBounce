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
import net.minecraft.client.MinecraftClient
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.potion.PotionUtil
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * Create item with NBT tags
 *
 * @docs https://minecraft.gamepedia.com/Commands/give
 */
fun createItem(stack: String, amount: Int = 1): ItemStack =
    ItemStringReader.item(Registries.ITEM.readOnlyWrapper, StringReader(stack)).let {
        ItemStackArgument(it.item, it.nbt).createStack(amount, false)
    }

fun createSplashPotion(name: String, vararg effects: StatusEffectInstance): ItemStack {
    return PotionUtil.setCustomPotionEffects(
        ItemStack(Items.SPLASH_POTION).setCustomName(regular(name)),
        effects.toList()
    )
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
fun ItemStack?.isNothing() = this?.isEmpty == true

fun ItemStack?.getEnchantmentCount(): Int {
    val enchantments = this?.enchantments ?: return 0

    var c = 0

    for (enchantment in enchantments) {
        if (enchantment !is NbtCompound) {
            continue
        }

        if (enchantment.contains("ench") || enchantment.contains("id")) {
            c++
        }
    }

    return c
}

fun ItemStack?.getEnchantment(enchantment: Enchantment): Int {
    val enchantments = this?.enchantments ?: return 0
    val enchId = Registries.ENCHANTMENT.getId(enchantment)

    for (enchantmentEntry in enchantments) {
        if (enchantmentEntry !is NbtCompound) {
            continue
        }

        if (enchantmentEntry.contains("id") && Identifier.tryParse(enchantmentEntry.getString("id")) == enchId) {
            return enchantmentEntry.getShort("lvl").toInt()
        }
    }

    return 0
}

fun isHotbarSlot(slot: Int) = slot == 45 || slot in 36..44

val ToolItem.type: Int
    get() = when (this) {
        is AxeItem -> 0
        is PickaxeItem -> 1
        is ShovelItem -> 2
        is HoeItem -> 3
        else -> error("Unknown tool item $this (WTF?)")
    }

val Item.attackDamage: Float
    get() = when (this) {
        is SwordItem -> this.attackDamage
        is MiningToolItem -> this.attackDamage + 1.0f
        is ToolItem -> this.material.attackDamage
        else -> 1.0f
    }

val Item.attackSpeed: Float
    get() = getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED)

private fun Item.getAttributeValue(attribute: EntityAttribute): Float {
    val attribInstance = EntityAttributeInstance(attribute) {}

    for (entityAttributeModifier in this.getAttributeModifiers(EquipmentSlot.MAINHAND)
        .get(attribute)) {
        attribInstance.addTemporaryModifier(entityAttributeModifier)
    }

    return attribInstance.value.toFloat()
}

fun Item.isWool(): Boolean {
    return this in WOOL_BLOCKS
}

private val WOOL_BLOCKS = arrayOf(
    Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL,
    Items.GRAY_WOOL, Items.GREEN_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL,
    Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL,
    Items.PURPLE_WOOL, Items.RED_WOOL, Items.WHITE_WOOL, Items.YELLOW_WOOL)
