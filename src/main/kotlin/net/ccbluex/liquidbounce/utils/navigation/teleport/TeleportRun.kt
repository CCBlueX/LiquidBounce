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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

enum class RunState {
    ACTIVE,
    DONE,

    /** The server set the player back. */
    REJECTED,

    /** A solid block appeared ahead. */
    INVALID,
    CANCELLED,
}

enum class RunFinish {
    /** The client moves to the destination as well. */
    LAND,

    /** The server stays at the destination until [TeleportDispatcher.undo] brings it back to the client. */
    HOLD,

    /** The route ends where the client is. */
    RESYNC,
}

/** The steps of a route on their way to the server, one batch per tick. */
class TeleportRun internal constructor(
    val owner: EventListener,
    val route: TeleportRoute,
    val steps: List<TeleportStep>,
    private val batches: IntArray,
    val finish: RunFinish,
) : RouteProgress {

    var state = RunState.ACTIVE
        private set

    internal var isBlocked = false
        private set

    private var sent = 0
    private var batch = 0
    private var previousPosition = route.start

    private val position: Vec3
        get() = if (sent == 0) route.start else steps[sent - 1].position

    val ticks: Int
        get() = batches.size

    override val isMoving: Boolean
        get() = state == RunState.ACTIVE

    override val ahead: List<Vec3>
        get() = steps.subList(sent, steps.size).map(TeleportStep::position)

    internal val isSent: Boolean
        get() = batch >= batches.size

    override fun head(partialTicks: Float): Vec3 = previousPosition.lerp(position, partialTicks.toDouble())

    internal fun nextBatch(): List<TeleportStep> {
        previousPosition = position
        val from = sent
        sent += batches[batch++]
        return steps.subList(from, sent)
    }

    internal fun settle() {
        previousPosition = position
    }

    internal fun end(state: RunState) {
        this.state = state
    }

    internal fun onBlockChange(pos: BlockPos, state: BlockState) {
        if (isMoving && !state.getCollisionShape(world, pos).isEmpty && isAhead(AABB(pos))) {
            isBlocked = true
        }
    }

    private fun isAhead(block: AABB): Boolean {
        var previous = position
        for (index in sent until steps.size) {
            val next = steps[index].position
            if (sweptBox(previous, next).intersects(block)) {
                return true
            }
            previous = next
        }
        return false
    }

    private fun sweptBox(from: Vec3, to: Vec3): AABB {
        val half = NavigationTerrain.PLAYER_WIDTH / 2.0
        val height = NavigationTerrain.PLAYER_HEIGHT
        return AABB(
            minOf(from.x, to.x) - half, minOf(from.y, to.y), minOf(from.z, to.z) - half,
            maxOf(from.x, to.x) + half, maxOf(from.y, to.y) + height, maxOf(from.z, to.z) + half,
        )
    }

}
