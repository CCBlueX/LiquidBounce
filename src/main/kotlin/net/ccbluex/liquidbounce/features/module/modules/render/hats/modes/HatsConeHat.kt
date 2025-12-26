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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.modes

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author minecrrrr
 */
object HatsConeHat : HatsMode("ConeHat") {

    private val height by float("HeightOffset", 0.1f, 0f..1f)

    private val color by color("Color", Color4b(0, 0, 255, 125))

    private object HatSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.6f, 0.1f..2f)
        val peak by float("Peak", 0.3f, 0.01f..2f)
        val showInFirstPerson by boolean("FirstPersonView", true)
    }

    init {
        tree(HatSettings)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>{
        val player = mc.player ?: return@handler

        if (mc.options.cameraType.isFirstPerson && !HatSettings.showInFirstPerson) return@handler
            renderEnvironmentForWorld(it.matrixStack) {
                val pos = player.interpolateCurrentPosition(it.partialTicks)

                withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {
                    drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
                        val segments = 40

                        for (i in 0 until segments) {
                            val angle1 = i * Math.PI * 2 / segments
                            val angle2 = (i + 1) * Math.PI * 2 / segments
                            addVertex(matrix, 0f, HatSettings.peak, 0f).color(color)
                            val x2 = (sin(angle1) * HatSettings.radius).toFloat()
                            val z2 = (cos(angle1) * HatSettings.radius).toFloat()
                            addVertex(matrix, x2, 0f, z2).color(color)

                            val x1 = (sin(angle2) * HatSettings.radius).toFloat()
                            val z1 = (cos(angle2) * HatSettings.radius).toFloat()
                            addVertex(matrix, x1, 0f, z1).color(color)
                        }
                    }
                }
            }
    }
}
