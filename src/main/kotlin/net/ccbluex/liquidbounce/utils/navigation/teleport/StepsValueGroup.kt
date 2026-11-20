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

import net.ccbluex.agent.navigation.teleport.Options
import net.ccbluex.agent.navigation.teleport.Pacing
import net.ccbluex.agent.pathfinder.teleport.Route
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged

class StepsValueGroup : ValueGroup("Steps") {

    private val length by float("Length", Options.DEFAULT_STEP_LENGTH.toFloat(), 1f..9.5f, "blocks")
    private val pace by enumChoice("Pacing", Pace.BATCHES)
    private val packetsPerTick by int(
        "PerTick", Pacing.DEFAULT_PACKETS_PER_TICK, 1..Route.MAX_PACKETS_PER_TICK, "packets",
    ).visibleWhen { pace == Pace.BATCHES }

    fun options() = Options(length.toDouble(), pace.pacing(packetsPerTick))

    /** The ticks the packets of [route] take to go out. */
    fun ticks(route: Route) = if (pace == Pace.BATCHES) route.batches(packetsPerTick).size else 1

    private enum class Pace(override val tag: String) : Tagged {
        BATCHES("Batches"),
        BURST("Burst"),
        INSTANT("Instant");

        fun pacing(packetsPerTick: Int) = when (this) {
            BATCHES -> Pacing.Batches(packetsPerTick)
            BURST -> Pacing.Burst
            INSTANT -> Pacing.Instant
        }
    }

}
