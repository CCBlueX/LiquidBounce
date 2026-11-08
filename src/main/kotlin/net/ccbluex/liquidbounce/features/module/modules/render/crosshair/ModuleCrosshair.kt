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

package net.ccbluex.liquidbounce.features.module.modules.render.crosshair

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes.CrosshairCS2
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes.CrosshairCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.world.entity.LivingEntity

object ModuleCrosshair : ClientModule("Crosshair", ModuleCategories.RENDER) {
    val modes =
        choices("Mode", 0) {
            arrayOf(
                CrosshairCircle,
                CrosshairCS2,
            )
        }.apply { tagBy(this) }

    /**
     * Turns the crosshair into [color] for a moment when the entity under it gets hurt.
     */
    object HitFlash : ToggleableValueGroup(this, "HitFlash", false) {
        private val color by color("Color", Color4b(255, 40, 40))
        private val duration by int("Duration", 300, 50..1000, "ms")

        private var target: LivingEntity? = null
        private var targetHurtTime = 0
        private val chronometer = Chronometer()

        /**
         * Strength of the flash right now, from `1` when it starts down to `0`.
         */
        val progress: Float
            get() = (1f - chronometer.elapsed / duration.toFloat()).coerceIn(0f, 1f)

        fun tint(base: Color4b): Color4b = if (running) base.interpolateTo(color, progress) else base

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            val entity = mc.crosshairPickEntity as? LivingEntity
            val hurtTime = entity?.hurtTime ?: 0
            if (entity === target && hurtTime > targetHurtTime) {
                chronometer.reset()
            }
            target = entity
            targetHurtTime = hurtTime
        }

        override fun onDisabled() {
            target = null
            chronometer.reset(0)
        }
    }

    init {
        tree(HitFlash)
    }
}
