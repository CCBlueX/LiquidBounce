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

package net.ccbluex.liquidbounce.features.module.modules.render.totemeffect

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.ModuleTotemEffect.coords
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.world.phys.Vec3

abstract class TotemEffectMode(name:String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = ModuleTotemEffect.modes

    val lifetime by int("lifetime", 20, 1..200)
    protected val fade by float("Fade", 0.7f, 0f..1f)
    protected val animCurve by easing("AnimCurve", Easing.EXPONENTIAL_OUT)
    protected val canBeCovered by boolean("CanBeCovered", false)

    protected abstract fun WorldRenderEnvironment.drawTotemEffect(
        progress: Float,
        age: Float,
        pos: Vec3,
        event: WorldRenderEvent
    )

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            coords.forEach {
                withPositionRelativeToCamera(it.value) {
                    val age = lifetime - coords.timeToDie(it) + event.partialTicks

                    val progress = animCurve
                        .transform(age / lifetime)
                        .coerceIn(0f, 1f)

                    poseStack.withPush {
                        drawTotemEffect(progress, age, it.value, event)
                    }
                }
            }
        }
    }
}
