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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.RangedValue

/**
 * Provides a ranged value to a submodule.
 * This has the advantage that the value can be either registered in the module or in the submodule.
 */
sealed interface RangedValueProvider {

    /**
     * Offers the provider to register to the configurable.
     *
     * @return The ranged value.
     */
    fun register(offeredConfigurable: Configurable): RangedValue<*>?

}

/**
 * Just returns the [value]; expects the value to be already registered elsewhere.
 */
class DummyRangedValueProvider(private val value: RangedValue<*>) : RangedValueProvider {

    override fun register(offeredConfigurable: Configurable) = value

}

/**
 * Does nothing; Has no value.
 */
data object NoneRangedValueProvider : RangedValueProvider {

    override fun register(offeredConfigurable: Configurable) = null

}

/**
 * [Configurable.float] registered to the submodule directly.
 */
class FloatValueProvider(
    val name: String,
    val default: Float,
    val range: ClosedFloatingPointRange<Float>,
    val suffix: String = ""
) : RangedValueProvider {

    override fun register(offeredConfigurable: Configurable) : RangedValue<*> {
        return offeredConfigurable.float(name, default, range, suffix)
    }

}
