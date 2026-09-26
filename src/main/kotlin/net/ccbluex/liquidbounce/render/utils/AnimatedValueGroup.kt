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

package net.ccbluex.liquidbounce.render.utils

import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs

abstract class AnimatedValueGroup(name: String) : ValueGroup(name) {
    protected abstract val curve: CurveValue
    private val period by int("Period", 1000, 10..20000, "ms")
    private val symmetric by boolean("Symmetric", true)

    fun current(): Float = if (symmetric) {
        val p = (clientStartDurationMs % (period * 2)) / period.toFloat()
        this.curve.transform(if (p > 1) 2f - p else p)
    } else {
        this.curve.transform((clientStartDurationMs % period) / period.toFloat())
    }
}
