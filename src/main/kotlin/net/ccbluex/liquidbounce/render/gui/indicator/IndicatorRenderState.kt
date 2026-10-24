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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.phys.Vec3

/**
 * One arrow with its tag, submitted for a single frame and drawn by [IndicatorRenderer].
 */
interface IndicatorRenderState {

    /**
     * World position the arrow points at.
     */
    val pos: Vec3

    /**
     * Higher values are drawn later, so they cover lower ones.
     */
    val priority: Int get() = 0

    val indicatorStyle: IndicatorStyle

    val indicatorColor: Color4b

    /**
     * Whether the indicator is drawn while its target is inside the viewport as well. An offscreen indicator
     * usually only wants the targets that cannot be seen.
     */
    val showInView: Boolean get() = true

    /**
     * How much of the screen the arrows keep clear, as a fraction of the half-size on each axis: `0.1` keeps them
     * 10% away from every border, which shrinks the frame at the same shape as the screen.
     */
    val edgeInset: Float get() = 0.0F

    /**
     * Draws the tag, which is whatever an indicator wants to show next to its arrow.
     *
     * The pose is already translated to the tag anchor, one arrow length inside the screen from the arrow, so draw
     * around the origin. ([outwardX], [outwardY]) is the unit direction the arrow points in, away from the screen
     * center: content laid out on the opposite side of the origin keeps clear of the arrow and stays on screen.
     */
    fun extractTag(ctx: GuiGraphicsExtractor, outwardX: Float, outwardY: Float)

}
