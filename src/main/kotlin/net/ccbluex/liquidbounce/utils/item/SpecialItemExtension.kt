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

import com.mojang.blaze3d.platform.NativeImage
import net.ccbluex.liquidbounce.interfaces.ItemCooldownsAddition
import net.minecraft.core.component.DataComponents.MAP_ID
import net.minecraft.world.item.ItemCooldowns
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

fun ItemCooldowns.getCooldown(stack: ItemStack): ItemCooldownsAddition.Entry? =
    (this as ItemCooldownsAddition).`liquidBounce$getCooldown`(stack)

/**
 * Size of a map in pixels, both horizontally and vertically.
 *
 * @see net.minecraft.world.level.saveddata.maps.MapItemSavedData
 */
const val MAP_SIZE = 128

/**
 * @see net.minecraft.world.item.MapItem.getSavedData
 */
fun ItemStack.mapSavedData(level: Level): MapItemSavedData? =
    this[MAP_ID]?.let { MapItem.getSavedData(it, level) }

/**
 * ARGB color of the map pixel at the given coordinate, see [net.minecraft.util.ARGB].
 *
 * @see net.minecraft.world.level.material.MapColor.getColorFromPackedId
 */
fun MapItemSavedData.argbColorAt(x: Int, y: Int): Int =
    MapColor.getColorFromPackedId(this.colors[x + y * MAP_SIZE].toInt())

/**
 * @see net.minecraft.client.resources.MapTextureManager.MapInstance.updateTextureIfNeeded
 */
fun MapItemSavedData.toNativeImage(): NativeImage =
    NativeImage(NativeImage.Format.RGBA, MAP_SIZE, MAP_SIZE, true).also { image ->
        for (y in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                image.setPixel(x, y, this.argbColorAt(x, y))
            }
        }
    }
