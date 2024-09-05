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

import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.Curves
import net.minecraft.util.math.Vec3d
import org.apache.commons.lang3.tuple.MutablePair

@IncludeModule
object ModuleJumpEffect : Module("JumpEffect", Category.RENDER) {
    private val endRadius by floatRange("EndRadius", 0.15F..0.8F, 0F..3F)

    private val innerColor by color("InnerColor", Color4b(0, 255, 4, 0))
    private val outerColor by color("OuterColor", Color4b(0, 255, 4, 89))

    private val animCurve by curve("AnimCurve", Curves.EASE_OUT)

    private val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)

    private val lifetime by int("Lifetime", 15, 1..30)

    private var circles = arrayListOf<MutablePair<Vec3d, Long>>()

    val repeatable = repeatable {
        circles.forEach { it.right += 1 }
        circles.removeIf { it.getRight() >= lifetime }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            circles.forEach {
                val progress = animCurve
                    .at((it.right + event.partialTicks) / lifetime)
                    .coerceIn(0f..1f)

                withPositionRelativeToCamera(it.left) {
                    drawGradientCircle(
                        endRadius.endInclusive * progress,
                        endRadius.start * progress,
                        animateColor(outerColor, progress),
                        animateColor(innerColor, progress)
                    )
                }
            }
        }

    }


    private fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val color = baseColor.alpha((baseColor.a * (1 - progress)).toInt())
        if(hueOffsetAnim == 0){
            return color
        }
        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    @Suppress("unused")
    val onJump = handler<PlayerJumpEvent> { _ ->
        // Adds new circle when the player jumps
        circles.add(MutablePair(player.pos, 0L))
    }

}
