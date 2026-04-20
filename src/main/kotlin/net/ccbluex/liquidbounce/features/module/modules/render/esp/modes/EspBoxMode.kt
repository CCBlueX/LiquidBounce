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

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP.getColor
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.math.KeyedAabb
import net.ccbluex.liquidbounce.utils.math.mergeIntersectingAabbsSweep
import net.ccbluex.liquidbounce.utils.math.worldToLocal
import net.minecraft.world.phys.AABB

object EspBoxMode : EspMode.BoxBased("Box") {

    private val outline by boolean("Outline", true)
    private val mergeIntersecting by boolean("MergeIntersecting", false)

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            val preparedBoxes = collectPreparedBoxes(event.partialTicks)

            if (!mergeIntersecting) {
                for ((entity, localBox, position) in preparedBoxes) {
                    withPositionRelativeToCamera(position) {
                        drawColoredBox(localBox, getColor(entity))
                    }
                }
                return@renderEnvironmentForWorld
            }

            val mergedBoxes = mergeIntersectingAabbsSweep(
                preparedBoxes.mapToArray { (entity, _, _, worldBox) ->
                    KeyedAabb(worldBox, getColor(entity))
                }.asList()
            )

            for ((box, color) in mergedBoxes) {
                val (origin, localBox) = box.worldToLocal()
                withPositionRelativeToCamera(origin) {
                    drawColoredBox(localBox, color)
                }
            }
        }
    }

    private fun WorldRenderEnvironment.drawColoredBox(box: AABB, color: Color4b) {
        val baseColor = color.with(a = 50)
        val outlineColor = color.with(a = 100).takeIf { outline }
        drawBox(box, baseColor, outlineColor)
    }

}
