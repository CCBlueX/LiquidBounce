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

package net.ccbluex.liquidbounce.render.engine

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.drawFullScreenPositionTexture
import net.ccbluex.liquidbounce.utils.render.clearColor
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer

/**
 * For [net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockESP] outline mode.
 *
 * @see net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
 */
object OutlineFramebufferHolder : MinecraftShortcuts {
    private val framebuffer = SimpleFramebuffer(
        "Outline shader",
        mc.window.framebufferWidth,
        mc.window.framebufferHeight,
        false,
    )

    private val outlineTexture get() = framebuffer.colorAttachment!!
    private val outlineTextureView get() = framebuffer.colorAttachmentView!!

    @JvmStatic
    fun prepare(): Framebuffer {
        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        if (width != framebuffer.textureWidth || height != framebuffer.textureHeight) {
            framebuffer.resize(width, height)
        } else {
            outlineTexture.clearColor(0)
        }

        return framebuffer
    }

    @JvmStatic
    var isDirty: Boolean = false

    @JvmStatic
    fun drawIfDirty(target: Framebuffer) {
        if (isDirty) {
            isDirty = false

            target.createRenderPass().use { pass ->
                pass.setPipeline(ClientRenderPipelines.Outline)
                pass.bindSampler("texture0", outlineTextureView)
                pass.drawFullScreenPositionTexture()
            }
        }
    }
}
