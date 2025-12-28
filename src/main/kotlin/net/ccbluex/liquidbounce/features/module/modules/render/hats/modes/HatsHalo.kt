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
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getColorByAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getToroidalMeshCords
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import org.joml.Vector2f

/**
 * @author minecrrrr
 */
internal object HatsHalo : HatsMode("Halo") {

    private val height by float("HeightOffset", 0.2f, 0f..1f)
    private object Colors : Configurable("Colors") {
        val syncColors by boolean("SyncColors", true)
        val firstColor by color("FirstColor", Color4b(0, 0, 255, 125))
        val secondColor by color("SecondColor", Color4b(0, 0, 255, 125))
        object ColorSpin : ToggleableConfigurable(this@HatsHalo, "ColorSpin", true) {
            val spinSpeed by float("SpinSpeed", 1f, 0.1f..10f)
        }
    }

    private object HatHaloSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.3f, 0.1f..2f)
        val tubeRadius by float("Thickness", 0.05f, 0.01f..1f)
        val showInFirstPerson by boolean("FirstPersonView", true)
    }

    init {
        tree(HatHaloSettings)
        tree(Colors)
        tree(Colors.ColorSpin)
    }


    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>{
        val player = mc.player ?: return@handler

        if (mc.options.cameraType.isFirstPerson && !HatHaloSettings.showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {

            val pos = player.interpolateCurrentPosition(it.partialTicks)

            withPositionRelativeToCamera(pos.add(0.0, (player.bbHeight + height).toDouble(), 0.0)) {

                drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->

                    val mainSegments = 600
                    val tubeSegments = 60

                    // Main loop for creating the torus (donut) using segments.
                    for (mainI in 0 until mainSegments) {

                        val mainCurrentAngleTorus = getAngle(mainI, mainSegments)
                        val mainNextAngleTorus = getNextAngle(mainI, mainSegments)

                        // Nested loop for rendering the torus "thickness".
                        for (tubeI in 0 until tubeSegments) {

                            val tubeCurrentAngleTorus = getAngle(tubeI, tubeSegments)
                            val tubeNextAngleTorus = getNextAngle(tubeI, tubeSegments)

                            val color = getColorByAngle(
                                mainCurrentAngleTorus,
                                Colors.firstColor,
                                if(!Colors.syncColors) Colors.secondColor else Colors.firstColor,
                                if(Colors.ColorSpin.enabled) Colors.ColorSpin.spinSpeed else {
                                    0.0f
                                }
                            )

                            val radii = Vector2f(HatHaloSettings.radius, HatHaloSettings.radius)
                            val quad = getToroidalMeshCords(
                                mainCurrentAngleTorus,
                                mainNextAngleTorus,
                                tubeCurrentAngleTorus,
                                tubeNextAngleTorus,
                                0.0,
                                radii,
                                HatHaloSettings.tubeRadius
                            )

                            addVertex(matrix, quad.p1.first,
                                quad.p1.second, quad.p1.third).color(color)

                            addVertex(matrix, quad.p2.first,
                                quad.p2.second, quad.p2.third).color(color)

                            addVertex(matrix, quad.p3.first,
                                quad.p3.second, quad.p3.third).color(color)

                            addVertex(matrix, quad.p2.first,
                                quad.p2.second, quad.p2.third).color(color)

                            addVertex(matrix, quad.p4.first,
                                quad.p4.second,
                                quad.p4.third).color(color)

                            addVertex(matrix, quad.p3.first,
                                quad.p3.second, quad.p3.third).color(color)
                        }
                    }
                }
            }
        }
    }
}
