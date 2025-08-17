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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.BoxRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf

// pixels / (16 + 16)
private val LIMB = Box(0.0, 0.0, 0.0, 0.125, 0.375, 0.125)
private val BODY = Box(0.0, 0.0, 0.0, 0.25, 0.375, 0.125)
private val HEAD = Box(0.0, 0.0, 0.0, 0.25, 0.25, 0.25)

private val RENDER_LEFT_LEG: Box = LIMB.offset(-LIMB.maxX, 0.0, 0.0)
private val RENDER_RIGHT_LEG: Box = LIMB
private val RENDER_BODY: Box = BODY.offset(-LIMB.maxX, LIMB.maxY, 0.0)
private val RENDER_LEFT_ARM: Box = LIMB.offset(-2 * LIMB.maxX, LIMB.maxY, 0.0)
private val RENDER_RIGHT_ARM: Box = LIMB.offset(BODY.maxX - LIMB.maxX, LIMB.maxY, 0.0)
private val RENDER_HEAD: Box = HEAD.offset(-LIMB.maxX, LIMB.maxY * 2, -HEAD.maxZ * 0.25)

data class WireframePlayer(private var pos: Vec3d, private var yaw: Float, private var pitch: Float) {

    fun render(event: WorldRenderEvent, color: Color4b, outlineColor: Color4b) {
        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera(pos) {
                val matrix = matrixStack.peek().positionMatrix
                val yRot = -MathHelper.wrapDegrees(yaw.toDouble())
                matrix.rotate(Quaternionf().rotationY(Math.toRadians(yRot).toFloat()))
                matrix.scale(1.9f)

                BoxRenderer.drawWith(this) {
                    drawBox(RENDER_LEFT_LEG, color, outlineColor)
                    drawBox(RENDER_RIGHT_LEG, color, outlineColor)
                    drawBox(RENDER_BODY, color, outlineColor)
                    drawBox(RENDER_LEFT_ARM, color, outlineColor)
                    drawBox(RENDER_RIGHT_ARM, color, outlineColor)

                    matrix.translate(0f, RENDER_HEAD.minY.toFloat(), 0f)
                    matrix.rotate(Quaternionf().rotationX(Math.toRadians(pitch.toDouble()).toFloat()))
                    matrix.translate(0f, -RENDER_HEAD.minY.toFloat(), 0f)

                    drawBox(RENDER_HEAD, color, outlineColor)
                }
            }
        }
    }

    fun setPosRot(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        this.pos = Vec3d(x, y, z)
        this.yaw = yaw
        this.pitch = pitch
    }

}
