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

package net.ccbluex.liquidbounce.gametest.navigation

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkPath
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The player as the client has it in one tick, and the path the walker follows.
 */
internal class ParkourSample(
    val position: Vec3,
    val onGround: Boolean,
    val dead: Boolean,
    val path: WalkPath?,
)

/**
 * Follows one stage tick by tick and decides when it ends and how.
 */
internal class StageRecorder(val stage: ParkourStage, first: ParkourSample) {

    /**
     * 2 s per block of straight distance, at least 30 s.
     */
    val limit = max(MIN_TICKS, (stage.straightDistance * TICKS_PER_BLOCK).toInt())

    private val floor = min(stage.start.y, stage.finish.y) - FALL_DEPTH
    private val goal = stage.goal
    private val visited = LongOpenHashSet()
    private val trace = ArrayDeque<List<Double>>()

    var last = first
        private set
    var tick = 0
        private set
    var travelled = 0.0
        private set
    var closest = first.position.distanceTo(goal)
        private set
    var falls = 0
        private set
    var pathFound = false
        private set

    /**
     * Where the player last stood, the path it had when it left the ground there, and the last path at all.
     */
    var lastGround: Vec3? = first.position
        private set
    var takeoffPath: WalkPath? = first.path
        private set
    var lastPath: WalkPath? = first.path
        private set

    var outcome: StageOutcome? = null
        private set
    var detail = ""
        private set

    private var progressTick = 0
    private var nearFinishTick = Int.MIN_VALUE
    private var arrivedTicks = 0

    val positions: List<List<Double>>
        get() = trace.toList()

    fun update(now: ParkourSample) {
        tick++
        val step = now.position.subtract(last.position)
        val teleported = step.horizontalDistance() > TELEPORT_HORIZONTAL || abs(step.y) > TELEPORT_VERTICAL
        now.path?.let {
            lastPath = it
            pathFound = true
        }
        if (tick % TRACE_INTERVAL == 0) {
            trace.addLast(now.position.rounded())
            if (trace.size > TRACE_SIZE) {
                trace.removeFirst()
            }
        }
        if (!teleported) {
            travelled += step.length()
            trackGround(now)
        }
        trackProgress(now)

        verdict(now, teleported)?.let { (outcome, detail) ->
            this.outcome = outcome
            this.detail = detail
        }
        // A teleport ends the stage; where it happened matters, not where it led
        if (!teleported) {
            last = now
        }
    }

    private fun trackGround(now: ParkourSample) {
        if (last.onGround && !now.onGround) {
            takeoffPath = last.path ?: lastPath
        }
        if (now.onGround) {
            val ground = lastGround
            if (!last.onGround && ground != null && ground.y - now.position.y >= FALL_HEIGHT) {
                falls++
            }
            lastGround = now.position
        }
    }

    private fun trackProgress(now: ParkourSample) {
        val distance = now.position.distanceTo(goal)
        if (distance <= NEAR_FINISH) {
            nearFinishTick = tick
        }
        val feet = BlockPos.containing(now.position)
        if (distance < closest - PROGRESS || visited.add(feet.asLong())) {
            progressTick = tick
        }
        closest = min(closest, distance)
        arrivedTicks = if (reached(feet)) arrivedTicks + 1 else 0
    }

    private fun verdict(now: ParkourSample, teleported: Boolean): Pair<StageOutcome, String>? = when {
        teleported && tick - nearFinishTick <= TELEPORT_GRACE ->
            StageOutcome.PASS to "the map teleported the player at the finish"

        arrivedTicks > 0 && (now.onGround || arrivedTicks >= ARRIVAL_TICKS) ->
            StageOutcome.PASS to "reached the finish"

        now.dead -> StageOutcome.FELL to "died"

        teleported ->
            StageOutcome.FELL to "the map teleported the player from ${last.position.rounded()} to " +
                "${now.position.rounded()}"

        now.position.y < floor -> StageOutcome.FELL to "fell below y $floor"
        tick - progressTick >= STUCK_TICKS ->
            StageOutcome.STUCK to if (pathFound) "no progress for ${STUCK_TICKS / TICKS_PER_SECOND} s" else "no path"

        tick >= limit -> StageOutcome.TIMEOUT to "not there after $limit ticks"
        else -> null
    }

    /**
     * Whether the feet block is within the finish radius, measured like the walker's goal.
     */
    private fun reached(feet: BlockPos): Boolean {
        val dx = feet.x + 0.5 - goal.x
        val dy = (feet.y - stage.finish.y).toDouble()
        val dz = feet.z + 0.5 - goal.z
        return sqrt(dx * dx + dy * dy + dz * dz) <= stage.finishRadius
    }

    private companion object {
        const val MIN_TICKS = 600
        const val TICKS_PER_BLOCK = 40.0
        const val TICKS_PER_SECOND = 20
        const val FALL_DEPTH = 3
        const val FALL_HEIGHT = 2.0
        const val NEAR_FINISH = 3.0
        const val PROGRESS = 1.0
        const val STUCK_TICKS = 300
        const val TELEPORT_HORIZONTAL = 2.5
        const val TELEPORT_VERTICAL = 4.5
        const val TELEPORT_GRACE = 10
        const val ARRIVAL_TICKS = 3
        const val TRACE_INTERVAL = 5
        const val TRACE_SIZE = 80
    }

}
