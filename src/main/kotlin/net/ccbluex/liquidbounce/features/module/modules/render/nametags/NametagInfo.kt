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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack

class NametagInfo(
    /**
     * The text to render as nametag
     */
    val text: String,
    /**
     * The items that should be rendered above the name tag
     */
    val items: List<ItemStack?>,
) {
    companion object {
        fun createForEntity(entity: Entity): NametagInfo {
            val text = NametagTextFormatter(entity).format()
            val items = createItemList(entity)

            return NametagInfo(text, items)
        }

        /**
         * Creates a list of items that should be rendered above the name tag. Currently, it is the item in main hand,
         * the item in off-hand (as long as it exists) and the armor items.
         */
        private fun createItemList(entity: Entity): List<ItemStack?> {
            if (entity !is LivingEntity) {
                return emptyList()
            }

            val itemIterator = entity.handItems.iterator()

            val firstHandItem = itemIterator.next()
            val secondHandItem = itemIterator.next()

            val armorItems = entity.armorItems

            val heldItems =
                if (secondHandItem.isNothing()) {
                    listOf(firstHandItem)
                } else {
                    listOf(firstHandItem, secondHandItem)
                }

            return heldItems + armorItems
        }
    }
}
