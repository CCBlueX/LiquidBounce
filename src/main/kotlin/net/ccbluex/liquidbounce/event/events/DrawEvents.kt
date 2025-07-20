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
 *
 */

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.Camera
import net.minecraft.client.util.math.MatrixStack

@Nameable("gameRender")
object GameRenderEvent : Event()

@Nameable("screenRender")
class ScreenRenderEvent(val context: DrawContext, val partialTicks: Float) : Event()

@Nameable("worldRender")
class WorldRenderEvent(val matrixStack: MatrixStack, val camera: Camera, val partialTicks: Float) : Event()

/**
 * Sometimes, modules might want to contribute something to the glow framebuffer. They can hook this event
 * in order to do so.
 *
 * Note: After writing to the outline framebuffer [markDirty] must be called.
 */
@Nameable("drawOutlines")
class DrawOutlinesEvent(
    val matrixStack: MatrixStack,
    val camera: Camera,
    val partialTicks: Float,
    val type: OutlineType,
) : Event() {
    var dirtyFlag: Boolean = false
        private set

    /**
     * Called when the framebuffer was edited.
     */
    fun markDirty() {
        this.dirtyFlag = true
    }

    enum class OutlineType {
        INBUILT_OUTLINE,
        MINECRAFT_GLOW
    }
}

@Nameable("overlayRender")
class OverlayRenderEvent(val context: DrawContext, val tickDelta: Float) : Event()
