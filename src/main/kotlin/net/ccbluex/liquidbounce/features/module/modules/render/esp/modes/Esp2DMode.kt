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
 */
package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object Esp2DMode : EspMode("2D") {

    private val outline by boolean("Outline", true)
    private val expand by float("Expand", 0.05f, 0f..0.5f)

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val entitiesWithBoxes = RenderedEntities.map { entity ->
            val dimensions = entity.getDimensions(entity.pose)
            val d = dimensions.width.toDouble() / 2.0
            val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(expand.toDouble())
            val pos = entity.interpolateCurrentPosition(event.tickDelta)
            val boxAtPos = box.offset(pos)
            entity to boxAtPos
        }

        renderEnvironmentForGUI {
            for ((entity, box) in entitiesWithBoxes) {
                val color = getColor(entity)
                val baseColor = color.with(a = 50)
                val outlineColor = color.with(a = 100)

                val corners = listOf(
                    Vec3d(box.minX, box.minY, box.minZ),
                    Vec3d(box.minX, box.minY, box.maxZ),
                    Vec3d(box.minX, box.maxY, box.minZ),
                    Vec3d(box.minX, box.maxY, box.maxZ),
                    Vec3d(box.maxX, box.minY, box.minZ),
                    Vec3d(box.maxX, box.minY, box.maxZ),
                    Vec3d(box.maxX, box.maxY, box.minZ),
                    Vec3d(box.maxX, box.maxY, box.maxZ)
                )

                val projected = corners.mapNotNull { pos -> WorldToScreen.calculateScreenPos(pos) }
                if (projected.isEmpty()) {
                    continue
                }

                val minX = projected.minOf { it.x }
                val maxX = projected.maxOf { it.x }
                val minY = projected.minOf { it.y }
                val maxY = projected.maxOf { it.y }
                val minZ = projected.minOf { it.z } // TODO: Handle Z-index correctly
                val rectWidth = (maxX - minX).toInt()
                val rectHeight = (maxY - minY).toInt()

                with(DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)) {
                    with(matrices) {
                        translate(minX, minY, minZ)
                        fill(0, 0, rectWidth, rectHeight, baseColor.toARGB())
                        if (outline) {
                            drawHorizontalLine(0, rectWidth, 0, outlineColor.toARGB())
                            drawHorizontalLine(0, rectWidth, rectHeight, outlineColor.toARGB())
                            drawVerticalLine(0, 0, rectHeight, outlineColor.toARGB())
                            drawVerticalLine(rectWidth, 0, rectHeight, outlineColor.toARGB())
                        }
                    }
                }
            }
        }
    }

}
