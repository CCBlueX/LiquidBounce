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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.render.std140Size
import net.minecraft.client.renderer.MappableRingBuffer
import java.util.function.Supplier

enum class ClientUniformDefine(val uboName: String, val size: Int) {
    DISTANCE_FADE("u_DistanceFade", std140Size { vec4 }),
    ROUNDED_RECT("u_RoundedRect", std140Size { vec2 + float }),
    HAND_ITEM_LIGHTMAP("ItemChamsData", std140Size { int + float + vec4 + float + vec4 + float + int }),
    GUI_BLUR("BlurData", std140Size { float + float + float }),
    BLEND("BlendData", std140Size { vec4 }),
    THEME_BACKGROUND("ThemeBackgroundData", std140Size { float + vec2 + vec2 }),
    ;

    fun label(): String = "${LiquidBounce.CLIENT_NAME} Uniform ${this.uboName} (${this.size}b)"

    @JvmOverloads
    fun createSingleBuffer(
        labelGetter: Supplier<String> = Supplier(this::label),
    ): GpuBufferSlice {
        return gpuDevice.createBuffer(
            labelGetter,
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE,
            this.size.toLong(),
        ).slice()
    }

    @JvmOverloads
    fun createRingBuffer(
        labelGetter: Supplier<String> = Supplier(this::label),
    ): MappableRingBuffer {
        return MappableRingBuffer(
            labelGetter,
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_MAP_WRITE,
            this.size,
        )
    }

}
