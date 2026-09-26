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

package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect.circles
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect.modes
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.math.Easing

abstract class JumpEffectMode(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = modes

    protected val endRadius by floatRange("EndRadius", 0.15F..0.8F, 0F..3F)

    protected val animCurve by easing("AnimCurve", Easing.QUAD_OUT)

    protected val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)

    val lifetime by intRange("Lifetime", 15..15, 1..120, "anim/life")
    protected val canBeCovered by boolean("CanBeCovered", false)

    protected abstract fun WorldRenderEnvironment.drawJumpEffect(progress: Float, age: Float)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            circles.forEach {
                val age = lifetime.last - circles.timeToDie(it) + event.partialTicks

                val progress = animCurve
                    .transform(age / lifetime.first)
                    .coerceIn(0f, 1f)

                // small offset to exclude artifacts
                withPositionRelativeToCamera(it.value.add(0.0, 0.01, 0.0)) {
                    poseStack.withPush {
                        drawJumpEffect(progress, age)
                    }
                }
            }
        }
    }

    fun animateColor(baseColor: Color4b, age: Float): Color4b {
        val fadeProgress = when {
            lifetime.last > lifetime.first ->
                ((age - lifetime.first) / (lifetime.last - lifetime.first).toFloat()).coerceIn(0f, 1f)
            else -> (age / lifetime.last.toFloat()).coerceIn(0f, 1f)
        }

        val color = baseColor.fade(1.0f - fadeProgress)

        if (hueOffsetAnim == 0) return color

        val lifeProgress = (age / lifetime.last.toFloat()).coerceIn(0f, 1f)
        return shiftHue(color, (hueOffsetAnim * lifeProgress).toInt())
    }

}
