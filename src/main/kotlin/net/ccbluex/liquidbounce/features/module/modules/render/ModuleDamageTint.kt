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

import net.ccbluex.liquidbounce.event.events.EntityHealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * DamageTint module
 *
 * Flashes the screen with a tint color whenever the player takes damage,
 * giving immediate feedback about incoming hits.
 */
object ModuleDamageTint : ClientModule(
    "DamageTint",
    ModuleCategories.RENDER,
    aliases = listOf("HitColor", "HitFlash"),
) {

    private val color by color("Color", Color4b.RED)
    private val opacity by int("Opacity", 50, 1..100, "%")
    private val duration by int("Duration", 250, 100..2000, "ms")

    private var lastDamageTime = 0L
    private var lastHurtTime = 0

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val hurtTime = player.hurtTime
        if (hurtTime > lastHurtTime) {
            lastDamageTime = System.currentTimeMillis()
        }
        lastHurtTime = hurtTime
    }

    @Suppress("unused")
    private val healthUpdateHandler = handler<EntityHealthUpdateEvent> { event ->
        if (event.entity === player && event.new < event.old) {
            lastDamageTime = System.currentTimeMillis()
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val elapsed = System.currentTimeMillis() - lastDamageTime
        if (elapsed >= duration) {
            return@handler
        }

        val fade = 1.0f - elapsed.toFloat() / duration
        val alpha = (color.a * (opacity / 100.0f) * fade).toInt().coerceIn(0, 255)

        event.context.fill(
            0, 0,
            mc.window.guiScaledWidth, mc.window.guiScaledHeight,
            color.alpha(alpha).argb,
        )
    }

}
