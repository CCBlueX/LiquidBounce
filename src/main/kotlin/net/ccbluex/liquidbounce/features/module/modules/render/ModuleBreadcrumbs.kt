/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition

/**
 * Breadcrumbs module
 *
 * Leaves a trace behind you.
 */

object ModuleBreadcrumbs : Module("Breadcrumbs", Category.RENDER) {

    private val color by color("Color", Color4b(255, 179, 72, 255))
    private val colorRainbow by boolean("Rainbow", false)
    private val maxLength by int("MaxLength", 500, 10..5000)

    private val positions = mutableListOf<Double>()
    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    override fun enable() {
        synchronized(positions) {
            positions.addAll(listOf(player.x, player.eyeY, player.z))
            positions.addAll(listOf(player.x, player.y, player.z))
        }
    }

    override fun disable() {
        synchronized(positions) {
            positions.clear()
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val color = if (colorRainbow) rainbow() else color

        synchronized(positions) {
            renderEnvironmentForWorld(matrixStack) {
                withColor(color) {
                    drawLineStrip(*makeLines(color, positions, event.partialTicks))
                }
            }
        }
    }

    @JvmStatic
    internal fun makeLines(color: Color4b, positions: List<Double>, tickDelta: Float): Array<Vec3> {
        val mutableList = mutableListOf<Vec3>()
        for (i in 0 until positions.size / 3 - 1) {
            mutableList += Vec3(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2])
        }
        mutableList += player.interpolateCurrentPosition(tickDelta)
        return mutableList.toTypedArray()
    }

    val updateHandler = handler<PlayerTickEvent> {
        if (player.x == lastPosX && player.y == lastPosY && player.z == lastPosZ) {
            return@handler
        }

        lastPosX = player.x
        lastPosY = player.y
        lastPosZ = player.z

        synchronized(positions) {
            if (positions.size > maxLength * 3) {
                positions.subList(0, positions.size - maxLength * 3).clear()
            }
            positions.addAll(listOf(player.x, player.y, player.z))
        }
    }

}
