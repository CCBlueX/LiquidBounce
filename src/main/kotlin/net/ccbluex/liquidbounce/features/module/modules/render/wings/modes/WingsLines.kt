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

package net.ccbluex.liquidbounce.features.module.modules.render.wings.modes

import com.mojang.math.Axis
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.wings.WingsColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.wings.WingsMode
import net.ccbluex.liquidbounce.features.module.modules.render.wings.modes.WingsLines.WingsOptions.angles
import net.ccbluex.liquidbounce.features.module.modules.render.wings.modes.WingsLines.WingsOptions.linesCount
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.withPush
import org.joml.Matrix4f

object WingsLines : WingsMode("Lines") {

    private val colors = WingsColorSettings()

    private object WingsOptions : ValueGroup("WingsOptions") {
        val wingsLength by float("WingsLength", 1f, 0.1f..2f)
        val wingsWidth by float("WingsWidth", 0.05f, 0.01f..1f)
        val fadeStartRatio by float("FadeStartRatio", 0.5f, 0.1f..1f)
        val linesCount by int("LinesCount", 4, 1..12)
        val angles by intRange("Angles", 10..37, 1..90)
    }

    init {
        tree(WingsOptions)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawWings(isHurt: Boolean, bodyRot: Float, shifting: Float) {

        val currentColor = when (isHurt) {
            true -> Color4b.RED.alpha(colors.color.a)
            else -> colors.color
        }

        fun drawQuadSegment(
            m: Matrix4f,
            x1: Float,
            x2: Float,
            height: Float,
            c1: Color4b,
            c2: Color4b
        ) {
            drawCustomMesh(ClientRenderPipelines.triangles(false)) {
                val h2 = height / 2f
                addVertex(m, x1, 0f, -h2).setColor(c1)
                addVertex(m, x2, 0f, -h2).setColor(c2)
                addVertex(m, x2, 0f, h2).setColor(c2)

                addVertex(m, x1, 0f, -h2).setColor(c1)
                addVertex(m, x2, 0f, h2).setColor(c2)
                addVertex(m, x1, 0f, h2).setColor(c1)
            }
        }

        fun drawWingPair(
            firstX: Float,
            height: Float,
            secondX: Float = firstX,
        ) {
            drawCustomMesh(ClientRenderPipelines.triangles(false)) { pose ->
                val m = pose.pose()
                val transparent = currentColor.alpha(0)

                val rSolid = firstX * WingsOptions.fadeStartRatio
                drawQuadSegment(m, 0f, rSolid, height, currentColor, currentColor)
                drawQuadSegment(m, rSolid, firstX, height, currentColor, transparent)

                val lSolid = -secondX * WingsOptions.fadeStartRatio
                drawQuadSegment(m, 0f, lSolid, height, currentColor, currentColor)
                drawQuadSegment(m, lSolid, -secondX, height, currentColor, transparent)
            }
        }

        val step = if (linesCount > 1) (angles.last - angles.first).toFloat() / (linesCount - 1) else 0f
        val shiftOffset = when (shifting) {
            0f -> 0f
            else -> 27.5f
        }
        poseStack.withPush {
            mulPose(Axis.XP.rotationDegrees(90f))
            mulPose(Axis.ZP.rotationDegrees(bodyRot))
            mulPose(Axis.XP.rotationDegrees(shiftOffset))

            for (i in (0 until linesCount)) {
                val angle = angles.first.toFloat() + (i * step)

                poseStack.withPush {
                    translate(0.1 + (0.0375 * i), 0.0, -0.1 - (0.0375 * i))
                    mulPose(Axis.YP.rotationDegrees(angle))
                    drawWingPair(
                        WingsOptions.wingsLength - 0.25f,
                        WingsOptions.wingsWidth,
                        secondX = WingsOptions.wingsLength
                    )
                }

                poseStack.withPush {
                    translate(-0.1 - (0.0375 * i), 0.0, -0.1 - (0.0375 * i))
                    mulPose(Axis.YP.rotationDegrees(-angle))
                    drawWingPair(
                        WingsOptions.wingsLength,
                        WingsOptions.wingsWidth,
                        secondX = WingsOptions.wingsLength - 0.25f
                    )
                }
            }
        }
    }
}
