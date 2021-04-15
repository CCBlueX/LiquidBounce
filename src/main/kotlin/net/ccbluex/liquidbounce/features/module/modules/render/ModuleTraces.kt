/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.aiming.shouldBeShown
import net.minecraft.entity.Entity
import java.awt.Color
import kotlin.math.sqrt

object ModuleTraces : Module("Traces", Category.RENDER) {

    private val modes = choices("ColorMode", "Distance") {
        DistanceColor
        StaticColor
        RainbowColor
    }

    private object DistanceColor : Choice("Distance", modes) {
        val useViewDistance by boolean("UseViewDistance", true)
        val customViewDistance by float("CustomViewDistance", 128.0F, 1.0F..512.0F)
    }

    private object StaticColor : Choice("Static", modes) {
        val color by color("Color", Color4b(0, 160, 255, 255))
    }

    private object RainbowColor : Choice("Rainbow", modes)

    val renderHandler = handler<EngineRenderEvent> { event ->
        val useDistanceColor = DistanceColor.isActive

        val baseColor = when {
            RainbowColor.isActive -> rainbow()
            StaticColor.isActive -> StaticColor.color
            else -> null
        }

        val viewDistance =
            (if (DistanceColor.useViewDistance) mc.options.viewDistance.toFloat() else DistanceColor.customViewDistance) * 16 * sqrt(
                2.0
            )
        val player = mc.player!!
        val filteredEntities = world.entities.filter(this::shouldRenderTrace)
        val camera = mc.gameRenderer.camera

        val renderTask = ColoredPrimitiveRenderTask(filteredEntities.size * 2, PrimitiveType.Lines)

        val eyeVector = Vec3(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat()) + Vec3(camera.pos) + Vec3(0.0, 0.0, -1.0)

        for (entity in filteredEntities) {
            val dist = player.distanceTo(entity) * 2.0

            val color = if (useDistanceColor) {
                Color4b(
                    Color.getHSBColor(
                        (dist.coerceAtMost(viewDistance) / viewDistance).toFloat() * (120.0f / 360.0f),
                        1.0f,
                        1.0f
                    )
                )
            } else {
                baseColor!!
            }

            val x = (entity.lastRenderX + (entity.x - entity.lastRenderX) * event.tickDelta)
            val y = (entity.lastRenderY + (entity.y - entity.lastRenderY) * event.tickDelta)
            val z = (entity.lastRenderZ + (entity.z - entity.lastRenderZ) * event.tickDelta) - 1.0

            val v0 = renderTask.vertex(eyeVector, color)
            val v1 = renderTask.vertex(Vec3(x, y, z), color)
            val v2 = renderTask.vertex(Vec3(x, y + entity.height, z), color)

            renderTask.index(v0)
            renderTask.index(v1)
            renderTask.index(v1)
            renderTask.index(v2)
        }

        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER_WITHOUT_BOBBING, renderTask)
    }

    @JvmStatic
    fun shouldRenderTrace(entity: Entity) = entity.shouldBeShown()
}
