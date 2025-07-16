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
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinDrawContextAccessor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f

object Esp2DMode : EspMode("2D") {

    private val outline by boolean("Outline", true)
    private val border by boolean("Border", true)
    private val expand by float("Expand", 0.05f, 0f..0.5f)
    private val fill by boolean("Fill", true)
    private val healthBar by boolean("HealthBar", true)

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
                val outlineColor = color.with(a = 255)

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
                var rectWidth = (maxX - minX)
                var rectHeight = (maxY - minY)
                val black = 0xFF000000.toInt()

                with(DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)) {
                    with(matrices) {
                        translate(minX, minY, minZ)

                        if (fill) {
                            fill(0f, 0f, rectWidth, rectHeight, 0f, baseColor.toARGB())
                        }

                        if (outline) {

                            if (border) {
                                horizontalLine(0.0f, rectWidth, 0.0f, 1.5f, black)
                                verticalLine(0.0f, 0.0f, rectHeight, 1.5f, black)
                                horizontalLine(0.0f, rectWidth, rectHeight, 1.5f, black)
                                verticalLine(rectWidth, 0.0f, rectHeight + 1.5f, 1.5f, black)

                                translate(0.5f, 0.5f, 0.0f)
                            }

                            horizontalLine(0.0f, rectWidth, 0.0f, 0.5f, outlineColor.toARGB())
                            horizontalLine(0.0f, rectWidth, rectHeight, 0.5f, outlineColor.toARGB())
                            verticalLine(0.0f, 0.0f, rectHeight, 0.5f, outlineColor.toARGB())
                            verticalLine(rectWidth, 0.0f, rectHeight + 0.5f, 0.5f, outlineColor.toARGB())

                            if (border) {
                                translate(-0.5f, -0.5f, 0.0f)
                            }
                        }

                        if (healthBar) {
                            val health = (entity.health / entity.maxHealth).coerceIn(0f..1f)
                            val healthColor = Color4b(((1 - health) * 255).toInt(), (health * 255).toInt(), 0)
                            val healthHeight = rectHeight * health

                            translate(-3.0f, 0.0f, 0.0f)

                            verticalLine(0.0f, 0.0f, rectHeight + 1.5f, 1.5f, black)
                            verticalLine(0.5f, rectHeight + 1f, rectHeight - healthHeight + 0.5f, 0.5f, healthColor.toARGB())
                        }
                    }
                }
            }
        }
    }

}


private fun DrawContext.fill(x1: Float, y1: Float, x2: Float, y2: Float, z: Float, color: Int) {
    val layer = RenderLayer.getGui()
    var x1 = x1
    var y1 = y1
    var x2 = x2
    var y2 = y2
    val matrix4f: Matrix4f? = this.matrices.peek().getPositionMatrix()
    if (x1 < x2) {
        val i = x1
        x1 = x2
        x2 = i
    }

    if (y1 < y2) {
        val i = y1
        y1 = y2
        y2 = i
    }

    val vertexConsumer: VertexConsumer = (this as MixinDrawContextAccessor).vertexConsumers.getBuffer(layer)
    vertexConsumer.vertex(matrix4f, x1, y1, z).color(color)
    vertexConsumer.vertex(matrix4f, x1, y2, z).color(color)
    vertexConsumer.vertex(matrix4f, x2, y2, z).color(color)
    vertexConsumer.vertex(matrix4f, x2, y1, z).color(color)
}

private fun DrawContext.horizontalLine(x1: Float, x2: Float, y: Float, thickness: Float, color: Int) {
    this.fill(x1, y, x2, y + thickness, 0f, color)
}

private fun DrawContext.verticalLine(x: Float, y1: Float, y2: Float, thickness: Float, color: Int) {
    this.fill(x, y1, x + thickness, y2, 0f, color)
}
