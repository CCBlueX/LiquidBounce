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
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Drawing from inside [WorldRenderEvent] (in the world, coordinates are world coordinates) and
 * [OverlayRenderEvent] (on the screen, in GUI pixels). Nothing here works outside those handlers.
 */
object Render {

    /**
     * @param fill null or transparent for an outline only
     * @param outline null or transparent for faces only
     * @param throughWalls draw over what is in front of it
     */
    @JvmStatic
    @JvmOverloads
    fun box(
        event: WorldRenderEvent,
        box: AABB,
        fill: Color4b?,
        outline: Color4b?,
        throughWalls: Boolean = true,
    ) {
        event.environment.withPositionRelativeToCamera {
            drawBox(box, fill, outline, noDepthTest = throughWalls)
        }
    }

    /** [box] with the block's own shape, as ESPs draw it. */
    @JvmStatic
    @JvmOverloads
    fun blockBox(
        event: WorldRenderEvent,
        pos: BlockPos,
        fill: Color4b?,
        outline: Color4b?,
        throughWalls: Boolean = true,
    ) {
        event.environment.withPositionRelativeToCamera(pos) {
            drawBox(pos.outlineBox, fill, outline, noDepthTest = throughWalls)
        }
    }

    @JvmStatic
    fun line(event: WorldRenderEvent, from: Vec3, to: Vec3, color: Color4b) {
        event.environment.withPositionRelativeToCamera {
            drawLine(from, to, color.argb)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun rect(
        event: OverlayRenderEvent,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        fill: Color4b?,
        outline: Color4b? = null,
    ) {
        event.context.drawQuad(x1, y1, x2, y2, fill, outline)
    }

    /**
     * Draws [text] with the client's font, top-left at [x]/[y].
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
        shadow: Boolean = true,
        scale: Float = 1f,
    ): Float {
        val renderer = FontManager.FONT_RENDERER
        val processed = renderer.process(text, color)
        return with(event.context) {
            renderer.draw(processed) {
                this.x = x
                this.y = y
                this.shadow = shadow
                this.scale = scale * renderer.scaleToVanillaFont
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
