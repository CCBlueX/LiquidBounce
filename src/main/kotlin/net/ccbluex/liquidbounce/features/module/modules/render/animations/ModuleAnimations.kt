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
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PlayerStrideEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import org.joml.Quaternionf

/**
 * Animations module
 *
 * This module affects item animations. It allows the user to customize the animation.
 * If you are looking forward to contribute to this module, please name your animation with a reasonable name.
 * Do not name them after clients or yourself.
 * Please credit from where you got the animation from and make sure they are willing to contribute.
 * If they are not willing to contribute, please do not add the animation to this module.
 */
@Suppress("MagicNumber")
object ModuleAnimations : ClientModule("Animations", ModuleCategories.RENDER, aliases = listOf("ViewModel")) {

    init {
        tree(MainHand)
        tree(OffHand)
        tree(EquipOffset)
        tree(SwingAnimations)
    }

    object MainHand : ToggleableValueGroup(this, "MainHand", false) {
        val mainHandItemScale by float("ItemScale", 0f, -5f..5f)
        val mainHandX by float("X", 0f, -5f..5f)
        val mainHandY by float("Y", 0f, -5f..5f)
        val mainHandPositiveX by float("PositiveRotationX", 0f, -50f..50f)
        val mainHandPositiveY by float("PositiveRotationY", 0f, -50f..50f)
        val mainHandPositiveZ by float("PositiveRotationZ", 0f, -50f..50f)
    }

    object OffHand : ToggleableValueGroup(this, "OffHand", false) {
        val offHandItemScale by float("ItemScale", 0f, -5f..5f)
        val offHandX by float("X", 0f, -1f..1f)
        val offHandY by float("Y", 0f, -1f..1f)
        val OffHandPositiveX by float("PositiveRotationX", 0f, -50f..50f)
        val OffHandPositiveY by float("PositiveRotationY", 0f, -50f..50f)
        val OffHandPositiveZ by float("PositiveRotationZ", 0f, -50f..50f)
    }

    val swingDuration by int("SwingDuration", 6, 1..20)

    /**
     * A choice that allows the user to choose the animation that will be used during the blocking
     * of a sword.
     * This choice is only used when the [ModuleSwordBlock] module is enabled.
     */

    val swingAnimation = SwingAnimations
    val blockAnimationChoice = choices(
        "BlockingAnimation", OneSevenAnimation, arrayOf(
            OneSevenAnimation,
            PushdownAnimation,
            SigmaAnimation,
            ExhibitionAnimation,
            AvatarAnimation,
            DortwareAnimation
        )
    )

    object EquipOffset : ToggleableValueGroup(this, "EquipOffset", true) {
        private val ignore by multiEnumChoice("Ignore",
            Ignores.BLOCKING,
            Ignores.PLACE
        )

        val ignoreBlocking get() = Ignores.BLOCKING in ignore
        val ignorePlace get() = Ignores.PLACE in ignore
        val ignoreAmount get() = Ignores.AMOUNT in ignore

        private enum class Ignores(
            override val tag: String
        ) : Tagged {
            BLOCKING("Blocking"),
            PLACE("Place"),
            AMOUNT("Amount")
        }
    }

    /**
     * if true, the walk animation will also be applied in the air.
     */
    private val airWalker by boolean("AirWalker", false)

    @Suppress("unused")
    val strideHandler = handler<PlayerStrideEvent> { event ->
        if (airWalker) {
            event.strideForce = 0.1.coerceAtMost(player.deltaMovement.horizontalDistance()).toFloat()
        }
    }

    /**
     * A choice that aims to transform the held item transformation during the swing progress.
     */


    abstract class AnimationMode(name: String) : Mode(name) {

        override val parent: ModeValueGroup<*>
            get() = blockAnimationChoice

        protected fun applySwingOffset(matrices: PoseStack, arm: HumanoidArm, swingProgress: Float) {
            val armSide = if (arm == HumanoidArm.RIGHT) 1 else -1
            val f = Mth.sin(swingProgress * swingProgress * Math.PI)
            matrices.mulPose(Axis.YP.rotationDegrees(armSide.toFloat() * (45.0f + f * -20.0f)))
            val g = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
            matrices.mulPose(Axis.ZP.rotationDegrees(armSide.toFloat() * g * -20.0f))
            matrices.mulPose(Axis.XP.rotationDegrees(g * -80.0f))
            matrices.mulPose(Axis.YP.rotationDegrees(armSide.toFloat() * -45.0f))
        }

        abstract fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float)

    }

    /**
     * This animation is based on the 1.7 animation. It is the closest to the original animation
     * if not altered by the user.
     *
     * This animation is used in the ViaFabricPlus project.
     * https://github.com/ViaVersion/ViaFabricPlus/blob/9eb2adf6265cf0ac9d2a17921791642f2b0cdd2c/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/item/MixinHeldItemRenderer.java#L50-L60
     */
    object OneSevenAnimation : AnimationMode("1.7") {

        private val translateY by float("Y", 0.1f, 0.05f..0.3f)
        private val swingProgressScale by float("SwingScale", 0.9f, 0.1f..1.0f)

        override fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {
            matrices.translate(if (arm == HumanoidArm.RIGHT) -0.1f else 0.1f, translateY, 0.0f)
            applySwingOffset(matrices, arm, swingProgress * swingProgressScale)
        }

    }

    /**
     * Based on the [applySwingOffset] but with a different transformation
     * during swing progress to make it look like the [PushdownAnimation] from LiquidBounce Legacy.
     *
     * This animation is not the same as the original, but it is similar.
     */
    object PushdownAnimation : AnimationMode("Pushdown") {

        override fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {
            matrices.translate(if (arm == HumanoidArm.RIGHT) -0.1f else 0.1f, 0.1f, 0.0f)

            val g = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
            matrices.mulPose(
                Axis.ZP.rotationDegrees(
                    (if (arm == HumanoidArm.RIGHT) 1 else -1) * g * 10.0f
                )
            )
            matrices.mulPose(Axis.XP.rotationDegrees(g * -35.0f))
        }

    }

    object SigmaAnimation : AnimationMode("Sigma") {

        private val translateY by float("Y", 0.1f, 0.05f..0.3f)

        override fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {

            val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
            val sideM = if (arm == HumanoidArm.RIGHT) 1.0f else -1.0f

            val rot = Quaternionf().rotationAxis(
                (-sine * 27.5f * sideM).toRadians(),
                -8.0f * sideM,
                0.0f,
                9.0f
            ).rotateAxis(
                (-sine * 45.0f * sideM).toRadians(),
                1.0f * sideM,
                sine / 2.0f,
                0.0f
            )
            matrices.mulPose(rot)

            matrices.translate(0.0f, translateY, 0.0f)
            applySwingOffset(matrices, arm, 0f)
        }
    }

    object ExhibitionAnimation : AnimationMode("Exhibition") {

        private val translateY by float("Y", 0.1f, 0.05f..0.3f)

        override fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {

            val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
            val sideM = if (arm == HumanoidArm.RIGHT) 1.0f else -1.0f

            matrices.translate(0.0, -0.1, 0.0)

            applySwingOffset(matrices, arm, 0f)

            matrices.translate(0.1F, 0.4F, -0.1F)

            val rot = Quaternionf().rotationAxis(
                (-sine * 30.0F * sideM).toRadians(),
                sine.div(2F),
                0.0F,
                9.0F
            ).rotateAxis(
                (-sine * 50.0F * sideM).toRadians(),
                0.8F * sideM,
                sine.div(2F),
                0F
            )
            matrices.mulPose(rot)

            matrices.translate(0.0f, translateY - 0.2f, 0.0f)

        }

    }

    object AvatarAnimation : AnimationMode("Avatar") {

        private val translateY by float("Y", 0.1f, 0.05f..0.3f)

        override fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {

            val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
            val sine1 = Mth.sin(swingProgress * swingProgress * Math.PI)
            val sideM = if (arm == HumanoidArm.RIGHT) 1.0f else -1.0f

            matrices.translate(0.2F * sideM, translateY, 0F)

            val rot = Quaternionf().rotationAxis(
                (0.0F * sideM).toRadians(),
                0.0F,
                1.0F,
                0.0F
            ).rotateY(
                (sine1 * -20.0F * sideM).toRadians()
            ).rotateZ(
                (sine * -20.0F * sideM).toRadians()
            ).rotateAxis(
                (sine * -40.0F * sideM).toRadians(),
                1.0F * sideM,
                0.0F,
                0.0F
            )
            matrices.mulPose(rot)

            applySwingOffset(matrices, arm, 0f)
        }
    }

    object DortwareAnimation : AnimationMode("Dortware") {

        private val translateY by float("Y", 0.1f, 0.05f..0.3f)

        override fun transform(matrices: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {

            val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
            val sqrtSwing = Mth.sqrt(swingProgress)
            val altSine = Mth.sin(sqrtSwing * Math.PI - 3)

            val rot = Quaternionf().rotationAxis(
                (-sine * 10).toRadians(),
                0.0f,
                15.0f,
                200.0f
            ).rotateAxis(
                (-sine * 10f).toRadians(),
                300.0f,
                sine.div(2f),
                1.0f
            )
            matrices.mulPose(rot)

            matrices.translate(3.4, 0.3, -0.4)
            matrices.translate(-2.10f, -0.2f, 0.1f)

            val rot1 = Quaternionf().rotationAxis(
                (altSine * 13.0f).toRadians(),
                -10.0f,
                -1.4f,
                -10.0f
            )
            matrices.mulPose(rot1)

            matrices.translate(if(arm == HumanoidArm.RIGHT) -1f else -2f, translateY, 0f)

        }
    }
}
