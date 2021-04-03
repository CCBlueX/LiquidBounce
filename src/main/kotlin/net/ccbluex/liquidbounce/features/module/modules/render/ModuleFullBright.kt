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

import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.sequenceHandler
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

/**
 * A full bright module
 *
 * Allows you to see in the dark
 */
object ModuleFullBright : Module("FullBright", Category.RENDER) {
    private val modes = choices("Mode", "Gamma") {
        FullBrightGamma
        FullBrightNightVision
    }

    private object FullBrightGamma : Choice("Gamma", modes) {
        private var prevValue: Double = 0.0

        override fun enable() {
            prevValue = mc.options.gamma
        }

        val tickHandler = sequenceHandler<PlayerTickEvent> {
            if(mc.options.gamma <= 100) mc.options.gamma++
        }

        override fun disable() {
            mc.options.gamma = prevValue
        }
    }

    private object FullBrightNightVision : Choice("Night Vision", modes) {
        val tickHandler = sequenceHandler<PlayerTickEvent> {
            player.addStatusEffect(StatusEffectInstance(StatusEffects.NIGHT_VISION, 1337))
        }

        override fun disable() {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
        }
    }
}
