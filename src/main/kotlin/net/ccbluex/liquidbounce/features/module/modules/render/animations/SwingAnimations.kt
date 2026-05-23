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

    val mode by enumChoice("Mode", Mode.Fourteen)

    enum class Mode(override val tag: String) : Tagged {
        One("Jump"), Two("Swipe"), Three("Bounce"), Four("Tilt"),
        Five("Pulse"), Six("Spin"), Seven("Hook"), Eight("Dash"), Nine("Tap"),
        Ten("Inject"), Eleven("Slap"), Twelve("Akrien"),
        Thirteen("Smooth"), Fourteen("Power"), Fifteen("Feast")
    }

    private fun fSin(v: Float) = Mth.sin(v.toDouble())
    private fun fSqrt(v: Float) = Mth.sqrt(v)
    private const val PI = Math.PI.toFloat()

    fun onRenderItem(player: AbstractClientPlayer,
                     hand: InteractionHand,
                     swingProgress: Float,
                     equipProgress: Float,
                     poseStack: PoseStack
    ) {
        val isMainHand = hand == InteractionHand.MAIN_HAND
        val arm = if (isMainHand) player.mainArm else player.mainArm.opposite

        applySwing(poseStack, swingProgress, equipProgress, arm)
    }

    @Suppress("LongMethod")
    private fun applySwing(poseStack: PoseStack,
                           swing: Float,
                           equip: Float,
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
            Mode.One -> applyEquipOffset(poseStack, arm, 0.2f * fSin(fSqrt * PI * 2f))
            Mode.Two -> {
                applyEquipOffset(poseStack, arm, n)
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0f + swing * -20.0f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -70.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-70f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0f))
            }
            Mode.Three -> {
                applyEquipOffset(poseStack, arm, 0f)
                if (swing > 0) poseStack.mulPose(Axis.XP.rotationDegrees(-fSin(swing * 13f) * 37f))
            }
            Mode.Four -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -20.0f))
            }
            Mode.Five -> {
                applyEquipOffset(poseStack, arm, 0f)
                val scale = -fSin(swing * 3f) / 2f + 1f
                poseStack.scale(scale, scale, scale)
            }
            Mode.Six -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.mulPose(Axis.XP.rotationDegrees(swing * -360f))
            }
            Mode.Seven -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (-30f * (1f - g) - 30f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 110f))
            }
            Mode.Eight -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (-60f * g - 50f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 110f))
            }
            Mode.Nine -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -60f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * (110f + 20f * g)))
            }
            Mode.Ten -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.translate(0.0, 0.0, (-g / 4.0))
                poseStack.mulPose(Axis.XP.rotationDegrees(-120f))
            }
            Mode.Eleven -> {
                applyEquipOffset(poseStack, arm, 0f)
                poseStack.mulPose(Axis.XP.rotationDegrees(-fSin(swing * 3f) * 60f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * -60f * g))
            }
            Mode.Twelve -> {
                if (swing > 0) {
                    poseStack.translate(i * 0.56, (equip * -0.2f) - 0.5, -0.7)
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * 45f))
                    poseStack.mulPose(Axis.XP.rotationDegrees(g * -85.0f))
                    poseStack.translate(i * -0.1, 0.28, 0.2)
                    poseStack.mulPose(Axis.XP.rotationDegrees(-85.0f))
                } else {
                    val m = 0.2f * fSin(fSqrt * PI * 2f)
                    val f2 = -0.2f * fSin(swing * PI)
                    poseStack.translate(i * n.toDouble(), m.toDouble(), f2.toDouble())
                    applyEquipOffset(poseStack, arm, 0f)
                    applySwingOffset(poseStack, arm, swing)
                }
            }

            Mode.Thirteen -> {
                poseStack.translate(i * 0.56, -0.42, -0.72)
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0f + sin1 * -20.0f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * sin2 * -20.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -80.0f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0f))
                poseStack.translate(0.0, -0.1, 0.0)
            }

            Mode.Fourteen -> {
                poseStack.translate(i * 0.56, -0.32, -0.72)
                poseStack.translate((-sinSmooth * sinSmooth * sin1 * i).toDouble(), 0.0, 0.0)
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 61f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2))
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * sin1 * -5.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * sin1 * -30.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-60.0f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sinSmooth * -60.0f))
            }

            Mode.Fifteen -> {
                poseStack.translate(i * 0.56, -0.32, -0.72)
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 30f))
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * 75.0f * i))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -45.0f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 30f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 35f))
            }
        }
    }

    private fun applyEquipOffset(poseStack: PoseStack, arm: HumanoidArm, equip: Float) {
        val i = if (arm == HumanoidArm.RIGHT) 1 else -1
        poseStack.translate(i * 0.56, -0.52 + equip * -0.6, -0.72)
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
