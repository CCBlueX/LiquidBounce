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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

/**
 * A full bright module
 *
 * Allows you to see in the dark.
 */
object ModuleFullBright : ClientModule("FullBright", Category.RENDER) {

    private val modes = choices(
        "Mode", FullBrightGamma, arrayOf(
            FullBrightGamma, FullBrightNightVision
        )
    )

    object FullBrightGamma : Choice("Gamma") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val brightness by int("Brightness", 15, 1..15)

        var gamma = 0.0

        override fun enable() {
            gamma = mc.options.gamma.value
        }

        val tickHandler = handler<PlayerPostTickEvent> {
            if (gamma < brightness) {
                gamma = (gamma + 0.1).coerceAtMost(brightness.toDouble())
            }
        }

    }

    private object FullBrightNightVision : Choice("NightVision") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val tickHandler = handler<PlayerPostTickEvent> {
            player.addStatusEffect(StatusEffectInstance(StatusEffects.NIGHT_VISION, 1337))
        }

        override fun disable() {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
        }

    }

}
