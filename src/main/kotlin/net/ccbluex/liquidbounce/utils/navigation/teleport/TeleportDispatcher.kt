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
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3

/**
 * Sends teleport routes to the server, one batch of position packets per tick, and keeps the vanilla movement packets
 * in line while the server has the player somewhere else. One owner at a time; one that stops running is undone.
 */
object TeleportDispatcher : EventListener {

    /** The latest run, also after it ended. */
    var run: TeleportRun? = null
        private set

    @Volatile
    private var holder: EventListener? = null

    /** The last step the server got, while it has the player somewhere else than the client. */
    @Volatile
    private var lastSent: TeleportStep? = null

    @Volatile
    private var setback = false

    /** Where the current excursion began, followed by the route points it passed. */
    private val trail = ArrayList<Vec3>()
    private var stepLength = TeleportStepSettings.DEFAULT_STEP_LENGTH
    private var packetsPerTick = TeleportStepSettings.DEFAULT_PACKETS_PER_TICK

    val serverPosition: Vec3?
        get() = lastSent?.position

    val isMoving: Boolean
        get() = run?.isMoving == true

    fun holds(owner: EventListener) = holder === owner

    /** Where the server has the player for [owner] between the last tick and this one, null if with the client. */
    fun serverPosition(owner: EventListener, partialTicks: Float): Vec3? =
        if (holds(owner)) run?.takeIf { it.isMoving }?.head(partialTicks) ?: serverPosition else null

    /** Whether nothing moves and the server has the player with the client or where [owner] left it. */
    fun isAvailable(owner: EventListener) = !isMoving && (holder == null || holder === owner)

    /** Starts sending [plan] from where the server has the player; null when busy or the plan begins too far off. */
    fun dispatch(
        owner: EventListener,
        plan: TeleportPlan,
        finish: RunFinish,
        settings: TeleportStepSettings,
    ): TeleportRun? {
        if (!isAvailable(owner) || player.isPassenger) {
            return null
        }

        val held = serverPosition
        val current = held ?: player.position()
        val drift = plan.route.start.distanceToSqr(current)
        val linked = when {
            drift > MAX_DRIFT_SQ -> return null
            drift <= TeleportRoute.SAME_POSITION_SQ -> plan
            else -> plan.startingAt(current, NavigationTerrain.live(), settings.stepLength) ?: return null
        }

        if (held == null) {
            trail.clear()
            trail += current
        }

        stepLength = settings.stepLength
        packetsPerTick = settings.packetsPerTick
        return start(owner, linked.route, linked.steps, finish)
    }

    /** Brings the server back to where the excursion of [owner] began, or lands the client if the way is blocked. */
    fun undo(owner: EventListener) {
        val current = run
        if (holder !== owner || current?.isMoving == true && current.finish == RunFinish.RESYNC) {
            return
        }

        current?.takeIf { it.isMoving }?.end(RunState.CANCELLED)

        val position = serverPosition ?: return release()
        val back = wayBack(position, trail) ?: return release()
        val steps = TeleportSteps.plan(NavigationTerrain.live(), back, stepLength) ?: return land(position)
        start(owner, back, steps, RunFinish.RESYNC)
    }

    private fun start(owner: EventListener, route: TeleportRoute, steps: List<TeleportStep>, finish: RunFinish) =
        TeleportRun(owner, route, steps, TeleportSteps.batches(route.start, steps, packetsPerTick), finish).also {
            run = it
            holder = owner
            setback = false
        }

    private fun send(run: TeleportRun) {
        if (!run.isSent) {
            for (step in run.nextBatch()) {
                if (setback) {
                    return
                }

                sendPacketSilently(ServerboundMovePlayerPacket.Pos(step.position, step.onGround, false))
                lastSent = step

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
        lastSent = null
        holder = null
        trail.clear()
    }

    private fun reset(state: RunState) {
        run?.takeIf { it.isMoving || holder != null }?.end(state)
        release()
        setback = false
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        val run = run ?: return@handler
        if (mc.player == null) {
            return@handler
        }

        val holder = holder
        when {
            setback -> reset(RunState.REJECTED)
            holder != null && !holder.running && run.finish != RunFinish.RESYNC -> undo(holder)
            !run.isMoving -> run.settle()
            run.isBlocked -> {
                run.end(RunState.INVALID)
                undo(run.owner)
            }
            else -> send(run)
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
        val packet = it.packet

        if (it.origin == TransferOrigin.INCOMING) {
            if (packet is ClientboundPlayerPositionPacket && holder != null) {
                setback = true
                lastSent = null
            }
        } else if (packet is ServerboundMovePlayerPacket && !setback) {
            val server = lastSent ?: return@handler

            if (packet.hasPosition()) {
                packet.x = server.position.x
                packet.y = server.position.y
                packet.z = server.position.z
            }
            packet.onGround = server.onGround
        }
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        run?.onBlockChange(event.blockPos, event.newState)
    }

    @Suppress("unused")
    private val chunkDeltaHandler = handler<ChunkDeltaUpdateEvent> { event ->
        event.packet.runUpdates { pos, state -> run?.onBlockChange(pos, state) }
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

/** The way from [position] back along [trail], without the points the server is already at. */
private fun wayBack(position: Vec3, trail: List<Vec3>): TeleportRoute? {
    val points = arrayListOf(position)
    for (point in trail.asReversed()) {
        if (point.distanceToSqr(points.last()) > TeleportRoute.SAME_POSITION_SQ) {
            points += point
        }
    }
    return points.takeIf { it.size >= 2 }?.let(::TeleportRoute)
}
