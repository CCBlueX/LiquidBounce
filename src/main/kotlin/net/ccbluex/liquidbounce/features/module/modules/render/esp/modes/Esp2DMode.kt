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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.drawHorizontalLine
import net.ccbluex.liquidbounce.render.drawVerticalLine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.fill
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.aiming.utils.edgePoints
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.math.Box

object Esp2DMode : EspMode("2D") {

    object Outline: ToggleableConfigurable(this, "Outline", true) {
        val thickness by int("Thickness", 1, 1..9, "px")
    }
    object Border: ToggleableConfigurable(this, "Border", true) {
        val thickness by int("Thickness", 1, 1..9, "px")
    }
    private val expand by float("Expand", 0.05f, 0f..0.5f)
    private val fill by boolean("Fill", true)
    object HealthBar: ToggleableConfigurable(this, "HealthBar", true) {
        val spacing by int("Spacing", 2, 0..32, "px")
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
            val boxNoOffset = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(expand.toDouble())
            val pos = entity.interpolateCurrentPosition(event.tickDelta)
            val box = boxNoOffset.offset(pos)

            val projected = box.edgePoints.mapNotNull { pos -> WorldToScreen.calculateScreenPos(pos) }
            if (projected.isEmpty()) {
                continue
            }

            val color = getColor(entity)
            val baseColor = color.with(a = 50).toARGB()
            val outlineColor = color.with(a = 255).toARGB()
            val black = Color4b.BLACK.toARGB()

            val minX = projected.minOf { it.x }
            val maxX = projected.maxOf { it.x }
            val minY = projected.minOf { it.y }
            val maxY = projected.maxOf { it.y }
            val minZ = projected.minOf { it.z } // TODO: Handle Z-index correctly
            var rectWidth = (maxX - minX)
            var rectHeight = (maxY - minY)

            val guiScaleFactor = mc.options.guiScale.value
            val outlineThickness = Outline.thickness.toFloat() / guiScaleFactor
            val borderThickness = Border.thickness.toFloat() / guiScaleFactor

            with(event.context) {
                matrices.withPush {
                    translate(minX, minY, minZ)

                    if (fill) {
                        fill(0f, 0f, rectWidth, rectHeight, 0f, baseColor)
                    }

                    if (Outline.enabled) {
                        if (Border.enabled) {
                            drawHorizontalLine(-outlineThickness / 2 - borderThickness,
                                rectWidth + outlineThickness / 2 + borderThickness,
                                -outlineThickness / 2 - borderThickness,
                                outlineThickness + 2 * borderThickness, black)
                            drawVerticalLine(-outlineThickness / 2 - borderThickness,
                                -outlineThickness / 2 - borderThickness,
                                rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black)
                            drawHorizontalLine(-outlineThickness / 2 - borderThickness,
                                rectWidth + outlineThickness / 2 + borderThickness,
                                rectHeight - outlineThickness / 2 - borderThickness,
                                outlineThickness + 2 * borderThickness, black)
                            drawVerticalLine(rectWidth - outlineThickness / 2 - borderThickness,
                                -outlineThickness / 2 - borderThickness,
                                rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black)
                        }

                        drawHorizontalLine(-outlineThickness / 2,
                            rectWidth + outlineThickness / 2,
                            -outlineThickness / 2,
                            outlineThickness, outlineColor)
                        drawHorizontalLine(-outlineThickness / 2,
                            rectWidth + outlineThickness / 2,
                            rectHeight - outlineThickness / 2,
                            outlineThickness, outlineColor)
                        drawVerticalLine(-outlineThickness / 2,
                            -outlineThickness / 2,
                            rectHeight,
                            outlineThickness, outlineColor)
                        drawVerticalLine(rectWidth - outlineThickness / 2,
                            -outlineThickness / 2,
                            rectHeight,
                            outlineThickness, outlineColor)

                        if (Border.enabled) {
                            translate(-2 * borderThickness, 0.0f, 0.0f)
                        }
                    }
                    translate(-HealthBar.spacing.toFloat() / guiScaleFactor - outlineThickness, 0.0f, 0.0f)

                    if (HealthBar.enabled) {
                        val actualHealth = entity.getActualHealth()
                        val maxHealth = entity.maxHealth.coerceAtLeast(1f) // prevent division by zero
                        val healthPercentage = (actualHealth / maxHealth).coerceIn(0f..1f)

                        val healthColor = Color4b.RED
                            .interpolateTo(Color4b.GREEN, healthPercentage.toDouble())
                            .toARGB()
                        val healthHeight = rectHeight * healthPercentage

                        if (Border.enabled) {
                            drawVerticalLine(-outlineThickness / 2 - borderThickness,
                                -outlineThickness / 2 - borderThickness,
                                rectHeight + outlineThickness / 2 + borderThickness,
                                outlineThickness + 2 * borderThickness, black)
                        }
                        drawVerticalLine(-outlineThickness / 2,
                            rectHeight - healthHeight - outlineThickness / 2,
                            rectHeight + outlineThickness / 2, outlineThickness, healthColor)
                    }
                }
            }
        }
    }

}
