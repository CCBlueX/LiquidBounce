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

    private val length by float(
        "StepLength", DEFAULT_STEP_LENGTH.toFloat(), 1f..9.5f, "blocks", aliases = listOf("Length"),
    )
    val packetsPerTick by int(
        "PacketsPerTick", DEFAULT_PACKETS_PER_TICK, 1..Route.MAX_PACKETS_PER_TICK, "packets",
        aliases = listOf("PerTick"),
    )

    /**
     * Reports every descending step as standing, so the server never banks a fall over the height the
     * route drops: it settles zero damage instead of the whole difference when the route lands again.
     */
    val spoofOnGround by boolean("SpoofOnGround", false)

    val stepLength: Double
        get() = length.toDouble()

    companion object {
        const val DEFAULT_STEP_LENGTH = 5.0
        const val DEFAULT_PACKETS_PER_TICK = 3
    }

}
