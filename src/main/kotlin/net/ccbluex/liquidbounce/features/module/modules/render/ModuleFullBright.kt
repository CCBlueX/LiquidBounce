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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects

/**
 * A full bright module
 *
 * Allows you to see in the dark.
 */
object ModuleFullBright : ClientModule("FullBright", ModuleCategories.RENDER) {

    private val modes = choices(
        "Mode", FullBrightGamma, arrayOf(
            FullBrightGamma, FullBrightNightVision
        )
    )

    object FullBrightGamma : Mode("Gamma") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        val brightness by int("Brightness", 15, 1..15)

        var gamma = 0.0

        override fun enable() {
            gamma = mc.options.gamma().get()
        }

        val tickHandler = handler<PlayerPostTickEvent> {
            if (gamma < brightness) {
                gamma = (gamma + 0.1).coerceAtMost(brightness.toDouble())
            }
        }

    }

    private object FullBrightNightVision : Mode("NightVision") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        val tickHandler = handler<PlayerPostTickEvent> {
            player.addEffect(MobEffectInstance(MobEffects.NIGHT_VISION, 1337))
        }

        override fun disable() {
            player.removeEffect(MobEffects.NIGHT_VISION)
        }

    }

}
