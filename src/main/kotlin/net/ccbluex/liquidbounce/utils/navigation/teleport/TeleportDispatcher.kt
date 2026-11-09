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
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.navigation.render.RouteProgress
import net.ccbluex.liquidbounce.utils.navigation.terrain.LiveWorld
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

enum class RunState {
    ACTIVE,
    DONE,
    REJECTED,
    INVALID,
    CANCELLED,
}

enum class RunFinish {
    /**
     * The client moves to the destination as well.
     */
    LAND,

    /**
     * The server keeps the player at the destination while the client stays; [TeleportDispatcher.undo] brings
     * the server back.
     */
    HOLD,

    /**
     * The route ends where the client is, so both agree again.
     */
    RESYNC,
}

class TeleportRun internal constructor(
    val owner: EventListener,
    val route: TeleportRoute,
    val steps: List<TeleportStep>,
    private val batches: IntArray,
    val finish: RunFinish,
) : RouteProgress {

    var state = RunState.ACTIVE
        private set

    var stateChangedAt = System.currentTimeMillis()
        private set

    var sent = 0
        private set

    private var batch = 0

    /**
     * Server position before the latest batch, so renderers can interpolate.
     */
    var previousPosition: Vec3 = route.start
        private set

    val position: Vec3
        get() = if (sent == 0) route.start else steps[sent - 1].position

    val segment: Int
        get() = if (sent == 0) 0 else steps[sent - 1].segment

    val ticks: Int
        get() = batches.size

    override val isMoving: Boolean
        get() = state == RunState.ACTIVE

    override fun head(partialTicks: Float): Vec3 = previousPosition.lerp(position, partialTicks.toDouble())

    override val ahead: List<Vec3>
        get() = steps.subList(sent, steps.size).map(TeleportStep::position)

    internal val isSent: Boolean
        get() = batch >= batches.size

    internal fun nextBatch(): List<TeleportStep> {
        previousPosition = position
        val count = batches[batch++]
        val slice = steps.subList(sent, sent + count)
        sent += count
        return slice
    }

    internal fun settle() {
        previousPosition = position
    }

    internal fun end(state: RunState) {
        this.state = state
        stateChangedAt = System.currentTimeMillis()
    }

    /**
     * Whether a solid block at [pos] would be in the way of the steps not sent yet.
     */
    internal fun isAhead(pos: BlockPos): Boolean {
        val block = AABB(pos)
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

/**
 * Sends teleport routes to the server, one batch of position packets per tick, and keeps the vanilla
 * movement packets in line while the server has the player somewhere else than the client.
 *
 * A setback from the server ends a run as [RunState.REJECTED]. A solid block appearing ahead ends it as
 * [RunState.INVALID] and sends the server back to where the run began. Only one owner moves the player at
 * a time, and an owner that stops running is brought back automatically.
 */
@Suppress("TooManyFunctions")
object TeleportDispatcher : EventListener {

    /**
     * Where the server has the player while it differs from the client.
     */
    @Volatile
    var serverPosition: Vec3? = null
        private set

    private var serverOnGround = false

    /**
     * The latest run, also after it ended.
     */
    var run: TeleportRun? = null
        private set

    @Volatile
    private var holder: EventListener? = null

    @Volatile
    private var setback = false
    private var blockedAhead = false

    /**
     * Where the current excursion began, followed by the route points it passed.
     */
    private val trail = ArrayList<Vec3>()
    private var stepLength = TeleportStepSettings.DEFAULT_STEP_LENGTH
    private var packetsPerTick = TeleportStepSettings.DEFAULT_PACKETS_PER_TICK

    val isMoving: Boolean
        get() = run?.state == RunState.ACTIVE

    fun holds(owner: EventListener) = holder === owner

    /**
     * Where the server has the player for [owner], between the last tick and this one, or null when the server
     * has it where the client is.
     */
    fun serverPosition(owner: EventListener, partialTicks: Float): Vec3? {
        if (!holds(owner)) {
            return null
        }

        val run = run
        return if (run != null && run.owner === owner && run.isMoving) run.head(partialTicks) else serverPosition
    }

    /**
     * Whether [owner] may dispatch now: nothing moves, and the server has the player either where the client is
     * or where an earlier run of [owner] left it.
     */
    fun isAvailable(owner: EventListener) = !isMoving && (holder == null || holder === owner)

    /**
     * Starts sending [steps] of [route]. The route has to begin near where the server has the player; the bit
     * the player walked since the route was planned is prepended.
     */
    fun dispatch(
        owner: EventListener,
        route: TeleportRoute,
        steps: List<TeleportStep>,
        finish: RunFinish,
        settings: TeleportStepSettings,
    ): TeleportRun? {
        if (!isAvailable(owner) || player.isPassenger) {
            return null
        }

        val held = serverPosition
        val current = held ?: player.position()
        val drift = route.start.distanceToSqr(current)
        if (drift > MAX_DRIFT_SQ) {
            return null
        }

        var linkedRoute = route
        var linkedSteps = steps
        if (drift > TeleportRoute.SAME_POSITION_SQ) {
            val link = ArrayList<TeleportStep>(steps.size + 2)
            val terrain = NavigationTerrain(LiveWorld(world))
            if (!TeleportSteps.appendSegment(terrain, current, route.start, settings.stepLength, 0, link)) {
                return null
            }
            steps.mapTo(link) { TeleportStep(it.position, it.onGround, it.segment + 1) }
            linkedRoute = TeleportRoute(listOf(current) + route.points)
            linkedSteps = link
        }

        if (held == null) {
            trail.clear()
            trail += current
        }

        stepLength = settings.stepLength
        packetsPerTick = settings.packetsPerTick
        return start(owner, linkedRoute, linkedSteps, finish)
    }

    /**
     * Brings the server back to where the excursion of [owner] began. If that way is blocked now, the client
     * lands where the server has the player instead.
     */
    fun undo(owner: EventListener) {
        val current = run
        if (holder !== owner || current?.state == RunState.ACTIVE && current.finish == RunFinish.RESYNC) {
            return
        }

        current?.takeIf { it.state == RunState.ACTIVE }?.end(RunState.CANCELLED)

        val position = serverPosition ?: return release()
        val points = ArrayList<Vec3>(trail.size + 1)
        points += position
        for (index in trail.indices.reversed()) {
            if (trail[index].distanceToSqr(points.last()) > TeleportRoute.SAME_POSITION_SQ) {
                points += trail[index]
            }
        }

        if (points.size < 2) {
            return release()
        }

        val back = TeleportRoute(points)
        val steps = TeleportSteps.plan(NavigationTerrain(LiveWorld(world)), back, stepLength)
        if (steps == null) {
            land(position)
            return
        }

        start(owner, back, steps, RunFinish.RESYNC)
    }

    private fun start(owner: EventListener, route: TeleportRoute, steps: List<TeleportStep>, finish: RunFinish) =
        TeleportRun(owner, route, steps, TeleportSteps.batches(route.start, steps, packetsPerTick), finish).also {
            run = it
            holder = owner
            setback = false
            blockedAhead = false
        }

    private fun complete(run: TeleportRun) {
        run.end(RunState.DONE)
        when (run.finish) {
            RunFinish.LAND -> land(run.route.destination)
            RunFinish.HOLD -> {}
            RunFinish.RESYNC -> release()
        }
    }

    private fun land(position: Vec3) {
        teleportPlayer(position.x, position.y, position.z, resetMotion = true)
        player.resetFallDistance()
        release()
    }

    private fun release() {
        serverPosition = null
        holder = null
        trail.clear()
    }

    private fun reset(state: RunState) {
        run?.let {
            if (it.state == RunState.ACTIVE || holder != null) {
                it.end(state)
            }
        }
        release()
        setback = false
        blockedAhead = false
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        val run = run ?: return@handler
        if (mc.player == null) {
            return@handler
        }

        if (setback) {
            reset(RunState.REJECTED)
            return@handler
        }

        val holder = holder
        if (holder != null && !holder.running && run.finish != RunFinish.RESYNC) {
            undo(holder)
            return@handler
        }

        if (run.state != RunState.ACTIVE) {
            run.settle()
            return@handler
        }

        if (blockedAhead) {
            run.end(RunState.INVALID)
            undo(run.owner)
            return@handler
        }

        send(run)
    }

    private fun send(run: TeleportRun) {
        if (!run.isSent) {
            for (step in run.nextBatch()) {
                if (setback) {
                    return
                }

                sendPacketSilently(ServerboundMovePlayerPacket.Pos(step.position, step.onGround, false))
                serverPosition = step.position
                serverOnGround = step.onGround

                val reached = run.route.points[step.segment + 1]
                if (run.finish != RunFinish.RESYNC &&
                    step.position.distanceToSqr(reached) < TeleportRoute.SAME_POSITION_SQ) {
                    trail += reached
                }
            }
        }

        if (run.isSent) {
            complete(run)
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
        val packet = it.packet

        if (it.origin == TransferOrigin.INCOMING) {
            if (packet is ClientboundPlayerPositionPacket && holder != null) {
                setback = true
                serverPosition = null
            }
            return@handler
        }

        if (packet is ServerboundMovePlayerPacket && !setback) {
            val position = serverPosition ?: return@handler

            if (packet is ServerboundMovePlayerPacket.Pos || packet is ServerboundMovePlayerPacket.PosRot) {
                packet.x = position.x
                packet.y = position.y
                packet.z = position.z
            }
            packet.onGround = serverOnGround
        }
    }

    private fun checkBlock(pos: BlockPos, state: BlockState) {
        val run = run?.takeIf { it.state == RunState.ACTIVE } ?: return
        if (!state.getCollisionShape(world, pos).isEmpty && run.isAhead(pos)) {
            blockedAhead = true
        }
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        checkBlock(event.blockPos, event.newState)
    }

    @Suppress("unused")
    private val chunkDeltaHandler = handler<ChunkDeltaUpdateEvent> { event ->
        event.packet.runUpdates(::checkBlock)
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset(RunState.CANCELLED)
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        reset(RunState.CANCELLED)
    }

    private const val MAX_DRIFT_SQ = 2.0 * 2.0

}
