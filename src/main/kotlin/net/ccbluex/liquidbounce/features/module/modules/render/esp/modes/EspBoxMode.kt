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
package net.ccbluex.liquidbounce.features.module.modules.render.esp.modes

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.world.phys.AABB

object EspBoxMode : EspMode("Box") {

    private val outline by boolean("Outline", true)
    private val expand by float("Expand", 0.05f, 0f..0.5f)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            startBatch()
            for (entity in RenderedEntities) {
                val dimensions = entity.getDimensions(entity.pose)
                val d = dimensions.width.toDouble() / 2.0
                val box = AABB(-d, 0.0, -d, d, dimensions.height.toDouble(), d).inflate(expand.toDouble())

                val pos = entity.interpolateCurrentPosition(event.partialTicks)
                val color = getColor(entity)

                val baseColor = color.with(a = 50)
                val outlineColor = color.with(a = 100)

                withPositionRelativeToCamera(pos) {
                    drawBox(
                        box,
                        baseColor,
                        outlineColor.takeIf { outline }
                    )
                }
            }
            commitBatch()
        }
    }

}
