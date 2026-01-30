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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
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
import net.minecraft.world.phys.AABB

object Esp2DMode : EspMode("2D") {

    object Outline : ToggleableValueGroup(this, "Outline", true) {
        val thickness by float("Thickness", 1f, 1f..9f, "px")
    }

    object Border : ToggleableValueGroup(this, "Border", true) {
        val thickness by float("Thickness", 1f, 1f..9f, "px")
    }

    private val expand by float("Expand", 0.05f, 0f..0.5f)
    private val fill by boolean("Fill", true)

    object HealthBar : ToggleableValueGroup(this, "HealthBar", true) {
        val spacing by float("Spacing", 2f, 0f..32f, "px")
    }

    init {
        tree(Outline)
        tree(Border)
        tree(HealthBar)
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        for (entity in RenderedEntities) {
            if (!shouldRender(entity)) continue

            val dimensions = entity.getDimensions(entity.pose)
            val d = dimensions.width.toDouble() / 2.0
            val boxNoOffset = AABB(-d, 0.0, -d, d, dimensions.height.toDouble(), d).inflate(expand.toDouble())
            val pos = entity.interpolateCurrentPosition(event.tickDelta)
            val box = boxNoOffset.move(pos)

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
            val rectWidth = maxX - minX
            val rectHeight = maxY - minY

            val guiScaleFactor = mc.options.guiScale().get()
            val outlineThickness = Outline.thickness / guiScaleFactor
            val borderThickness = Border.thickness / guiScaleFactor

            with(event.context) {
                pose().withPush {
                    translate(minX, minY)

                    if (fill) {
                        drawQuad(0.0f, 0.0f, rectWidth, rectHeight, fillColor = baseColor)
                    }

                    if (Outline.enabled) {
                        if (Border.enabled) {
                            drawHorizontalLine(
                                x1 = -outlineThickness / 2 - borderThickness,
                                x2 = rectWidth + outlineThickness / 2 + borderThickness,
                                y = -outlineThickness / 2 - borderThickness,
                                outlineThickness + 2 * borderThickness, black
                            )
                            drawVerticalLine(
                                x = -outlineThickness / 2 - borderThickness,
                                y1 = -outlineThickness / 2 - borderThickness,
                                y2 = rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black
                            )
                            drawHorizontalLine(
                                x1 = -outlineThickness / 2 - borderThickness,
                                x2 = rectWidth + outlineThickness / 2 + borderThickness,
                                y = rectHeight - outlineThickness / 2 - borderThickness,
                                outlineThickness + 2 * borderThickness, black
                            )
                            drawVerticalLine(
                                x = rectWidth - outlineThickness / 2 - borderThickness,
                                y1 = -outlineThickness / 2 - borderThickness,
                                y2 = rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black
                            )
                        }

                        drawHorizontalLine(
                            x1 = -outlineThickness / 2,
                            x2 = rectWidth + outlineThickness / 2,
                            y = -outlineThickness / 2,
                            outlineThickness, outlineColor
                        )
                        drawHorizontalLine(
                            x1 = -outlineThickness / 2,
                            x2 = rectWidth + outlineThickness / 2,
                            y = rectHeight - outlineThickness / 2,
                            outlineThickness, outlineColor
                        )
                        drawVerticalLine(
                            x = -outlineThickness / 2,
                            y1 = -outlineThickness / 2,
                            y2 = rectHeight + outlineThickness / 2,
                            outlineThickness, outlineColor
                        )
                        drawVerticalLine(
                            x = rectWidth - outlineThickness / 2,
                            y1 = -outlineThickness / 2,
                            y2 = rectHeight + outlineThickness / 2,
                            outlineThickness, outlineColor
                        )

                        if (Border.enabled) {
                            translate(-2 * borderThickness, 0.0f)
                        }
                    }

                    translate(-HealthBar.spacing / guiScaleFactor - outlineThickness, 0.0f)

                    if (HealthBar.enabled) {
                        val actualHealth = entity.getActualHealth()
                        val maxHealth = entity.maxHealth.coerceAtLeast(1f) // prevent division by zero
                        val healthPercentage = (actualHealth / maxHealth).coerceIn(0f..1f)

                        val healthColor = Color4b.RED
                            .interpolateTo(Color4b.GREEN, healthPercentage.toDouble())
                        val healthHeight = rectHeight * healthPercentage

                        if (Border.enabled) {
                            drawVerticalLine(
                                x = -outlineThickness / 2 - borderThickness,
                                y1 = -outlineThickness / 2 - borderThickness,
                                y2 = rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black
                            )
                        }

                        drawVerticalLine(
                            x = -outlineThickness / 2,
                            y1 = rectHeight - healthHeight - outlineThickness / 2,
                            y2 = rectHeight + outlineThickness / 2,
                            outlineThickness, healthColor
                        )
                    }
                }
            }
        }
    }

}
