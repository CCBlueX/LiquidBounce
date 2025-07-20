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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

@Suppress("DataClassPrivateConstructor")
data class Nametag private constructor(
    val entity: Entity,
    /**
     * The text to render as nametag
     */
    val text: Text,
    /**
     * The items that should be rendered above the name tag
     */
    val items: List<ItemStack?>
) {

    var position: Vec3? = null

    constructor(entity: Entity) : this(entity, NametagTextFormatter(entity).format(), createItemList(entity))

    fun calculatePosition(tickDelta: Float) {
        val nametagPos = entity.interpolateCurrentPosition(tickDelta)
            .add(0.0, entity.getEyeHeight(entity.pose) + 0.55, 0.0)

        position = WorldToScreen.calculateScreenPos(nametagPos)
    }

    companion object {

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

            val armorItems = entity.armorItems.reversed()

            return listOf(firstHandItem) + armorItems + secondHandItem
        }

    }

}

