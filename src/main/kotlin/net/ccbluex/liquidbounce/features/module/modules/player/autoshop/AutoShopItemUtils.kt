/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries

const val WOOL_ID           = "wool"
const val TERRACOTTA_ID     = "terracotta"
const val STAINED_GLASS_ID  = "stained_glass"
const val CONCRETE_ID       = "concrete"
const val POTION_PREFIX     = "potion:"     //usage example: potion:speed
const val EXPERIENCE_ID     = "experience"
const val TIER_ID           = ":tier:"      //usage example: sword:tier:2

/**
 * The items usually used to buy other items in BedWars.
 *
 * A server will take them from the player if the latter wants to buy something.
 */
val LIMITED_ITEMS = setOf(
    "brick", "iron_ingot", "gold_ingot", "diamond", "emerald", EXPERIENCE_ID
)

fun Item.isWool(): Boolean {
    return this is BlockItem && this in WOOL_BLOCKS
}

fun Item.isTerracotta() : Boolean {
    return this is BlockItem && this in TERRACOTTA_BLOCKS
}

fun Item.isStainedGlass() : Boolean {
    return this is BlockItem && this in STAINED_GLASS_BLOCKS
}

fun Item.isConcrete() : Boolean {
    return this is BlockItem && this in CONCRETE_BLOCKS
}

fun String.isArmorItem() : Boolean {
    // example: armor:tier:3 -> diamond_boots:protection:2 -> diamond_boots
    if (this.isItemWithTiers()) {
        val actualTierItem = actualTierItem(this)
        return actualTierItem.split(":")[0] in ARMOR_ITEMS
    }

    return this.split(":")[0] in ARMOR_ITEMS
}

fun GenericContainerScreen.stacks(): List<String> {
    return this.screenHandler.slots
        .filter { !it.stack.isNothing() &&
            it.inventory === this.screenHandler.inventory }
        .mapNotNull { Registries.ITEM.getId(it.stack.item).path }
}

private val WOOL_BLOCKS = setOf(
    Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL,
    Items.GRAY_WOOL, Items.GREEN_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL,
    Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL,
    Items.PURPLE_WOOL, Items.RED_WOOL, Items.WHITE_WOOL, Items.YELLOW_WOOL
)

private val TERRACOTTA_BLOCKS = setOf(
    Items.BLACK_TERRACOTTA, Items.BLUE_TERRACOTTA, Items.BROWN_TERRACOTTA,
    Items.CYAN_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.GREEN_TERRACOTTA,
    Items.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
    Items.LIME_TERRACOTTA, Items.MAGENTA_TERRACOTTA, Items.ORANGE_TERRACOTTA,
    Items.PINK_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.RED_TERRACOTTA,
    Items.WHITE_TERRACOTTA, Items.YELLOW_TERRACOTTA
)

private val STAINED_GLASS_BLOCKS = setOf(
    Items.BLACK_STAINED_GLASS, Items.BLUE_STAINED_GLASS, Items.BROWN_STAINED_GLASS,
    Items.CYAN_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.GREEN_STAINED_GLASS,
    Items.LIGHT_BLUE_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS,
    Items.LIME_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS,
    Items.ORANGE_STAINED_GLASS, Items.PINK_STAINED_GLASS, Items.PURPLE_STAINED_GLASS,
    Items.RED_STAINED_GLASS, Items.WHITE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS
)

private val CONCRETE_BLOCKS = setOf(
    Items.BLACK_CONCRETE, Items.BLUE_CONCRETE, Items.BROWN_CONCRETE, Items.CYAN_CONCRETE,
    Items.GRAY_CONCRETE, Items.GREEN_CONCRETE, Items.LIGHT_BLUE_CONCRETE,
    Items.LIGHT_GRAY_CONCRETE, Items.LIME_CONCRETE, Items.MAGENTA_CONCRETE,
    Items.ORANGE_CONCRETE, Items.PINK_CONCRETE, Items.PURPLE_CONCRETE,
    Items.RED_CONCRETE, Items.WHITE_CONCRETE, Items.YELLOW_CONCRETE
)

/**
 * Some BedWars implementations don't give players armor straight after a purchase.
 * The players receive it after a shop gets closed.
 */
private val ARMOR_ITEMS = arrayOf(
    Items.LEATHER_HELMET,
    Items.CHAINMAIL_HELMET,
    Items.IRON_HELMET,
    Items.DIAMOND_HELMET,
    Items.NETHERITE_HELMET,

    Items.LEATHER_CHESTPLATE,
    Items.CHAINMAIL_CHESTPLATE,
    Items.IRON_CHESTPLATE,
    Items.DIAMOND_CHESTPLATE,
    Items.NETHERITE_CHESTPLATE,

    Items.LEATHER_LEGGINGS,
    Items.CHAINMAIL_LEGGINGS,
    Items.IRON_LEGGINGS,
    Items.DIAMOND_LEGGINGS,
    Items.NETHERITE_LEGGINGS,

    Items.LEATHER_BOOTS,
    Items.CHAINMAIL_BOOTS,
    Items.IRON_BOOTS,
    Items.DIAMOND_BOOTS,
    Items.NETHERITE_BOOTS
).map { Registries.ITEM.getId(it).path }.toSet()
