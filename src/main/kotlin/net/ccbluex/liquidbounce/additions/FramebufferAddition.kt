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

@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.additions

import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.pipeline.RenderTarget

interface FramebufferAddition {

    fun `liquidbounce$setColorAttachment`(texture: GpuTexture?): GpuTexture?

    fun `liquidbounce$setDepthAttachment`(texture: GpuTexture?): GpuTexture?

}

/**
 * Set the color attachment of the framebuffer to the given texture.
 *
 * @return the original color attachment
 */
inline fun RenderTarget.setColorAttachment(texture: GpuTexture?): GpuTexture? =
    (this as FramebufferAddition).`liquidbounce$setColorAttachment`(texture)

/**
 * Set the depth attachment of the framebuffer to the given texture.
 *
 * @return the original depth attachment
 */
inline fun RenderTarget.setDepthAttachment(texture: GpuTexture?): GpuTexture? =
    (this as FramebufferAddition).`liquidbounce$setDepthAttachment`(texture)
