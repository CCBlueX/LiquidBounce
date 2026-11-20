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

package net.ccbluex.liquidbounce.features.global.viewmodel

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm

/**
 * Replaces the vanilla swing transformation of the main hand.
 * If you are looking forward to contribute to this module, please name your animation with a reasonable name.
 * Do not name them after clients or yourself.
 * Please credit from where you got the animation from and make sure they are willing to contribute.
 * If they are not willing to contribute, please do not add the animation to this module.
 */
sealed class SwingAnimationMode(name: String) : Mode(name) {

    override val parent: ModeValueGroup<*>
        get() = GlobalSettingsViewModel.swingAnimation

    abstract fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float)

    object Swipe : SwingAnimationMode("Swipe") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val g = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.YP.rotationDegrees(i * (45.0f + swing * -20.0f)))
            poseStack.rotate(Axis.ZP.rotationDegrees(i * g * -70.0f))
            poseStack.rotate(Axis.XP.rotationDegrees(-70f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * -45.0f))
        }
    }

    object Spin : SwingAnimationMode("Spin") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            poseStack.rotate(Axis.XP.rotationDegrees(swing * -360f))
        }
    }

    object Hook : SwingAnimationMode("Hook") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val g = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.XP.rotationDegrees(50f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * (-30f * (1f - g) - 30f)))
            poseStack.rotate(Axis.ZP.rotationDegrees(i * 110f))
        }
    }

    object Dash : SwingAnimationMode("Dash") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val g = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.XP.rotationDegrees(50f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * (-60f * g - 50f)))
            poseStack.rotate(Axis.ZP.rotationDegrees(i * 110f))
        }
    }

    object Tap : SwingAnimationMode("Tap") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val g = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.XP.rotationDegrees(50f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * -60f))
            poseStack.rotate(Axis.ZP.rotationDegrees(i * (110f + 20f * g)))
        }
    }

    object Inject : SwingAnimationMode("Inject") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val g = fSin(fSqrt(swing) * PI)
            poseStack.translate(0.0, 0.0, (-g / 4.0))
            poseStack.rotate(Axis.XP.rotationDegrees(-120f))
        }
    }

    object Slap : SwingAnimationMode("Slap") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val g = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.XP.rotationDegrees(-fSin(swing * 3f) * 60f))
            poseStack.rotate(Axis.ZP.rotationDegrees(i * -60f * g))
        }
    }

    object Akrien : SwingAnimationMode("Akrien") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val fSqrt = fSqrt(swing)
            val g = fSin(fSqrt * PI)
            if (swing > 0) {
                poseStack.rotate(Axis.YP.rotationDegrees(i * 45f))
                poseStack.rotate(Axis.XP.rotationDegrees(g * -85.0f))
                poseStack.translate(i * -0.1, 0.28, 0.2)
                poseStack.rotate(Axis.XP.rotationDegrees(-85.0f))
            } else {
                val n = -0.4f * g
                val m = 0.2f * fSin(fSqrt * PI * 2f)
                val f2 = -0.2f * fSin(swing * PI)
                poseStack.translate(i * n.toDouble(), m.toDouble(), f2.toDouble())
                applySwingOffset(poseStack, i, swing)
            }
        }

        private fun applySwingOffset(poseStack: PoseStack, i: Int, swing: Float) {
            val f1 = fSin(swing * swing * PI)
            poseStack.rotate(Axis.YP.rotationDegrees(i * (45.0f + f1 * -20.0f)))
            val g = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.ZP.rotationDegrees(i * g * -20.0f))
            poseStack.rotate(Axis.XP.rotationDegrees(g * -80.0f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * -45.0f))
        }
    }

    object Smooth : SwingAnimationMode("Smooth") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val sin1 = fSin(swing * swing * PI)
            val sin2 = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.YP.rotationDegrees(i * (45.0f + sin1 * -20.0f)))
            poseStack.rotate(Axis.ZP.rotationDegrees(i * sin2 * -20.0f))
            poseStack.rotate(Axis.XP.rotationDegrees(sin2 * -80.0f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * -45.0f))
            poseStack.translate(0.0, -0.1, 0.0)
        }
    }

    object Power : SwingAnimationMode("Power") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val sin1 = fSin(swing * swing * PI)
            val sin2 = fSin(fSqrt(swing) * PI)
            val sinSmooth = fSin(swing * PI) * 0.5f
            poseStack.translate((-sinSmooth * sinSmooth * sin1 * i).toDouble(), 0.0, 0.0)
            poseStack.rotate(Axis.YP.rotationDegrees(i * 61f))
            poseStack.rotate(Axis.ZP.rotationDegrees(sin2))
            poseStack.rotate(Axis.YP.rotationDegrees(sin2 * sin1 * -5.0f))
            poseStack.rotate(Axis.XP.rotationDegrees(sin2 * sin1 * -30.0f))
            poseStack.rotate(Axis.XP.rotationDegrees(-60.0f))
            poseStack.rotate(Axis.XP.rotationDegrees(sinSmooth * -60.0f))
        }
    }

    object Feast : SwingAnimationMode("Feast") {
        override fun transform(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
            val i = arm.side
            val sin2 = fSin(fSqrt(swing) * PI)
            poseStack.rotate(Axis.YP.rotationDegrees(i * 30f))
            poseStack.rotate(Axis.YP.rotationDegrees(sin2 * 75.0f * i))
            poseStack.rotate(Axis.XP.rotationDegrees(sin2 * -45.0f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * 30f))
            poseStack.rotate(Axis.XP.rotationDegrees(-80.0f))
            poseStack.rotate(Axis.YP.rotationDegrees(i * 35f))
        }
    }
}

private const val PI = Math.PI.toFloat()

private val HumanoidArm.side get() = if (this == HumanoidArm.RIGHT) 1 else -1

private fun fSin(v: Float) = Mth.sin(v.toDouble())

private fun fSqrt(v: Float) = Mth.sqrt(v)
