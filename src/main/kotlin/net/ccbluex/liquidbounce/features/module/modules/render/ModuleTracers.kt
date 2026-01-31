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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.GenericDistanceHSBColorMode
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.drawLinesWithWidth
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.longLines
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toVec3f

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : ClientModule("Tracers", ModuleCategories.RENDER) {

    private val modes = choices("ColorMode", 0) {
        arrayOf(
            GenericDistanceHSBColorMode.entity(it),
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b(0, 160, 255, 255)),
            GenericRainbowColorMode(it)
        )
    }

    private val lineWidth by float("LineWidth", 1f, 1f..16f)

    private val maximumDistance by float("MaximumDistance", 128F, 1F..512F)

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        if (RenderedEntities.isEmpty()) {
            return@handler
        }

        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            val eyeVector = Vec3f(0.0, 0.0, 1.0)
                .rotateX(-camera.xRot().toRadians())
                .rotateY(-camera.yRot().toRadians())

            longLines {
                startBatch()
                val maxDistanceSq = maximumDistance.sq()
                for (entity in RenderedEntities) {
                    val distanceSq = entity.position().cameraDistanceSq().toFloat()
                    if (distanceSq > maxDistanceSq) {
                        continue
                    }

                    val color = if (FriendManager.isFriend(entity)) {
                        Color4b.BLUE
                    } else {
                        EntityTaggingManager.getTag(entity).color ?: modes.activeMode.getColor(entity)
                    }

                    val pos = relativeToCamera(entity.interpolateCurrentPosition(event.partialTicks)).toVec3f()
                    val topPos = pos.add(0f, entity.bbHeight, 0f)

                    if (lineWidth == 1.0f) {
                        drawLines(color.argb, eyeVector, pos, pos, topPos)
                    } else {
                        drawLinesWithWidth(color.argb, lineWidth, eyeVector, pos, pos, topPos)
                    }
                }
                commitBatch()
            }
        }

    }
}
