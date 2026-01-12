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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * Reach module
 *
 * Increases your reach.
 */

object ModuleReach : ClientModule("Reach", Category.PLAYER) {

    class InteractionRange(name: String, default: Float) : ToggleableConfigurable(this, name, true) {
        private val mode by enumChoice("Mode", RangeMode.OVERRIDE)
        private val reach by float("Range", default, 0f..16f)

        fun modifyRange(original: Double): Double {
            if (!running) return original

            return when (mode) {
                RangeMode.EXTEND -> original + reach.toDouble()
                RangeMode.OVERRIDE -> reach.toDouble()
            }
        }
    }

    val entityInteractionRange = InteractionRange("EntityInteractionRange", 4.2f)
    val blockInteractionRange = InteractionRange("BlockInteractionRange", 5f)

    private enum class RangeMode(override val choiceName: String) : NamedChoice {
        EXTEND("Extend"),
        OVERRIDE("Override"),
    }
}
