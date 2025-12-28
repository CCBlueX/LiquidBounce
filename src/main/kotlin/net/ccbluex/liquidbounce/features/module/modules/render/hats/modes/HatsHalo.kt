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
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getTorusPoints
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition

/**
 * @author minecrrrr
 */
internal object HatsHalo : HatsMode("Halo") {

    private val height by float("HeightOffset", 0.2f, 0f..1f)
    private val color by color("Color", Color4b(0, 0, 255, 125))

    private object HatSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.3f, 0.1f..2f)
        val tubeRadius by float("Thickness", 0.05f, 0.01f..1f)
        val showInFirstPerson by boolean("FirstPersonView", true)
    }

    init {
        tree(HatSettings)
    }


    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val player = mc.player ?: return@handler

        if (mc.options.cameraType.isFirstPerson && !HatSettings.showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {

            val pos = player.interpolateCurrentPosition(it.partialTicks)

            withPositionRelativeToCamera(pos.add(0.0, (player.bbHeight + height).toDouble(), 0.0)) {

                drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->

                    val mainSegments = 40
                    val tubeSegments = 12

                    // Main loop for creating the torus (donut) using segments.
                    for (mainI in 0 until mainSegments) {

                        val mainCurrentAngleTorus = getAngle(mainI, mainSegments)
                        val mainNextAngleTorus = getNextAngle(mainI, mainSegments)

                        // Nested loop for rendering the torus "thickness".
                        for (tubeI in 0 until tubeSegments) {

                            val tubeCurrentAngleTorus = getAngle(tubeI, tubeSegments)
                            val tubeNextAngleTorus = getNextAngle(tubeI, tubeSegments)

                            val p1 = getTorusPoints(
                                mainCurrentAngleTorus,
                                tubeCurrentAngleTorus,
                                HatSettings.radius,
                                HatSettings.tubeRadius
                            )
                            val p2 = getTorusPoints(
                                mainCurrentAngleTorus,
                                tubeNextAngleTorus,
                                HatSettings.radius,
                                HatSettings.tubeRadius
                            )
                            val p3 = getTorusPoints(
                                mainNextAngleTorus,
                                tubeCurrentAngleTorus,
                                HatSettings.radius,
                                HatSettings.tubeRadius
                            )
                            val p4 = getTorusPoints(
                                mainNextAngleTorus,
                                tubeNextAngleTorus,
                                HatSettings.radius,
                                HatSettings.tubeRadius
                            )

                            addVertex(matrix, p1.first, p1.second, p1.third).color(color)
                            addVertex(matrix, p2.first, p2.second, p2.third).color(color)
                            addVertex(matrix, p3.first, p3.second, p3.third).color(color)

                            addVertex(matrix, p2.first, p2.second, p2.third).color(color)
                            addVertex(matrix, p4.first, p4.second, p4.third).color(color)
                            addVertex(matrix, p3.first, p3.second, p3.third).color(color)
                        }
                    }
                }
            }
        }
    }
}
