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
internal object HatsConeHat : HatsMode("Cone") {


    private val height by float("HeightOffset", 0.1f, 0f..1f)

    private object Colors : Configurable("Colors") {
        val syncColors by boolean("SyncColors", true)
        val innerColor by color("InnerColor", Color4b(0, 0, 255, 125))
        val outerColor by color("OuterColor", Color4b(0, 0, 255, 125))
    }


    private object HatConeSettings : Configurable("HatSettings") {
        object RadiusSettings : Configurable("RadiusSettings") {
            val outerRadius by float("OuterRadius", 0.6f, 0.1f..2f)
            val innerRadius by float("InnerRadius", 0f, 0f..1f)

        }
        val peak by float("Peak", 0.3f, 0.01f..2f)
        val showInFirstPerson by boolean("FirstPersonView", true)
    }

    init {
        tree(HatConeSettings)
        tree(HatConeSettings.RadiusSettings)
        tree(Colors)
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent>{
        val player = mc.player ?: return@handler

        if (mc.options.cameraType.isFirstPerson && !HatConeSettings.showInFirstPerson) return@handler

        renderEnvironmentForWorld(it.matrixStack) {

            val pos = player.interpolateCurrentPosition(it.partialTicks)

            val peakOffset = Vector3f(0f, HatConeSettings.peak, 0f)

            withPositionRelativeToCamera(pos.add(0.0, player.bbHeight + height.toDouble(), 0.0)) {

                // Draw a gradient circle forming the cone base with the apex at peakOffset.
                drawGradientCircle(
                    outerRadius = HatConeSettings.RadiusSettings.outerRadius,
                    innerRadius = HatConeSettings.RadiusSettings.innerRadius,
                    outerColor = Colors.outerColor,
                    innerColor = if(!Colors.syncColors)Colors.innerColor else Colors.outerColor,
                    innerOffset = peakOffset,
                )
            }
        }
    }
}
