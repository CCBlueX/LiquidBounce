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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * The drawing helpers of `net.ccbluex.liquidbounce.render` take Kotlin lambdas (`renderEnvironment`,
 * `withPositionRelativeToCamera`) or context parameters (text). These are the same calls in a form
 * Java can make, with the same parameters. Coordinates are world coordinates.
 */
object Render {

    /**
     * @see net.ccbluex.liquidbounce.render.drawBox
     */
    @JvmStatic
    @JvmOverloads
    fun box(
        event: WorldRenderEvent,
        box: AABB,
        fill: Color4b? = Color4b.TRANSPARENT,
        outline: Color4b? = Color4b.TRANSPARENT,
        faceVertices: Int = -1,
        outlineVertices: Int = -1,
        noDepthTest: Boolean = true,
    ) {
        event.environment.withPositionRelativeToCamera {
            drawBox(box, fill, outline, faceVertices, outlineVertices, noDepthTest)
        }
    }

    /** [box] with the block's own shape at [pos], as the ESPs draw it. */
    @JvmStatic
    @JvmOverloads
    fun blockBox(
        event: WorldRenderEvent,
        pos: BlockPos,
        fill: Color4b? = Color4b.TRANSPARENT,
        outline: Color4b? = Color4b.TRANSPARENT,
        faceVertices: Int = -1,
        outlineVertices: Int = -1,
        noDepthTest: Boolean = true,
    ) {
        event.environment.withPositionRelativeToCamera(pos) {
            drawBox(pos.outlineBox, fill, outline, faceVertices, outlineVertices, noDepthTest)
        }
    }

    @JvmStatic
    fun line(event: WorldRenderEvent, from: Vec3, to: Vec3, argb: Int) {
        event.environment.withPositionRelativeToCamera {
            drawLine(from, to, argb)
        }
    }

    /**
     * Draws [text] with the client's font at [x]/[y] in GUI pixels, [scale] 1 being vanilla's size.
     *
     * @return the width drawn
     */
    @JvmStatic
    @JvmOverloads
    fun text(
        event: OverlayRenderEvent,
        text: Component,
        x: Float,
        y: Float,
        color: Color4b = Color4b.WHITE,
        shadow: Boolean = false,
        scale: Float = 1f,
        horizontalAnchor: HorizontalAnchor? = null,
        verticalAnchor: VerticalAnchor? = null,
    ): Float {
        val renderer = FontManager.FONT_RENDERER
        val processed = renderer.process(text, color)
        return with(event.context) {
            renderer.draw(processed) {
                this.x = x
                this.y = y
                this.shadow = shadow
                this.scale = scale * renderer.scaleToVanillaFont
                this.horizontalAnchor = horizontalAnchor
                this.verticalAnchor = verticalAnchor
            }
        }
    }

    /** Width [text] takes at scale 1, in GUI pixels. */
    @JvmStatic
    fun textWidth(text: Component): Float {
        val renderer = FontManager.FONT_RENDERER
        return renderer.getStringWidth(renderer.process(text)) * renderer.scaleToVanillaFont
    }

}
