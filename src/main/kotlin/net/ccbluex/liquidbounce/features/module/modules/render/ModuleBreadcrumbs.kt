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

import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.int
import net.ccbluex.liquidbounce.event.EntityTickEvent
import net.ccbluex.liquidbounce.event.LiquidBounceRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.renderer.engine.*
import net.ccbluex.liquidbounce.renderer.utils.rainbow
import org.lwjgl.opengl.GL11.*

object ModuleBreadcrumbs : Module("Breadcrumbs", Category.RENDER) {
    private val colorRedValue = int("R", 255, 0..255)
    private val colorGreenValue = int("G", 179, 0..255)
    private val colorBlueValue = int("B", 72, 0..255)
    private val colorRainbow = boolean("Rainbow", false)
    private val positions = ArrayList<Double>()
    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    val renderHandler = handler<LiquidBounceRenderEvent> {
        val color = if (colorRainbow.value) rainbow() else Color4b(
            colorRedValue.value,
            colorGreenValue.value,
            colorBlueValue.value,
            255
        )

        synchronized(positions) {
            val renderTask = ColoredPrimitiveRenderTask(this.positions.size, PrimitiveType.LineStrip)

            for (i in 0 until this.positions.size / 3) {
                renderTask.index(
                    renderTask.vertex(
                        Vec3(
                            this.positions[i * 3],
                            this.positions[i * 3 + 1],
                            this.positions[i * 3 + 2]
                        ), color
                    )
                )
            }

            RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, renderTask)
        }
    }

    val updateHandler = handler<EntityTickEvent> {
        val player = mc.player!!

        if (player.x == this.lastPosX && player.y == this.lastPosY && player.z == this.lastPosZ) {
            return@handler
        }

        this.lastPosX = player.x
        this.lastPosY = player.y
        this.lastPosZ = player.z

        synchronized(positions) {
            positions.addAll(
                listOf(
                    player.x,
                    player.y,
                    player.z
                )
            )
        }
    }


    override fun enable() {
        val thePlayer = mc.player ?: return

        synchronized(positions) {
            positions.addAll(
                listOf(
                    thePlayer.x,
                    thePlayer.eyeY,
                    thePlayer.z
                )
            )

            positions.addAll(listOf(thePlayer.x, thePlayer.y, thePlayer.z))
        }
    }

    override fun disable() {
        synchronized(positions) { positions.clear() }
    }
}
