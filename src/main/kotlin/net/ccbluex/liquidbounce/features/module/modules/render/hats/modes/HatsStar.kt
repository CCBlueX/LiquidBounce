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
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getRotationAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getStarRadius
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getToroidalMeshCords
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
internal object HatsStar : HatsMode("Star") {

    private val height by float("HeightOffset", 0.2f, 0f..1f)
    private val color by color("Color", Color4b(0, 0, 255, 125))

    private object HatStarSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.3f, 0.1f..2f)
        val tubeRadius by float("Thickness", 0.05f, 0.01f..1f)
        val showInFirstPerson by boolean("FirstPersonView", true)
        val sharpness by float("Sharpness", 0.6f, 0.1f..0.9f)
        val pointsCount by int("PointsCount", 5, 5..15)
        object StarSpin : ToggleableConfigurable(this@HatsStar, "Spin", true) {
            val spinSpeed by float("Speed", 1f, 0.1f..10f)
        }
    }

    init {
        tree(HatStarSettings)
        tree(HatStarSettings.StarSpin)
    }


    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>{
        val player = mc.player ?: return@handler

        if (mc.options.cameraType.isFirstPerson && !HatStarSettings.showInFirstPerson) return@handler
        renderEnvironmentForWorld(it.matrixStack) {

            val pos = player.interpolateCurrentPosition(it.partialTicks)

            withPositionRelativeToCamera(pos.add(0.0,
                (player.bbHeight + height).toDouble(), 0.0)) {
                drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
                    val rotAngle = if(HatStarSettings.StarSpin.enabled) {
                        getRotationAngle(HatStarSettings.StarSpin.spinSpeed)
                    } else 0.0
                    val outerSegments = HatStarSettings.pointsCount * 2
                    val innerSegments = HatStarSettings.pointsCount
                    val points = HatStarSettings.pointsCount

                    for (mainI in 0 until outerSegments) {

                        val mainCurrentAngleStar = getAngle(mainI, outerSegments)
                        val mainNextAngleStar = getNextAngle(mainI, outerSegments)

                        val currentRadius = getStarRadius(mainCurrentAngleStar,
                            HatStarSettings.radius, points, HatStarSettings.sharpness)
                        val nextRadius = getStarRadius(mainNextAngleStar,
                            HatStarSettings.radius, points, HatStarSettings.sharpness)

                        for (tubeI in 0 until innerSegments) {

                            val tubeCurrentAngleStar = getAngle(tubeI, innerSegments)
                            val tubeNextAngleStar = getNextAngle(tubeI, innerSegments)

                            val quad = getToroidalMeshCords(
                                mainCurrentAngleStar,
                                mainNextAngleStar,
                                tubeCurrentAngleStar,
                                tubeNextAngleStar,
                                rotAngle,
                                currentRadius,
                                nextRadius,
                                HatStarSettings.tubeRadius
                            )

                            addVertex(matrix, quad.p1.first, quad.p1.second, quad.p1.third).color(color)
                            addVertex(matrix, quad.p2.first, quad.p2.second, quad.p2.third).color(color)
                            addVertex(matrix, quad.p3.first, quad.p3.second, quad.p3.third).color(color)
                            addVertex(matrix, quad.p2.first, quad.p2.second, quad.p2.third).color(color)
                            addVertex(matrix, quad.p4.first, quad.p4.second, quad.p4.third).color(color)
                            addVertex(matrix, quad.p3.first, quad.p3.second, quad.p3.third).color(color)
                        }
                    }
                }
            }
        }
    }

}
