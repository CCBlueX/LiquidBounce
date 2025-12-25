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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.scaledDimension
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.atan2

/**
 * Radar module
 *
 * Shows the direction of rendered entities on GUI.
 */
object ModuleRadar : ClientModule("Radar", Category.RENDER, aliases = listOf("PointerESP")) {

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        super.onEnabled()
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        super.onDisabled()
    }

    private val render2D = handler<OverlayRenderEvent> {
        with(it.context) {
            pose().withPush {
                val (width, height) = mc.window.scaledDimension
                translate(width / 2f, height / 2f)

                val yaw = player.getYRot(it.tickDelta)
                val playerPos = player.interpolateCurrentPosition(it.tickDelta)

                pose().rotate(-yaw.toRadians())

                for (entity in RenderedEntities) {
                    if (entity === player) continue

                    withPush {
                        val entityPos = entity.interpolateCurrentPosition(it.tickDelta)
                        val diffX = entityPos.x - playerPos.x
                        val diffZ = entityPos.z - playerPos.z
                        rotate(atan2(diffZ, diffX).toFloat() + Mth.HALF_PI)
                        translate(0f, 10f)
                        drawTriangle(
                            Vec2(-5f, 0f),
                            Vec2(0f, 10f),
                            Vec2(5f, 0f),
                            fillColor = Color4b.WHITE
                        )
                    }
                }
            }
        }
    }

}
