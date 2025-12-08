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
import net.ccbluex.liquidbounce.render.drawHorizontalLine
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawVerticalLine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.aiming.utils.edgePoints
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.math.Box

object Esp2DMode : EspMode("2D") {

    private val outline by boolean("Outline", true)
    private val border by boolean("Border", true)
    private val expand by float("Expand", 0.05f, 0f..0.5f)
    private val fill by boolean("Fill", true)
    private val healthBar by boolean("HealthBar", true)

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        for (entity in RenderedEntities) {
            if (!shouldRender(entity)) continue

            val dimensions = entity.getDimensions(entity.pose)
            val d = dimensions.width.toDouble() / 2.0
            val boxNoOffset = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(expand.toDouble())
            val pos = entity.interpolateCurrentPosition(event.tickDelta)
            val box = boxNoOffset.offset(pos)

            val projected = box.edgePoints.mapNotNull { pos -> WorldToScreen.calculateScreenPos(pos) }
            if (projected.isEmpty()) {
                continue
            }

            val color = getColor(entity)
            val baseColor = color.with(a = 50)
            val outlineColor = color.with(a = 255)
            val black = Color4b.BLACK

            val minX = projected.minOf { it.x }
            val maxX = projected.maxOf { it.x }
            val minY = projected.minOf { it.y }
            val maxY = projected.maxOf { it.y }
            var rectWidth = (maxX - minX)
            var rectHeight = (maxY - minY)

            with(event.context) {
                matrices.withPush {
                    translate(minX, minY)

                    if (fill) {
                        drawQuad(0.0f, 0.0f, rectWidth, rectHeight, fillColor = baseColor)
                    }

                    if (outline) {
                        if (border) {
                            drawHorizontalLine(0.0f, rectWidth, 0.0f, 1.5f, black)
                            drawVerticalLine(0.0f, 0.0f, rectHeight, 1.5f, black)
                            drawHorizontalLine(0.0f, rectWidth, rectHeight, 1.5f, black)
                            drawVerticalLine(rectWidth, 0.0f, rectHeight + 1.5f, 1.5f, black)

                            translate(0.5f, 0.5f)
                        }

                        drawHorizontalLine(0.0f, rectWidth, 0.0f, 0.5f, outlineColor)
                        drawHorizontalLine(0.0f, rectWidth, rectHeight, 0.5f, outlineColor)
                        drawVerticalLine(0.0f, 0.0f, rectHeight, 0.5f, outlineColor)
                        drawVerticalLine(rectWidth, 0.0f, rectHeight + 0.5f, 0.5f, outlineColor)

                        if (border) {
                            translate(-0.5f, -0.5f)
                        }
                    }

                    if (healthBar) {
                        val actualHealth = entity.getActualHealth()
                        val maxHealth = entity.maxHealth.coerceAtLeast(1f) // prevent division by zero
                        val healthPercentage = (actualHealth / maxHealth).coerceIn(0f..1f)

                        val healthColor = Color4b.RED
                            .interpolateTo(Color4b.GREEN, healthPercentage.toDouble())
                        val healthHeight = rectHeight * healthPercentage

                        translate(-3.0f, 0.0f)

                        if (border) {
                            drawVerticalLine(0.0f, 0.0f, rectHeight + 1.5f, 1.5f, black)
                        }
                        drawVerticalLine(0.5f, rectHeight + 1f, rectHeight - healthHeight + 0.5f, 0.5f, healthColor)
                    }
                }
            }
        }
    }

}
