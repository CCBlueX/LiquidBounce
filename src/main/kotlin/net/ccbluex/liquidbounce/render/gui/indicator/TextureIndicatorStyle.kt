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

package net.ccbluex.liquidbounce.render.gui.indicator

import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup

/**
 * A textured arrow, laid out like [TriangleIndicatorStyle]: its base at the local origin and its tip at
 * `(0, size)`.
 *
 * The texture is sampled over `(u1, v1)` to `(u2, v2)`, which spans the whole texture by default. An arrow whose
 * image is stored tip up, as most of them are, swaps the corners to turn it around.
 */
data class TextureIndicatorStyle(
    val textureSetup: TextureSetup,
    val size: Float,
    val u1: Float = 0.0F,
    val v1: Float = 0.0F,
    val u2: Float = 1.0F,
    val v2: Float = 1.0F,
) : IndicatorStyle {

    override val length: Float
        get() = size

    override fun extract(ctx: GuiGraphicsExtractor, color: Color4b) {
        ctx.drawTexQuad(
            textureSetup,
            -size * 0.5F, 0.0F, size * 0.5F, size,
            u1 = u1, v1 = v1, u2 = u2, v2 = v2,
            argb = color.argb,
            pipeline = ClientRenderPipelines.GUI.TexQuadNoCull,
        )
    }

}
