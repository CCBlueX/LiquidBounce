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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Draws the arrows and tags modules submit for the current frame.
 *
 * Collection works the way [net.ccbluex.liquidbounce.render.gui.ItemStackListRenderer] does it: producers submit from
 * their own [OverlayRenderEvent] handler, and this one, registered to run last, draws and then clears the frame.
 *
 * An arrow sits on the projection of its target, and is pulled onto the screen border once that projection leaves the
 * screen, or once the target is behind the camera. Its tag is drawn one step further inwards.
 */
object IndicatorRenderer : EventListener {

    /**
     * Gap between the arrow and its tag, on top of the arrow's own length.
     */
    private const val TAG_GAP = 2.0F

    /**
     * Keeps the arrow's tip off the exact screen boundary, which is one pixel past the last drawable one.
     */
    private const val BORDER_MARGIN = 1.0F

    /**
     * Upper bound for [IndicatorRenderState.edgeInset], so a config file cannot collapse the frame to a point.
     */
    private const val MAX_EDGE_INSET = 0.9F

    private val submitted = ArrayList<IndicatorRenderState>()
    private val ordered = ArrayList<IndicatorRenderState>()

    private val order: Comparator<IndicatorRenderState> = Comparator.comparingInt { it.priority }

    fun submit(state: IndicatorRenderState) {
        submitted += state
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent>(READ_FINAL_STATE) { event ->
        if (submitted.isEmpty()) {
            return@handler
        }

        try {
            ordered.clear()
            ordered += submitted
            // Ascending, so the highest priority is submitted last and ends up covering the rest.
            // TODO: this only orders arrows that share a pipeline; GuiRenderer groups elements by pipeline before
            //  submission order applies. Split into strata per priority group if mixed arrow styles ever need it.
            ordered.sortWith(order)

            for (state in ordered) {
                draw(event.context, state)
            }
        } finally {
            submitted.clear()
        }
    }

    private fun draw(ctx: GuiGraphicsExtractor, state: IndicatorRenderState) {
        val centerX = mc.window.guiScaledWidth * 0.5F
        val centerY = mc.window.guiScaledHeight * 0.5F

        val projected = WorldToScreen.calculateScreenPos(state.pos)
        val offset = projected?.let { Vector2f(it.x - centerX, it.y - centerY) }
        // A target behind the camera has no projection to test, and is never inside the viewport.
        val inView = offset != null && abs(offset.x) <= centerX && abs(offset.y) <= centerY

        if (inView && !state.showInView) {
            return
        }
        if (!inView && !state.showOffscreen) {
            return
        }

        // The frame the arrows are kept in. Shrinking it by a fraction keeps its shape and can never swallow the
        // whole screen, which an absolute margin would once it outgrows the half-size.
        val halfWidth = centerX * (1.0F - state.edgeInset.coerceIn(0.0F, MAX_EDGE_INSET))
        val halfHeight = centerY * (1.0F - state.edgeInset.coerceIn(0.0F, MAX_EDGE_INSET))

        val anchor: Vector2f
        val outward: Vector2f

        if (offset != null) {
            anchor = IndicatorGeometry.clampToBorder(offset.x, offset.y, halfWidth, halfHeight, BORDER_MARGIN)
            // A target exactly at the screen center has no direction to point at, the arrow then points straight
            // down. Mutates offset, which is not needed in its raw form past this point.
            outward = if (offset.lengthSquared() == 0.0F) Vector2f(0.0F, 1.0F) else offset.normalize()
        } else {
            // Behind the camera, where calculateScreenPos gives up. calculateScreenOffset still carries the
            // direction, but not a usable length: it shrinks the further behind the target is, which would drag the
            // arrow towards the center instead of onto the border.
            val direction = WorldToScreen.calculateScreenOffset(state.pos) ?: return
            if (direction.lengthSquared() == 0.0F) return
            direction.normalize()

            anchor = IndicatorGeometry.borderPoint(direction.x, direction.y, halfWidth, halfHeight, BORDER_MARGIN)
            outward = direction
        }

        val style = if (inView) state.inViewIndicatorStyle else state.indicatorStyle
        val anchorX = centerX + anchor.x
        val anchorY = centerY + anchor.y
        val tagDistance = style.length + TAG_GAP

        ctx.pose().withPush {
            translate(anchorX - outward.x * tagDistance, anchorY - outward.y * tagDistance)
            state.extractTag(ctx, outward.x, outward.y)
        }

        ctx.pose().withPush {
            // The style's tip sits at its local (0, length), so back the pose off by one arrow length to land the tip
            // on the anchor. rotate(angle) maps (0, 1) to (-sin, cos).
            translate(anchorX - outward.x * style.length, anchorY - outward.y * style.length)
            rotate(atan2(-outward.x, outward.y))
            style.extract(ctx, state.indicatorColor)
        }
    }

}
