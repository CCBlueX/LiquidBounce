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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.PlayerStepEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * Step module
 *
 * Allows you to step up blocks.
 */

object ModuleStep : Module("Step", Category.MOVEMENT) {

    var modes = choices("Mode", Instant, arrayOf(Instant, Legit))

    object Legit : Choice("Legit") {
        override val parent: ChoiceConfigurable
            get() = modes
    }

    object Instant : Choice("Instant") {
        override val parent: ChoiceConfigurable
            get() = modes

        private val height by float("Height", 1.0F, 0.6F..5.0F)

        val stepHandler = handler<PlayerStepEvent> {
            it.height = height
        }
    }
}
