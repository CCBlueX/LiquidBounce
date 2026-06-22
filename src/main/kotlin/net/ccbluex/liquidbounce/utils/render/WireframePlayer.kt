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
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.minecraft.util.Mth
import net.minecraft.world.entity.Pose
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

private const val MODEL_SCALE = 1.9f

private const val CROUCH_BODY_ROTATION = 28.64789f
private const val CROUCH_ARM_ROTATION = 22.918312f

private val CROUCH_LEFT_LEG: AABB = RENDER_LEFT_LEG.move(0.0, 0.0, 0.125)
private val CROUCH_RIGHT_LEG: AABB = RENDER_RIGHT_LEG.move(0.0, 0.0, 0.125)
private val CROUCH_BODY: AABB = RENDER_BODY.move(0.0, -0.12, 0.05)
private val CROUCH_LEFT_ARM: AABB = RENDER_LEFT_ARM.move(0.0, -0.12, 0.03)
private val CROUCH_RIGHT_ARM: AABB = RENDER_RIGHT_ARM.move(0.0, -0.12, 0.03)
private val CROUCH_HEAD: AABB = RENDER_HEAD.move(0.0, -0.18, 0.1)

private const val SWIM_PART_ROTATION = 90f
private const val SWIM_HEAD_TARGET_ROTATION = -45f
private const val SWIM_LEFT_ARM_ROLL = -15f
private const val SWIM_RIGHT_ARM_ROLL = 15f
private const val SWIM_LEFT_LEG_ROLL = -6f
private const val SWIM_RIGHT_LEG_ROLL = 6f
private const val SWIM_ROOT_Y_OFFSET = -0.4375

class WireframePlayer {
    var pos: Vec3 = Vec3.ZERO
    var yRot: Float = 0F
    var xRot: Float = 0F
    var pose: Pose = Pose.STANDING
    var swimAmount: Float = 0F

    private val quaternion = Quaternionf()

    fun render(event: WorldRenderEvent, color: Color4b, outlineColor: Color4b) {
        renderEnvironmentForWorld(event.matrixStack) {
            withPositionRelativeToCamera(pos) {
                poseStack.withPush {
                    val bodyYaw = -Mth.wrapDegrees(yRot)
                    poseStack.mulPose(quaternion.identity().rotationY(bodyYaw.toRadians()))
                    poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE)

                    when (pose) {
                        Pose.CROUCHING -> renderCrouching(color, outlineColor)
                        Pose.SWIMMING -> renderSwimming(color, outlineColor)
                        else -> renderStanding(color, outlineColor)
                    }
                }
            }
        }
    }

    fun setRotation(rotation: Rotation) {
        this.xRot = rotation.xRot
        this.yRot = rotation.yRot
    }

    fun setPosRot(x: Double, y: Double, z: Double, yRot: Float, xRot: Float) {
        this.pos = Vec3(x, y, z)
        this.yRot = yRot
        this.xRot = xRot
    }

    private fun WorldRenderEnvironment.renderStanding(
        color: Color4b,
        outlineColor: Color4b,
    ) {
        renderPart(RENDER_LEFT_LEG, color, outlineColor)
        renderPart(RENDER_RIGHT_LEG, color, outlineColor)
        renderPart(RENDER_BODY, color, outlineColor)
        renderPart(RENDER_LEFT_ARM, color, outlineColor)
        renderPart(RENDER_RIGHT_ARM, color, outlineColor)
        renderPart(
            box = RENDER_HEAD,
            color = color,
            outlineColor = outlineColor,
            pivot = RENDER_HEAD.bottomCenter,
            xRot = xRot,
        )
    }

    private fun WorldRenderEnvironment.renderCrouching(
        color: Color4b,
        outlineColor: Color4b,
    ) {
        renderPart(CROUCH_LEFT_LEG, color, outlineColor)
        renderPart(CROUCH_RIGHT_LEG, color, outlineColor)
        renderPart(
            box = CROUCH_BODY,
            color = color,
            outlineColor = outlineColor,
            pivot = CROUCH_BODY.bottomCenter,
            xRot = CROUCH_BODY_ROTATION,
        )
        renderPart(
            box = CROUCH_LEFT_ARM,
            color = color,
            outlineColor = outlineColor,
            pivot = CROUCH_LEFT_ARM.bottomCenter,
            xRot = CROUCH_ARM_ROTATION,
        )
        renderPart(
            box = CROUCH_RIGHT_ARM,
            color = color,
            outlineColor = outlineColor,
            pivot = CROUCH_RIGHT_ARM.bottomCenter,
            xRot = CROUCH_ARM_ROTATION,
        )
        renderPart(
            box = CROUCH_HEAD,
            color = color,
            outlineColor = outlineColor,
            pivot = CROUCH_HEAD.bottomCenter,
            xRot = xRot,
        )
    }

    private fun WorldRenderEnvironment.renderSwimming(
        color: Color4b,
        outlineColor: Color4b,
    ) {
        val swimProgress = if (swimAmount > 0f) swimAmount else 1f
        val swimHeadRotation = Mth.lerp(swimProgress, xRot, SWIM_HEAD_TARGET_ROTATION)

        poseStack.withPush {
            poseStack.translate(RENDER_BODY.center.x, RENDER_BODY.center.y + SWIM_ROOT_Y_OFFSET, RENDER_BODY.center.z)
            poseStack.mulPose(quaternion.identity().rotationX(SWIM_PART_ROTATION.toRadians()))
            poseStack.translate(-RENDER_BODY.center.x, -RENDER_BODY.center.y, -RENDER_BODY.center.z)

            renderPart(RENDER_BODY, color, outlineColor)
            renderPart(
                box = RENDER_LEFT_ARM,
                color = color,
                outlineColor = outlineColor,
                pivot = RENDER_LEFT_ARM.center,
                zRot = SWIM_LEFT_ARM_ROLL,
            )
            renderPart(
                box = RENDER_RIGHT_ARM,
                color = color,
                outlineColor = outlineColor,
                pivot = RENDER_RIGHT_ARM.center,
                zRot = SWIM_RIGHT_ARM_ROLL,
            )
            renderPart(
                box = RENDER_LEFT_LEG,
                color = color,
                outlineColor = outlineColor,
                pivot = RENDER_LEFT_LEG.center,
                zRot = SWIM_LEFT_LEG_ROLL,
            )
            renderPart(
                box = RENDER_RIGHT_LEG,
                color = color,
                outlineColor = outlineColor,
                pivot = RENDER_RIGHT_LEG.center,
                zRot = SWIM_RIGHT_LEG_ROLL,
            )
            renderPart(
                box = RENDER_HEAD,
                color = color,
                outlineColor = outlineColor,
                pivot = RENDER_HEAD.bottomCenter,
                xRot = swimHeadRotation,
            )
        }
    }

    private fun WorldRenderEnvironment.renderPart(
        box: AABB,
        color: Color4b,
        outlineColor: Color4b,
        pivot: Vec3 = box.center,
        xRot: Float = 0f,
        yRot: Float = 0f,
        zRot: Float = 0f,
    ) {
        poseStack.withPush {
            if (xRot != 0f || yRot != 0f || zRot != 0f) {
                poseStack.translate(pivot.x, pivot.y, pivot.z)
                if (zRot != 0f) {
                    poseStack.mulPose(quaternion.identity().rotationZ(zRot.toRadians()))
                }
                if (yRot != 0f) {
                    poseStack.mulPose(quaternion.identity().rotationY(yRot.toRadians()))
                }
                if (xRot != 0f) {
                    poseStack.mulPose(quaternion.identity().rotationX(xRot.toRadians()))
                }
                poseStack.translate(-pivot.x, -pivot.y, -pivot.z)
            }

            drawBox(box, color, outlineColor)
        }
    }

}
