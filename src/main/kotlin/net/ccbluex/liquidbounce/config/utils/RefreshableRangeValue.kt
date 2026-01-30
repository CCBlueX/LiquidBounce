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
import net.ccbluex.liquidbounce.utils.kotlin.random

interface RefreshableIntState {
    val current: Int
    fun refresh()
}

interface RefreshableFloatState {
    val current: Float
    fun refresh()
}

fun Value<IntRange>.asRefreshable(): RefreshableIntState = object : RefreshableIntState {
    init {
        onChanged { refresh() }
    }

    override var current = get().random()
        private set

    override fun refresh() {
        current = get().random()
    }
}

fun Value<ClosedFloatingPointRange<Float>>.asRefreshable(): RefreshableFloatState = object : RefreshableFloatState {
    init {
        onChanged { refresh() }
    }

    override var current = get().random()
        private set

    override fun refresh() {
        current = get().random()
    }
}
