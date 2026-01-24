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

package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

fun String.isItemWithTiers() : Boolean {
    return this.contains(TIER_ID)
}

fun String.generalTiersName() : String {
    return this.split(TIER_ID)[0]   // example: sword:tier:2 -> sword
}

fun String.autoShopItemTier() : Int {
    if (!isItemWithTiers()) {
        return 0
    }

    // example: sword:tier:2 -> 2
    return this.split(TIER_ID)[1].toIntOrNull() ?: 0
}

/**
 * Checks if there is a better item so that it's not necessary to buy the current item
 */
fun hasBetterTierItem(item: String, items: Map<String, Int>) : Boolean {
    return getAllTierItems(item, ModuleAutoShop.currentConfig.itemsWithTiers ?: emptyMap())
        .filter { it.autoShopItemTier() > item.autoShopItemTier() }
        .any { (items[it] ?: 0) > 0 }
}

fun actualTierItem(item: String, itemsWithTiers: Map<String, List<String>> =
    ModuleAutoShop.currentConfig.itemsWithTiers ?: emptyMap()) : String {
    val tiers = itemsWithTiers[item.generalTiersName()] ?: return item
    val tier = item.autoShopItemTier()

    // example: sword:tier:2 -> iron_sword
    return tiers.getOrElse(tier - 1) { item }
}

fun getAllTierItems(item: String, itemsWithTiers: Map<String, List<String>>) : List<String> {
    val generalName = item.generalTiersName()    // example: sword:tier:2 -> sword
    val tiers = itemsWithTiers[generalName] ?: return emptyList()

    // example: [sword:tier:1, sword:tier:2, sword:tier:3, sword:tier:4]
    return List(tiers.size) { index -> "${generalName}$TIER_ID${index + 1}" }
}
