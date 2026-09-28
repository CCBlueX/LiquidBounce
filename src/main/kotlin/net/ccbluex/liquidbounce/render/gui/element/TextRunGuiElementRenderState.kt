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

package net.ccbluex.liquidbounce.render.gui.element

import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage
import net.ccbluex.liquidbounce.utils.collection.GenericPools
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import org.joml.Matrix3x2f

/**
 * A run of textured quads that share one pose, pipeline and texture, submitted as a single GUI element.
 *
 * Batching keeps a text of N glyphs at one element and one pooled pose instead of N of each, and it
 * keeps [bounds] null so glyphs stay out of the per element intersection checks.
 */
class TextRunGuiElementRenderState(
    private val pipeline: RenderPipeline,
    private val page: GlyphPage,
    private val pose: Matrix3x2f,
    private val scissorArea: ScreenRectangle?,
) : RecyclableGuiElementRenderState {

    private val geometry = GenericPools.FLOAT_LIST.borrow()
    private val colors = GenericPools.INT_LIST.borrow()

    private val textureSetup = page.texture.textureSetup

    /**
     * Whether a quad can still join this run.
     *
     * The atlas page is compared by identity because a texture setup is rebuilt from its texture on
     * every access, which would make it a value that is never the same twice.
     */
    fun accepts(page: GlyphPage, scissorArea: ScreenRectangle?): Boolean =
        this.page === page && this.scissorArea == scissorArea

    /**
     * @param quad x0, y0, x1, y1, u1, v1, u2, v2
     */
    fun addQuad(quad: FloatArray, argb: Int) {
        geometry.addElements(geometry.size, quad, 0, FLOATS_PER_QUAD)
        colors.add(argb)
    }

    override fun buildVertices(vertices: VertexConsumer) {
        for (i in 0 until colors.size) {
            val index = i * FLOATS_PER_QUAD
            val argb = colors.getInt(i)

            vertices.addVertexWith2DPose(pose, geometry.getFloat(index), geometry.getFloat(index + 1))
                .setUv(geometry.getFloat(index + 4), geometry.getFloat(index + 5)).setColor(argb)
            vertices.addVertexWith2DPose(pose, geometry.getFloat(index), geometry.getFloat(index + 3))
                .setUv(geometry.getFloat(index + 4), geometry.getFloat(index + 7)).setColor(argb)
            vertices.addVertexWith2DPose(pose, geometry.getFloat(index + 2), geometry.getFloat(index + 3))
                .setUv(geometry.getFloat(index + 6), geometry.getFloat(index + 7)).setColor(argb)
            vertices.addVertexWith2DPose(pose, geometry.getFloat(index + 2), geometry.getFloat(index + 1))
                .setUv(geometry.getFloat(index + 6), geometry.getFloat(index + 5)).setColor(argb)
        }
    }

    override fun recycle() {
        Pools.Mat3x2f.recycle(pose)
        GenericPools.FLOAT_LIST.recycle(geometry)
        GenericPools.INT_LIST.recycle(colors)
    }

    override fun pipeline(): RenderPipeline = pipeline

    override fun textureSetup(): TextureSetup = textureSetup

    override fun scissorArea(): ScreenRectangle? = scissorArea

    override fun bounds(): ScreenRectangle? = null

    companion object {
        private const val FLOATS_PER_QUAD = 8
    }

}
