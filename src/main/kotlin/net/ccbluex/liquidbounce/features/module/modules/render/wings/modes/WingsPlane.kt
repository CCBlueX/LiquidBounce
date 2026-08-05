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
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.withPush
import org.joml.Matrix4f

internal object WingsPlane : WingsMode("Plane") {

    private val colors = WingsColorSettings()

    private object WingsOptions : ValueGroup("WingsOptions") {
        val wingsLength by float("WingsLength", 1f, 0.1f..2f)
        val wingsHeight by float("WingsHeight", 0.4f, 0.1f..1f)
        val fadeStartRatio by float("FadeStartRatio", 0.5f, 0.1f..1f)
    }

    init {
        tree(WingsOptions)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawWings(isHurt: Boolean, bodyRot: Float) {

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

        poseStack.withPush {
            mulPose(Axis.XP.rotationDegrees(-90f))
            mulPose(Axis.ZP.rotationDegrees(-bodyRot))

            drawWingPair(WingsOptions.wingsLength, WingsOptions.wingsHeight)


            poseStack.withPush {
                translate(0.1, 0.0, -0.1)
                mulPose(Axis.YP.rotationDegrees(27.5f))
                drawWingPair(
                    WingsOptions.wingsLength - 0.25f,
                    WingsOptions.wingsHeight,
                    secondX = WingsOptions.wingsLength
                )
            }

            poseStack.withPush {
                translate(-0.1, 0.0, -0.1)
                mulPose(Axis.YP.rotationDegrees(-27.5f))
                drawWingPair(
                    WingsOptions.wingsLength,
                    WingsOptions.wingsHeight,
                    secondX = WingsOptions.wingsLength - 0.25f
                )
            }
        }
    }
}
