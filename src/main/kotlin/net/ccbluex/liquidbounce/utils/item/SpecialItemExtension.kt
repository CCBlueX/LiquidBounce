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
import net.minecraft.world.item.ItemCooldowns
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

fun ItemCooldowns.getCooldown(stack: ItemStack): ItemCooldownsAddition.Entry? =
    (this as ItemCooldownsAddition).`liquidBounce$getCooldown`(stack)

private const val MAP_SIZE = 128

/**
 * @see net.minecraft.client.resources.MapTextureManager.MapInstance.updateTextureIfNeeded
 */
fun MapItemSavedData.toNativeImage(): NativeImage =
    NativeImage(NativeImage.Format.RGBA, MAP_SIZE, MAP_SIZE, true).also { image ->
        for (y in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                image.setPixel(x, y, MapColor.getColorFromPackedId(this.colors[x + y * MAP_SIZE].toInt()))
            }
        }
    }
