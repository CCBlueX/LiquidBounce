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
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getPointX
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getPointZ
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getRotationAngle
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
internal object HatsOrbs : HatsMode("Orbs") {

    val height by float("HeightOffset", 0.1f, 0f..2f)
    val showInFirstPerson by boolean("FirstPersonView", true)

    val color by color("color", Color4b(0, 0, 255, 125))

    private object HatOrbsSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.5f, 0f..2f)
        val speed by float("Speed", 0.5f, 0.1f..10f)
        val size by float("OrbsSize", 0.1f, 0.01f..0.5f)
        val count by int("OrbsCount", 6, 1..12)

        object WaveSettings : ToggleableConfigurable(this@HatsOrbs, "Wave", true) {
            val waveHeight by float("WaveHeight", 0.1f, 0.01f..1f)
            val waveSpeed by float("WaveSpeed", 2.0f, 0.1f..10f)
        }

        object OrbRotation : ToggleableConfigurable(this@HatsOrbs, "Spin", true) {
            val speedRot by float("SpinSpeed", 2.0f, 0.1f..10f)
        }

    }

    init {
        tree(HatOrbsSettings)
        tree(HatOrbsSettings.WaveSettings)
        tree(HatOrbsSettings.OrbRotation)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> {
        val player = mc.player ?: return@handler
        val pos = player.interpolateCurrentPosition(it.partialTicks)

        if (mc.options.cameraType.isFirstPerson && !HatsFlower.showInFirstPerson) return@handler
        renderEnvironmentForWorld(it.matrixStack) {
            withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {
                drawCustomMesh(ClientRenderPipelines.Triangles) {
                        matrix ->

                    val time = (System.currentTimeMillis() / 1000.0) * HatOrbsSettings.speed


                    // Loop for rendering each individual orb (orbit).
                    for (i in 0 until HatOrbsSettings.count) {

                        val angle = getAngle(i, HatOrbsSettings.count) + time

                        val x = getPointX(angle, HatOrbsSettings.radius)
                        val z = getPointZ(angle, HatOrbsSettings.radius)

                        val y = if (HatOrbsSettings.WaveSettings.enabled) {
                            sin(time * HatOrbsSettings.WaveSettings.waveSpeed + i).toFloat() *
                                HatOrbsSettings.WaveSettings.waveHeight
                        } else 0f

                        val rotAngle = if(HatOrbsSettings.OrbRotation.enabled) {
                            getRotationAngle(HatOrbsSettings.OrbRotation.speedRot)
                        } else 0.0
                        val sinA = (sin(rotAngle)).toFloat() * HatOrbsSettings.size
                        val cosA = (cos(rotAngle)).toFloat() * HatOrbsSettings.size

                        val top = y + HatOrbsSettings.size
                        val bottom = y - HatOrbsSettings.size

                        val ax = x + sinA; val az = z + cosA
                        val bx = x + cosA; val bz = z - sinA
                        val cx = x - sinA; val cz = z - cosA
                        val dx = x - cosA; val dz = z + sinA

                        // Rendering of the top part of the rhombus (4 faces/8 triangles).
                        addVertex(matrix, x, top, z).color(color)
                        addVertex(matrix, dx, y, dz).color(color)
                        addVertex(matrix, ax, y, az).color(color)
                        addVertex(matrix, x, top, z).color(color)
                        addVertex(matrix, ax, y, az).color(color)
                        addVertex(matrix, bx, y, bz).color(color)
                        addVertex(matrix, x, top, z).color(color)
                        addVertex(matrix, bx, y, bz).color(color)
                        addVertex(matrix, cx, y, cz).color(color)
                        addVertex(matrix, x, top, z).color(color)
                        addVertex(matrix, cx, y, cz).color(color)
                        addVertex(matrix, dx, y, dz).color(color)

                        // Rendering of the bottom part of the rhombus (4 faces/8 triangles).
                        addVertex(matrix, x, bottom, z).color(color)
                        addVertex(matrix, dx, y, dz).color(color)
                        addVertex(matrix, ax, y, az).color(color)
                        addVertex(matrix, x, bottom, z).color(color)
                        addVertex(matrix, ax, y, az).color(color)
                        addVertex(matrix, bx, y, bz).color(color)
                        addVertex(matrix, x, bottom, z).color(color)
                        addVertex(matrix, bx, y, bz).color(color)
                        addVertex(matrix, cx, y, cz).color(color)
                        addVertex(matrix, x, bottom, z).color(color)
                        addVertex(matrix, cx, y, cz).color(color)
                        addVertex(matrix, dx, y, dz).color(color)
                    }

                }

            }

        }

    }

}
