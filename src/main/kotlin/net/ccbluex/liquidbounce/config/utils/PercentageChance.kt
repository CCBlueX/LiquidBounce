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

package net.ccbluex.liquidbounce.config.utils

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import java.util.concurrent.ThreadLocalRandom
import java.util.function.BooleanSupplier
import java.util.function.Supplier
import java.util.random.RandomGenerator
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PercentageChance<T : Number>(
    private val value: Value<T>,
    private val randomGetter: Supplier<out RandomGenerator>,
) : BooleanSupplier, ReadOnlyProperty<Any?, Boolean> {
    val percentage: Float
        get() = value.get().toFloat()

    val isEnabled: Boolean
        get() = percentage > 0f

    override fun getAsBoolean(): Boolean = when (val v = percentage) {
        0f -> false
        100f -> true
        else -> randomGetter.get().nextFloat(100f) < v
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        this.asBoolean
}

private val DEFAULT_RANDOM = Supplier(ThreadLocalRandom::current)

fun ValueGroup.percentageChance(
    name: String,
    default: Int,
    range: IntRange = 0..100,
    randomGetter: Supplier<out RandomGenerator> = DEFAULT_RANDOM,
): PercentageChance<Int> =
    PercentageChance(int(name, default, range, "%"), randomGetter)

fun ValueGroup.percentageChance(
    name: String,
    default: Float,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    randomGetter: Supplier<out RandomGenerator> = DEFAULT_RANDOM,
): PercentageChance<Float> =
    PercentageChance(float(name, default, range, "%"), randomGetter)
