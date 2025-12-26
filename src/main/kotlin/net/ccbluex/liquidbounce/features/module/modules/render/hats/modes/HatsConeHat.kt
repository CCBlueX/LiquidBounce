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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.modes

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import org.joml.Vector3f

/**
 * @author minecrrrr
 */
object HatsConeHat : HatsMode("Cone") {


    private val height by float("HeightOffset", 0.1f, 0f..1f)

    private val color by color("Color", Color4b(0, 0, 255, 125))

    private object HatSettings : Configurable("HatSettings") {
        val radius by float("Radius", 0.6f, 0.1f..2f)
        val peak by float("Peak", 0.3f, 0.01f..2f)
        val showInFirstPerson by boolean("FirstPersonView", true)
    }

    init {
        tree(HatSettings)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>{
        val player = mc.player ?: return@handler

        // Check if the cone should be rendered in first-person view.
        if (mc.options.cameraType.isFirstPerson && !HatSettings.showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            
            // Get the player's interpolated position for smooth rendering.
            val pos = player.interpolateCurrentPosition(it.partialTicks)

            // Create an offset vector for the cone's peak.
            val peakOffset = Vector3f(0f, HatSettings.peak, 0f)

            // Translate render position relative to the camera and player's head.
            withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {

                // Draw a gradient circle forming the cone base with the apex at peakOffset.
                drawGradientCircle(
                    outerRadius = HatSettings.radius,
                    innerRadius = 0f,
                    outerColor = color,
                    innerColor = color,
                    innerOffset = peakOffset,
                )
            }
        }
    }
}
