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
package net.ccbluex.liquidbounce.utils.navigation.teleport

import net.ccbluex.liquidbounce.config.types.group.ValueGroup

class StepsValueGroup : ValueGroup("Steps") {

    private val length by float("Length", DEFAULT_STEP_LENGTH.toFloat(), 1f..9.5f, "blocks")
    val packetsPerTick by int("PerTick", DEFAULT_PACKETS_PER_TICK, 1..Route.MAX_PACKETS_PER_TICK, "packets")

    val stepLength: Double
        get() = length.toDouble()

    companion object {
        const val DEFAULT_STEP_LENGTH = 5.0
        const val DEFAULT_PACKETS_PER_TICK = 3
    }

}
