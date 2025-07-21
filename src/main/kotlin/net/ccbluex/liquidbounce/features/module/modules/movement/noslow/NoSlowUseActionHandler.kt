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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow

import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair
import it.unimi.dsi.fastutil.floats.FloatFloatPair
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable

abstract class NoSlowUseActionHandler(name: String) : ToggleableConfigurable(ModuleNoSlow, name, true) {

    private val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
    private val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)

    companion object {
        val DEFAULT_USE_MUL = FloatFloatImmutablePair(0.2f, 0.2f)
    }

    open fun getMultiplier() : FloatFloatPair {
        if (!this.enabled) {
            return DEFAULT_USE_MUL
        }

        return FloatFloatImmutablePair(forwardMultiplier, sidewaysMultiplier)
    }

}
