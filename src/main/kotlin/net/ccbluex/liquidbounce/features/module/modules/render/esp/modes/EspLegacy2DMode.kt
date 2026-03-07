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
package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import com.mojang.math.Axis
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Rect
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

object EspLegacy2DMode : EspMode("Legacy2D") {

    private val scale by float("Scale", 0.1f, 0.02f..0.3f)
    private val yOffset by float("YOffset", 0f, -1f..1f)
    private val backgroundAlpha by int("BackgroundAlpha", 150, 0..255)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            for (entity in RenderedEntities) {
                if (!shouldRender(entity)) continue

                val pos = entity.interpolateCurrentPosition(event.partialTicks).add(0.0, yOffset.toDouble(), 0.0)
                val color = getColor(entity).argb
                val backgroundColor = Color4b.BLACK.with(a = backgroundAlpha).argb

                drawLegacy2D(entity, pos, color, backgroundColor)
            }
        }
    }

    private fun WorldRenderEnvironment.drawLegacy2D(entity: LivingEntity, pos: Vec3, color: Int, backgroundColor: Int) {
        withPositionRelativeToCamera(pos) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()))
            poseStack.scale(-scale, -scale, scale)

            drawRects(color, TOP_FOREGROUND_RECTS)
            drawRects(backgroundColor, TOP_BACKGROUND_RECTS)

            val entityHeight = entity.boundingBox.ysize
            poseStack.translate(0.0, 21.0 - entityHeight * 12.0, 0.0)

            drawRects(color, BOTTOM_FOREGROUND_RECTS)
            drawRects(backgroundColor, BOTTOM_BACKGROUND_RECTS)
        }
    }

    private fun WorldRenderEnvironment.drawRects(color: Int, rects: Array<Rect>) {
        drawCustomMesh(ClientRenderPipelines.Quads) { pose ->
            for (rect in rects) {
                addVertex(pose, rect.x2, rect.y1, 0.0f).setColor(color)
                addVertex(pose, rect.x1, rect.y1, 0.0f).setColor(color)
                addVertex(pose, rect.x1, rect.y2, 0.0f).setColor(color)
                addVertex(pose, rect.x2, rect.y2, 0.0f).setColor(color)
            }
        }
    }

    private val TOP_FOREGROUND_RECTS = arrayOf(
        Rect(-7f, 2f, -4f, 3f),
        Rect(4f, 2f, 7f, 3f),
        Rect(-7f, 0.5f, -6f, 3f),
        Rect(6f, 0.5f, 7f, 3f),
    )

    private val TOP_BACKGROUND_RECTS = arrayOf(
        Rect(-7f, 3f, -4f, 3.3f),
        Rect(4f, 3f, 7f, 3.3f),
        Rect(-7.3f, 0.5f, -7f, 3.3f),
        Rect(7f, 0.5f, 7.3f, 3.3f),
    )

    private val BOTTOM_FOREGROUND_RECTS = arrayOf(
        Rect(4f, -20f, 7f, -19f),
        Rect(-7f, -20f, -4f, -19f),
        Rect(6f, -20f, 7f, -17.5f),
        Rect(-7f, -20f, -6f, -17.5f),
    )

    private val BOTTOM_BACKGROUND_RECTS = arrayOf(
        Rect(7f, -20f, 7.3f, -17.5f),
        Rect(-7.3f, -20f, -7f, -17.5f),
        Rect(4f, -20.3f, 7.3f, -20f),
        Rect(-7.3f, -20.3f, -4f, -20f),
    )

}
