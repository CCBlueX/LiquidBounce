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
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.ModuleJumpEffect.modes
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.world.phys.Vec3

abstract class JumpEffectMode(name: String) : Mode(name) {
    final override val parent: ModeValueGroup<*>
        get() = modes

    val endRadius by floatRange("EndRadius", 0.15F..0.8F, 0F..3F)

    val animCurve by easing("AnimCurve", Easing.QUAD_OUT)

    val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)

    val animtime by int("AnimationTime", 15, 1..120)
    val lifetime by int("Lifetime", 20, 1..120)
    val canBeCovered by boolean("CanBeCovered", false)

    val circles = ExpiringList<Vec3>()

    protected abstract fun WorldRenderEnvironment.drawJumpEffect(progress: Float)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            circles.forEach {
                val progress = animCurve
                    .transform((lifetime - circles.timeToDie(it) + event.partialTicks) / animtime)
                    .coerceIn(0f, 1f)

                withPositionRelativeToCamera(it.value) {
                    poseStack.withPush {
                        drawJumpEffect(progress)
                    }
                }
            }
        }
    }

    fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val color = baseColor.fade(1.0F - progress)

        if (hueOffsetAnim == 0){
            return color
        }

        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    @Suppress("unused")
    val playerJumpHandler = handler<PlayerJumpEvent> { _ ->
        // Adds new circle when the player jumps
        circles.add(player.position(), lifetime)
    }

}
