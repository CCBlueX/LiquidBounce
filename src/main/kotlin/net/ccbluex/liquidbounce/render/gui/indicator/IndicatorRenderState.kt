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

import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
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

    /**
     * Style drawn while the target is inside the viewport, so an indicator can point at what cannot be seen without
     * putting an arrow on what is already visible. Defaults to [indicatorStyle].
     */
    val inViewIndicatorStyle: IndicatorStyle get() = indicatorStyle

    val indicatorColor: Color4b

    /**
     * Where this indicator is drawn. An offscreen indicator usually only wants [IndicatorVisibility.OFFSCREEN],
     * since an arrow over something already visible is noise.
     */
    val visibleAt: Collection<IndicatorVisibility> get() = IndicatorVisibility.entries

    /**
     * How much of the screen the arrows keep clear, as a fraction of the half-size on each axis: `0.1` keeps them
     * 10% away from every border, which shrinks the frame at the same shape as the screen.
     */
    val edgeInset: Float get() = 0.0F

    /**
     * Extent of the tag in the space [extractTag] draws in, i.e. relative to the point the pose is translated to.
     *
     * It is measured before the tag is drawn, so the renderer can keep the whole tag on screen instead of only the
     * arrow. Where the origin sits inside this box is up to the tag: a box centred on the origin is centred on the
     * anchor, one that starts at zero hangs below it.
     */
    fun measureTag(): BoundingBox2f

    /**
     * Draws the tag around the origin. The pose is already translated so that [measureTag] sits on the far side of
     * the anchor from the arrow, and inside the screen.
     */
    fun extractTag(ctx: GuiGraphicsExtractor)

}
