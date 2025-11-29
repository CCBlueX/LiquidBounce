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
package net.ccbluex.liquidbounce.render.buffer

import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.GlCommandEncoder
import net.minecraft.client.texture.GlTexture

/**
 * Can be used to wrap framebuffer IDs in order to pass them to [GlobalFramebuffer.push].
 */
class MinecraftFramebuffer(val framebuffer: Framebuffer) : AbstractFramebuffer() {
    private val wrappedId = getFramebufferIdFromFramebuffer(framebuffer)

    override val id: Int
        get() = wrappedId
    override val width: Int
        get() = framebuffer.textureWidth
    override val height: Int
        get() = framebuffer.textureHeight
    override val useDepth: Boolean
        get() = this.framebuffer.useDepthAttachment

    override fun resize(width: Int, height: Int) {
        this.framebuffer.resize(width, height)
    }

    override fun close() {
        this.framebuffer.delete()
    }

    companion object {
        @JvmStatic
        private fun getFramebufferIdFromFramebuffer(framebuffer: Framebuffer): Int {
            val resourceManager = gpuDevice.createCommandEncoder() as GlCommandEncoder
            val colorAttachment = framebuffer.getColorAttachment() as GlTexture
            val depthAttachment = framebuffer.getDepthAttachment()

            return colorAttachment.getOrCreateFramebuffer(
                resourceManager.backend.bufferManager,
                depthAttachment
            )
        }
    }
}
