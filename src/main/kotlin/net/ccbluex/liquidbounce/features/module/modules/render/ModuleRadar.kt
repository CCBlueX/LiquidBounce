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

import net.ccbluex.liquidbounce.config.types.RangedValue.Companion.squared
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
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
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
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

    private val radius by float("Radius", 40f, 2f..200f)

    private val pointer = object : Configurable("Pointer") {
        val width by float("Width", 10f, 1f..100f)
        val height by float("Height", 10f, 1f..100f)
    }

    private val distanceRangeSq by floatRange("Distance", 0F..128F, 0F..512F).squared()

    // TODO: color option
    // TODO: distance-based alpha

    init {
        tree(pointer)
    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        super.onEnabled()
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        super.onDisabled()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        with(it.context) {
            pose().withPush {
                val (width, height) = mc.window.scaledDimension
                translate(width * 0.5f, height * 0.5f)

                val yawRad = player.getYRot(it.tickDelta).toRadians()
                val playerPos = player.interpolateCurrentPosition(it.tickDelta)

                rotate(-yawRad)

                for (entity in RenderedEntities) {
                    if (entity === player) continue
                    val entityPos = entity.interpolateCurrentPosition(it.tickDelta)

                    if (entityPos.cameraDistanceSq() !in distanceRangeSq) continue

                    val diffX = entityPos.x - playerPos.x
                    val diffZ = entityPos.z - playerPos.z

                    withPush {
                        rotate(atan2(diffZ, diffX).toFloat() + Mth.HALF_PI)
                        translate(0f, radius)
                        drawTriangle(
                            Vec2(-pointer.width * 0.5f, 0f),
                            Vec2(0f, pointer.height),
                            Vec2(pointer.width * 0.5f, 0f),
                            fillColor = Color4b.WHITE
                        )
                    }
                }
            }
        }
    }

}
