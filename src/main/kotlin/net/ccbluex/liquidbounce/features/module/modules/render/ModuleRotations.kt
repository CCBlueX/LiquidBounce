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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations.smooth
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.drawSolidBox
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.Box

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleRotations : ClientModule("Rotations", Category.RENDER) {

    /**
     * Body part to modify the rotation of.
     */
    val bodyParts by enumChoice("BodyParts", BodyPart.BOTH)

    @Suppress("unused")
    enum class BodyPart(
        override val choiceName: String,
        val head: Boolean,
        val body: Boolean
    ) : NamedChoice {
        BOTH("Both", true, true),
        HEAD("Head", true, false),
        BODY("Body", false, true);

        fun allows(part: BodyPart) = when (part) {
            BOTH -> head && body
            HEAD -> head
            BODY -> body
        }

    }

    /**
     * Smoothes the rotation visually only.
     */
    private val smooth by float("Smooth", 0.0f, 0.0f..0.3f)

    /**
     * Changes the perspective of the camera to match the Rotation Manager perspective
     * without changing the player perspective.
     */
    val camera by boolean("Camera", false)
    private val vectorLine by color("VectorLine", Color4b.WHITE.with(a = 0)) // alpha 0 means OFF
    private val vectorDot by color("VectorDot", Color4b(0x00, 0x80, 0xFF, 0x00))

    /**
     * The current model rotation, we could be using
     * [RotationManager.currentRotation] and [RotationManager.previousRotation]
     * directly but this is required for [smooth] to work.
     */
    var modelRotation: Rotation? = null
        get() = if (this.running) field else null
    var prevModelRotation: Rotation? = null

    @Suppress("unused")
    private val modelUpdater = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        val prev = prevModelRotation ?: player.lastRotation
        val current = RotationManager.currentRotation

        if (current == null) {
            prevModelRotation = modelRotation
            modelRotation = null
            return@handler
        }

        val next = if (smooth > 0f) {
            interpolate(prev, current, 1f - smooth)
        } else {
            current
        }

        prevModelRotation = modelRotation
        modelRotation = next
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val partialTicks = event.partialTicks

        val drawVectorLine = vectorLine.a > 0
        val drawVectorDot = vectorDot.a > 0

        if (drawVectorLine || drawVectorDot) {
            val currentRotation = RotationManager.currentRotation ?: return@handler
            val previousRotation = RotationManager.previousRotation ?: currentRotation
            val camera = mc.gameRenderer.camera

            val interpolatedRotationVec = previousRotation.directionVector.lerp(currentRotation.directionVector,
                partialTicks.toDouble()
            )

            val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            if (drawVectorLine) {
                renderEnvironmentForWorld(matrixStack) {
                    withColor(vectorLine) {
                        drawLineStrip(eyeVector, eyeVector + Vec3(interpolatedRotationVec * 100.0))
                    }
                }
            }

            if (drawVectorDot) {
                renderEnvironmentForWorld(matrixStack) {
                    withColor(vectorDot) {
                        val vector = eyeVector + Vec3(interpolatedRotationVec * 100.0)
                        drawSolidBox(Box.of(vector.toVec3d(), 2.5, 2.5, 2.5))
                    }
                }
            }
        }
    }

    private fun interpolate(from: Rotation, to: Rotation, factor: Float): Rotation {
        val diffYaw = to.yaw - from.yaw
        val diffPitch = to.pitch - from.pitch

        val interpolatedYaw = from.yaw + diffYaw * factor
        val interpolatedPitch = from.pitch + diffPitch * factor

        return Rotation(interpolatedYaw, interpolatedPitch)
    }

    override fun disable() {
        this.modelRotation = null
        this.prevModelRotation = null
        super.disable()
    }
}
