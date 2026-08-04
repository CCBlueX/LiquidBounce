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

package net.ccbluex.liquidbounce.features.module.modules.render.animations

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm

object SwingAnimations : ToggleableValueGroup(ModuleAnimations, "SwingAnimations", false) {

    val mode by enumChoice("Mode", Mode.Spin)

    enum class Mode(override val tag: String) : Tagged {
        Swipe("Swipe"), Spin("Spin"), Hook("Hook"), Dash("Dash"),
        Tap("Tap"), Inject("Inject"), Slap("Slap"), Akrien("Akrien"),
        Smooth("Smooth"), Power("Power"), Feast("Feast")
    }

    private fun fSin(v: Float) = Mth.sin(v.toDouble())
    private fun fSqrt(v: Float) = Mth.sqrt(v)
    private const val PI = Math.PI.toFloat()

    fun onRenderItem(player: AbstractClientPlayer,
                     hand: InteractionHand,
                     swingProgress: Float,
                     poseStack: PoseStack
    ) {
        val isMainHand = hand == InteractionHand.MAIN_HAND
        val arm = if (isMainHand) player.mainArm else player.mainArm.opposite

        applySwing(poseStack, swingProgress, arm)
    }

    @Suppress("LongMethod")
    private fun applySwing(poseStack: PoseStack,
                           swing: Float,
                           arm: HumanoidArm
    ) {
        val i = if (arm == HumanoidArm.RIGHT) 1 else -1
        val fSqrt = fSqrt(swing)
        val g = fSin(fSqrt * PI)
        val n = -0.4f * g
        val sin1 = fSin(swing * swing * PI)
        val sin2 = fSin(fSqrt(swing) * PI)
        val sinSmooth = (fSin(swing * PI) * 0.5f)

        when (mode) {
            Mode.Swipe -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0f + swing * -20.0f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -70.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-70f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0f))
            }
            Mode.Spin -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(swing * -360f))
            }
            Mode.Hook -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (-30f * (1f - g) - 30f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 110f))
            }
            Mode.Dash -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (-60f * g - 50f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 110f))
            }
            Mode.Tap -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -60f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * (110f + 20f * g)))
            }
            Mode.Inject -> {
                poseStack.translate(0.0, 0.0, (-g / 4.0))
                poseStack.mulPose(Axis.XP.rotationDegrees(-120f))
            }
            Mode.Slap -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-fSin(swing * 3f) * 60f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * -60f * g))
            }
            Mode.Akrien -> {
                if (swing > 0) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * 45f))
                    poseStack.mulPose(Axis.XP.rotationDegrees(g * -85.0f))
                    poseStack.translate(i * -0.1, 0.28, 0.2)
                    poseStack.mulPose(Axis.XP.rotationDegrees(-85.0f))
                } else {
                    val m = 0.2f * fSin(fSqrt * PI * 2f)
                    val f2 = -0.2f * fSin(swing * PI)
                    poseStack.translate(i * n.toDouble(), m.toDouble(), f2.toDouble())
                    applySwingOffset(poseStack, arm, swing)
                }
            }

            Mode.Smooth -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0f + sin1 * -20.0f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * sin2 * -20.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -80.0f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0f))
                poseStack.translate(0.0, -0.1, 0.0)
            }

            Mode.Power -> {
                poseStack.translate((-sinSmooth * sinSmooth * sin1 * i).toDouble(), 0.0, 0.0)
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 61f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2))
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * sin1 * -5.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * sin1 * -30.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-60.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sinSmooth * -60.0f))
            }

            Mode.Feast -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 30f))
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * 75.0f * i))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -45.0f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 30f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 35f))
            }
        }
    }

    private fun applySwingOffset(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
        val i = if (arm == HumanoidArm.RIGHT) 1 else -1
        val f1 = fSin(swing * swing * PI)
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0f + f1 * -20.0f)))
        val g = fSin(fSqrt(swing) * PI)
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -20.0f))
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -80.0f))
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0f))
    }
}
