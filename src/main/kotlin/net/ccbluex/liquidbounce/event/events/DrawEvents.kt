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

package net.ccbluex.liquidbounce.event.events

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.event.Event
import net.minecraft.client.Camera
import net.minecraft.client.gui.GuiGraphics

@Tag("gameRender")
object GameRenderEvent : Event()

@Tag("screenRender")
class ScreenRenderEvent(val context: GuiGraphics, val partialTicks: Float) : Event()

@Tag("worldRender")
class WorldRenderEvent(val matrixStack: PoseStack, val camera: Camera, val partialTicks: Float) : Event()

/**
 * Sometimes, modules might want to contribute something to the glow framebuffer. They can hook this event
 * in order to do so.
 *
 * Note: After writing to the outline framebuffer [markDirty] must be called.
 */
@Tag("drawOutlines")
class DrawOutlinesEvent(
    val renderTarget: RenderTarget,
    val pose: PoseStack,
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

@Tag("overlayRender")
class OverlayRenderEvent(
    val context: GuiGraphics,
    val tickDelta: Float,
) : Event()
