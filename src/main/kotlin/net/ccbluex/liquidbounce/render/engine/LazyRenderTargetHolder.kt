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

import com.mojang.renderpearl.api.GpuFormat
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import java.util.function.Supplier

/**
 * A holder for a RenderTarget that initializes it lazily and handles resizing.
 */
class LazyRenderTargetHolder private constructor(
    val name: String,
    val colorFormat: GpuFormat = GpuFormat.RGBA8_UNORM,
    val depthFormat: GpuFormat? = GpuFormat.D32_FLOAT,
) : Supplier<RenderTarget?>, AutoCloseable {

    constructor(name: String, useDepth: Boolean) :
        this(name, GpuFormat.RGBA8_UNORM, if (useDepth) GpuFormat.D32_FLOAT else null)

    /**
     * The targets used lately, most recent first. Views of background clients draw at another size than the
     * window in the same frame, so a holder keeps one target per size instead of resizing back and forth.
     */
    private val targets = ArrayList<RenderTarget>(SIZES)

    override fun get(): RenderTarget? = targets.firstOrNull()

    /**
     * Destroys the buffers and releases the RenderTargets.
     */
    override fun close() {
        targets.forEach(RenderTarget::destroyBuffers)
        targets.clear()
    }

    /**
     * Initializes a RenderTarget of the size of the target drawn to now, or clears the one there is.
     */
    fun initAndGet(): RenderTarget {
        val main = mc.gameRenderer.mainRenderTarget()
        return initAndGet(main.width, main.height)
    }

    /**
     * Initializes the RenderTarget with the requested size.
     */
    fun initAndGet(width: Int, height: Int): RenderTarget {
        require(width > 0 && height > 0) { "RenderTarget dimensions must be positive: ${width}x$height" }

        val index = targets.indexOfFirst { it.width == width && it.height == height }
        val target = when {
            index >= 0 -> targets.removeAt(index).also { it.clearColorAndDepth() }
            targets.size < SIZES -> TextureTarget(name, width, height, colorFormat, depthFormat)
            // Resizing includes clearing the framebuffer
            else -> targets.removeAt(targets.lastIndex).also { it.resize(width, height) }
        }
        targets.add(0, target)
        return target
    }

    private companion object {
        const val SIZES = 2
    }

}
