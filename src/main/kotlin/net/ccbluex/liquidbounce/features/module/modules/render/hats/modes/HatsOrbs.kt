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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author minecrrrr
 */
internal object HatsOrbs : HatsMode("Orbs") {

    val color by color("color", Color4b(0, 0, 255, 125))

    private object HatOrbsSettings : ValueGroup("HatSettings") {
        val radius by float("Radius", 0.5f, 0f..2f)
        val speed by float("Speed", 0.5f, 0.1f..10f)
        val size by float("OrbsSize", 0.1f, 0.01f..0.5f)
        val count by int("OrbsCount", 6, 1..12)

        object WaveSettings : ToggleableValueGroup(this@HatsOrbs, "Wave", true) {
            val waveHeight by float("WaveHeight", 0.1f, 0.01f..1f)
            val waveSpeed by float("WaveSpeed", 2.0f, 0.1f..10f)
        }

        val spinSpeed by float("SpinSpeed", 2f, -10f..10f)
    }

    init {
        tree(HatOrbsSettings)
        tree(HatOrbsSettings.WaveSettings)
    }

    override fun WorldRenderEnvironment.drawHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
            val time = ((System.currentTimeMillis() % 1000000L).toFloat() / 1000f) * HatOrbsSettings.speed

            // Loop for rendering each individual orb (orbit).
            for (i in 0 until HatOrbsSettings.count) {
                val angle = (getAngle(i, HatOrbsSettings.count) + time)

                val x = getPointX(angle, HatOrbsSettings.radius)
                val z = getPointZ(angle, HatOrbsSettings.radius)

                val y = if (HatOrbsSettings.WaveSettings.enabled) {
                    sin(time * HatOrbsSettings.WaveSettings.waveSpeed + i) *
                        HatOrbsSettings.WaveSettings.waveHeight
                } else {
                    0f
                }

                val rotAngle = getRotationAngle(HatOrbsSettings.spinSpeed)
                val sinA = rotAngle.fastSin() * HatOrbsSettings.size
                val cosA = rotAngle.fastCos() * HatOrbsSettings.size

                val top = y + HatOrbsSettings.size
                val bottom = y - HatOrbsSettings.size

                val ax = x + sinA
                val az = z + cosA
                val bx = x + cosA
                val bz = z - sinA
                val cx = x - sinA
                val cz = z - cosA
                val dx = x - cosA
                val dz = z + sinA

                val color = if (!isHurt) color else Color4b(255, 0, 0, color.a)
                // Rendering of the top part of the rhombus (4 faces/8 triangles).
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, dx, y, dz).setColor(color)
                addVertex(matrix, ax, y, az).setColor(color)
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, ax, y, az).setColor(color)
                addVertex(matrix, bx, y, bz).setColor(color)
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, bx, y, bz).setColor(color)
                addVertex(matrix, cx, y, cz).setColor(color)
                addVertex(matrix, x, top, z).setColor(color)
                addVertex(matrix, cx, y, cz).setColor(color)
                addVertex(matrix, dx, y, dz).setColor(color)

                // Rendering of the bottom part of the rhombus (4 faces/8 triangles).
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, dx, y, dz).setColor(color)
                addVertex(matrix, ax, y, az).setColor(color)
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, ax, y, az).setColor(color)
                addVertex(matrix, bx, y, bz).setColor(color)
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, bx, y, bz).setColor(color)
                addVertex(matrix, cx, y, cz).setColor(color)
                addVertex(matrix, x, bottom, z).setColor(color)
                addVertex(matrix, cx, y, cz).setColor(color)
                addVertex(matrix, dx, y, dz).setColor(color)
            }
        }
    }

    private fun getPointX(angle: Float, radius: Float) = sin(angle) * radius
    private fun getPointZ(angle: Float, radius: Float) = cos(angle) * radius

}
