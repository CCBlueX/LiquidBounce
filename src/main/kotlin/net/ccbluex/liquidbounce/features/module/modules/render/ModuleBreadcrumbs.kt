/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.utils.rainbow
import org.lwjgl.opengl.GL11.*

object ModuleBreadcrumbs : Module("Breadcrumbs", Category.RENDER) {

    private val color by color("Color", Color4b(255, 179, 72, 255))
    private val colorRainbow by boolean("Rainbow", false)

    private val positions = mutableListOf<Double>()
    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    override fun enable() {
        synchronized(positions) {
            positions.addAll(listOf(player.x, player.eyeY, player.z - 1.0))
            positions.addAll(listOf(player.x, player.y, player.z - 1.0))
        }
    }

    override fun disable() {
        synchronized(positions) {
            positions.clear()
        }
    }

    val renderHandler = handler<EngineRenderEvent> {
        val color = if (colorRainbow) rainbow() else color

        synchronized(positions) {
            RenderEngine.enqueueForRendering(
                RenderEngine.CAMERA_VIEW_LAYER_WITHOUT_BOBBING,
                createBreadcrumbsRenderTask(
                    color,
                    positions
                )
            )
        }
    }

    @JvmStatic
    internal fun createBreadcrumbsRenderTask(color: Color4b, positions: List<Double>): ColoredPrimitiveRenderTask {
        val renderTask = ColoredPrimitiveRenderTask(positions.size, PrimitiveType.LineStrip)

        for (i in 0 until positions.size / 3) {
            renderTask.index(
                renderTask.vertex(
                    Vec3(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]),
                    color
                )
            )
        }

        return renderTask
    }

    val updateHandler = handler<PlayerTickEvent> {
        if (player.x == lastPosX && player.y == lastPosY && player.z == lastPosZ) {
            return@handler
        }

        lastPosX = player.x
        lastPosY = player.y
        lastPosZ = player.z

        synchronized(positions) {
            positions.addAll(listOf(player.x, player.y, player.z - 1.0))
        }
    }

}
