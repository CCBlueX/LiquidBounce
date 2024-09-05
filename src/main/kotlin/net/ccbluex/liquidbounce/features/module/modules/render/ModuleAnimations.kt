/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerStrideEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations.PushdownAnimation.applySwingOffset
import net.ccbluex.liquidbounce.register.IncludeModule
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Arm
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis

/**
 * Animations module
 *
 * This module affects item animations. It allows the user to customize the animation.
 * If you are looking forward to contribute to this module, please name your animation with a reasonable name.
 * Do not name them after clients or yourself.
 * Please credit from where you got the animation from and make sure they are willing to contribute.
 * If they are not willing to contribute, please do not add the animation to this module.
 */
@IncludeModule
object ModuleAnimations : Module("Animations", Category.RENDER) {

    init {
        tree(MainHand)
        tree(OffHand)
    }

    object MainHand : ToggleableConfigurable(this, "MainHand", false) {
        val mainHandItemScale by float("ItemScale", 0f, -5f..5f)
        val mainHandX by float("X", 0f, -5f..5f)
        val mainHandY by float("Y", 0f, -5f..5f)
        val mainHandPositiveX by float("PositiveRotationX", 0f, -50f..50f)
        val mainHandPositiveY by float("PositiveRotationY", 0f, -50f..50f)
        val mainHandPositiveZ by float("PositiveRotationZ", 0f, -50f..50f)
    }

    object OffHand : ToggleableConfigurable(this, "OffHand", false) {
        val offHandItemScale by float("ItemScale", 0f, -5f..5f)
        val offHandX by float("X", 0f, -1f..1f)
        val offHandY by float("Y", 0f, -1f..1f)
        val OffHandPositiveX by float("PositiveRotationX", 0f, -50f..50f)
        val OffHandPositiveY by float("PositiveRotationY", 0f, -50f..50f)
        val OffHandPositiveZ by float("PositiveRotationZ", 0f, -50f..50f)
    }

    /**
     * A choice that allows the user to choose the animation that will be used during the blocking
     * of a sword.
     * This choice is only used when the [ModuleSwordBlock] module is enabled.
     */
    var blockAnimationChoice = choices(
        "BlockingAnimation", OneSevenAnimation, arrayOf(
            OneSevenAnimation,
            PushdownAnimation
        )
    )

    /**
     * If true, the original Minecraft equip offset will be applied.
     * This causes the blocking to look weirdly fast and cause the sword to almost disappear from
     * the screen.
     */
    var equipOffset by boolean("EquipOffset", false)

    /**
     * if true, the walk animation will also be applied in the air.
     */
    private val airWalker by boolean("AirWalker", false)

    @Suppress("unused")
    val strideHandler = handler<PlayerStrideEvent> { event ->
        if (airWalker) {
            event.strideForce = 0.1.coerceAtMost(player.velocity.horizontalLength()).toFloat()
        }
    }

    /**
     * A choice that aims to transform the held item transformation during the swing progress.
     */
    abstract class AnimationChoice(name: String) : Choice(name) {

        override val parent: ChoiceConfigurable<*>
            get() = blockAnimationChoice

        protected fun applySwingOffset(matrices: MatrixStack, arm: Arm, swingProgress: Float) {
            val armSide = if (arm == Arm.RIGHT) 1 else -1
            val f = MathHelper.sin(swingProgress * swingProgress * Math.PI.toFloat())
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide.toFloat() * (45.0f + f * -20.0f)))
            val g = MathHelper.sin(MathHelper.sqrt(swingProgress) * Math.PI.toFloat())
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSide.toFloat() * g * -20.0f))
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0f))
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSide.toFloat() * -45.0f))
        }

        abstract fun transform(matrices: MatrixStack, arm: Arm, equipProgress: Float, swingProgress: Float)

    }

    /**
     * This animation is based on the 1.7 animation. It is the closest to the original animation
     * if not altered by the user.
     *
     * This animation is used in the ViaFabricPlus project.
     * https://github.com/ViaVersion/ViaFabricPlus/blob/9eb2adf6265cf0ac9d2a17921791642f2b0cdd2c/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/item/MixinHeldItemRenderer.java#L50-L60
     */
    object OneSevenAnimation : AnimationChoice("1.7") {

        private val translateY by float("Y", 0.1f, 0.05f..0.3f)
        private val swingProgressScale by float("SwingScale", 0.9f, 0.1f..1.0f)

        override fun transform(matrices: MatrixStack, arm: Arm, equipProgress: Float, swingProgress: Float) {
            matrices.translate(if (arm == Arm.RIGHT) -0.1f else 0.1f, translateY, 0.0f)
            applySwingOffset(matrices, arm, swingProgress * swingProgressScale)
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25f))
            matrices.multiply(
                (if (arm == Arm.RIGHT) RotationAxis.POSITIVE_Y else RotationAxis.NEGATIVE_Y)
                    .rotationDegrees(13.365f)
            )
            matrices.multiply(
                (if (arm == Arm.RIGHT) RotationAxis.POSITIVE_Z else RotationAxis.NEGATIVE_Z)
                    .rotationDegrees(78.05f)
            )
        }

    }

    /**
     * Based on the [applySwingOffset] but with a different transformation
     * during swing progress to make it look like the [PushdownAnimation] from LiquidBounce Legacy.
     *
     * This animation is not the same as the original, but it is similar.
     */
    object PushdownAnimation : AnimationChoice("Pushdown") {

        override fun transform(matrices: MatrixStack, arm: Arm, equipProgress: Float, swingProgress: Float) {
            matrices.translate(if (arm == Arm.RIGHT) -0.1f else 0.1f, 0.1f, 0.0f)

            val g = MathHelper.sin(MathHelper.sqrt(swingProgress) * Math.PI.toFloat())
            matrices.multiply(
                RotationAxis.POSITIVE_Z.rotationDegrees(
                    (if (arm == Arm.RIGHT) 1 else -1) * g * 10.0f
                )
            )
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -35.0f))

            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25f))
            matrices.multiply(
                (if (arm == Arm.RIGHT) RotationAxis.POSITIVE_Y else RotationAxis.NEGATIVE_Y)
                    .rotationDegrees(13.365f)
            )
            matrices.multiply(
                (if (arm == Arm.RIGHT) RotationAxis.POSITIVE_Z else RotationAxis.NEGATIVE_Z)
                    .rotationDegrees(78.05f)
            )
        }

    }

}
