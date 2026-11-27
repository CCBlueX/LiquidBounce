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

    /**
     * Packets a tick, used by [Pace.BATCHES] only; the other pacings ignore it.
     *
     * Deliberately **not** hidden outside `Batches`: `visibleWhen` works by dropping the value from the
     * serialised list, and the click GUI does not re-add a setting that disappeared once — it stays
     * missing after switching back to `Batches` until the game restarts.
     */
    private val packetsPerTick by int(
        "PerTick", Pacing.DEFAULT_PACKETS_PER_TICK, 1..Route.MAX_PACKETS_PER_TICK, "packets",
    )

    /**
     * Sends descending steps as on ground, so the server never banks the fall distance. Off by default, as it
     * was in the module this grew out of: a strict server reads a descent that claims ground as an invalid move.
     */
    private val spoofOnGround by boolean("SpoofOnGround", false)

    fun options() = Options(
        stepLength = length.toDouble(),
        pacing = pace.pacing(packetsPerTick),
        shouldClaimGroundOnDescent = spoofOnGround,
    )

    /** The pacing name, for diagnostics. */
    val paceName: String
        get() = pace.tag

    val isBurst: Boolean
        get() = pace == Pace.BURST

    val isInstant: Boolean
        get() = pace == Pace.INSTANT

    /** The ticks the packets of [route] take to go out. */
    fun ticks(route: Route) = if (pace == Pace.BATCHES) route.batches(packetsPerTick).size else 1

    /** How many batches [route] goes out in, for the dispatch log. */
    fun batchCount(route: Route): Int = ticks(route)

    /** The sizes of the batches [route] is cut into, for the per-tick diagnostics. */
    fun batchSizes(route: Route): IntArray =
        if (pace == Pace.BATCHES) route.batches(packetsPerTick) else intArrayOf(route.steps.size)

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
