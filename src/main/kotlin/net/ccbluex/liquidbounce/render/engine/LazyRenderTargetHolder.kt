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

package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.clearColor
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth

/**
 * A holder for a RenderTarget that initializes it lazily and handles resizing.
 */
class LazyRenderTargetHolder(
    val name: String,
    @JvmField val useDepth: Boolean
) : AutoCloseable {
    var raw: RenderTarget? = null
        private set

    /**
     * Destroys the buffers and releases the RenderTarget.
     */
    override fun close() {
        this.raw?.destroyBuffers()
        this.raw = null
    }

    /**
     * Initializes the RenderTarget if needed, or resizes/clears it if it already exists, then returns it.
     */
    fun initAndGet(): RenderTarget {
        val width = mc.window.width
        val height = mc.window.height

        val current = this.raw

        if (current == null) {
            val new = TextureTarget(name, width, height, useDepth)
            this.raw = new
            return new
        } else {
            if (width != current.width || height != current.height) {
                current.resize(width, height) // Resizing includes clearing the framebuffer
            } else if (useDepth) {
                current.clearColorAndDepth()
            } else {
                current.colorTexture!!.clearColor()
            }
            return current
        }
    }
}
