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
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

// pixels / (16 + 16)
private val LIMB = AABB(0.0, 0.0, 0.0, 0.125, 0.375, 0.125)
private val BODY = AABB(0.0, 0.0, 0.0, 0.25, 0.375, 0.125)
private val HEAD = AABB(0.0, 0.0, 0.0, 0.25, 0.25, 0.25)

private val RENDER_LEFT_LEG: AABB = LIMB.move(-LIMB.maxX, 0.0, 0.0)
private val RENDER_RIGHT_LEG: AABB = LIMB
private val RENDER_BODY: AABB = BODY.move(-LIMB.maxX, LIMB.maxY, 0.0)
private val RENDER_LEFT_ARM: AABB = LIMB.move(-2 * LIMB.maxX, LIMB.maxY, 0.0)
private val RENDER_RIGHT_ARM: AABB = LIMB.move(BODY.maxX - LIMB.maxX, LIMB.maxY, 0.0)
private val RENDER_HEAD: AABB = HEAD.move(-LIMB.maxX, LIMB.maxY * 2, -HEAD.maxZ * 0.25)

data class WireframePlayer(private var pos: Vec3, private var yRot: Float, private var xRot: Float) {
    private val quaternion = Quaternionf()

    fun render(event: WorldRenderEvent, color: Color4b, outlineColor: Color4b) {
        renderEnvironmentForWorld(event.matrixStack) {
            startBatch()
            withPositionRelativeToCamera(pos) {
                val pose = matrixStack.last().pose()
                val yRot = -Mth.wrapDegrees(yRot)
                pose.rotate(quaternion.scaling(1f).rotationY(yRot.toRadians()))
                pose.scale(1.9f)

                drawBox(RENDER_LEFT_LEG, color, outlineColor)
                drawBox(RENDER_RIGHT_LEG, color, outlineColor)
                drawBox(RENDER_BODY, color, outlineColor)
                drawBox(RENDER_LEFT_ARM, color, outlineColor)
                drawBox(RENDER_RIGHT_ARM, color, outlineColor)

                pose.translate(0f, RENDER_HEAD.minY.toFloat(), 0f)
                pose.rotate(quaternion.scaling(1f).rotationX(xRot.toRadians()))
                pose.translate(0f, -RENDER_HEAD.minY.toFloat(), 0f)

                drawBox(RENDER_HEAD, color, outlineColor)
            }
            commitBatch()
        }
    }

    fun setPosRot(x: Double, y: Double, z: Double, yRot: Float, xRot: Float) {
        this.pos = Vec3(x, y, z)
        this.yRot = yRot
        this.xRot = xRot
    }

}
