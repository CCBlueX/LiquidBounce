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
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTeleport
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3

/**
 * Sends teleport routes to the server, one batch of position packets per tick, and keeps the vanilla movement packets
 * in line while the server has the player somewhere else. One owner at a time; one that stops running is undone.
 */
object TeleportManager : EventListener {

    /** The latest run, also after it ended. */
    var run: Teleport? = null
        private set

    @Volatile
    private var holder: EventListener? = null

    /** The last step the server got, while it has the player somewhere else than the client. */
    @Volatile
    private var lastSent: Route.Step? = null

    @Volatile
    private var setback = false

    /** Where the current excursion began, followed by the route points it passed. */
    private val trail = ArrayList<Vec3>()
    private var stepLength = StepsValueGroup.DEFAULT_STEP_LENGTH
    private var packetsPerTick = StepsValueGroup.DEFAULT_PACKETS_PER_TICK
    private var flightMode = Teleport.FlightMode.PACED
    private var spoofOnGround = false

    val serverPosition: Vec3?
        get() = lastSent?.position

    val isMoving: Boolean
        get() = run?.isMoving == true

    fun holds(owner: EventListener) = holder === owner

    /** Notes a position the server accepted outside of a run, so the way back starts from there too. */
    fun notePosition(position: Vec3, onGround: Boolean) {
        lastSent = Route.Step(position, onGround, 0)
        pinTtl = MAX_PIN_TICKS
    }

    /** Drops a tracked position that no run owns, so the client's own movement is sent again. */
    fun releasePosition() {
        if (holder == null) {
            release()
        }
    }

    /** Where the server has the player for [owner] between the last tick and this one, null if with the client. */
    fun serverPosition(owner: EventListener, partialTicks: Float): Vec3? =
        if (holds(owner)) run?.takeIf { it.isMoving }?.head(partialTicks) ?: serverPosition else null

    /** Whether nothing moves and the server has the player with the client or where [owner] left it. */
    fun isAvailable(owner: EventListener) = !isMoving && (holder == null || holder === owner)

    /** Starts sending [route] from where the server has the player; null when busy or it begins too far off. */
    fun dispatch(
        owner: EventListener,
        route: Route,
        finish: Teleport.Finish,
        settings: StepsValueGroup,
        flight: Teleport.FlightMode = Teleport.FlightMode.PACED,
        maxDrift: Double = MAX_DRIFT,
    ): Teleport? {
        if (!isAvailable(owner) || player.isPassenger) {
            return null
        }

        val held = serverPosition
        val current = held ?: player.position()
        val drift = route.start.distanceToSqr(current)
        val linked = when {
            drift > maxDrift * maxDrift -> return null
            drift <= Route.SAME_POSITION_SQ -> route
            else -> route.startingAt(current, Terrain.live(), settings.stepLength) ?: return null
        }

        if (held == null) {
            trail.clear()
            trail += current
        }

        stepLength = settings.stepLength
        packetsPerTick = settings.packetsPerTick
        spoofOnGround = settings.spoofOnGround
        flightMode = flight
        return start(owner, linked, finish)
    }

    /** Brings the server back to where the excursion of [owner] began, or lands the client if the way is blocked. */
    fun undo(owner: EventListener) {
        val current = run
        if (holder !== owner || current?.isMoving == true && current.finish == Teleport.Finish.RESYNC) {
            return
        }

        current?.takeIf { it.isMoving }?.end(Teleport.State.CANCELLED)

        val position = serverPosition ?: return release()
        val back = wayBack(position, trail) ?: return release()
        val route = Route.plan(Terrain.live(), back, stepLength) ?: return land(position)
        start(owner, route, Teleport.Finish.RESYNC)
    }

    private fun start(owner: EventListener, route: Route, finish: Teleport.Finish): Teleport {
        val (flying, batches) = when (flightMode) {
            Teleport.FlightMode.PACED -> route to route.batches(packetsPerTick)
            // Every step in one tick, like a client that knows no per-tick packet limit.
            Teleport.FlightMode.BURST -> route to intArrayOf(route.steps.size)
            // One packet to the destination: there is no intermediate position to check.
            Teleport.FlightMode.INSTANT -> {
                val last = route.steps.last()
                val jump = Route(route.points, listOf(Route.Step(route.destination, last.onGround, last.segment)))
                jump to intArrayOf(1)
            }
        }
        return Teleport(owner, flying, batches, finish).also {
            run = it
            holder = owner
            setback = false
        }
    }

    private fun send(run: Teleport) {
        if (!run.isSent) {
            for (step in run.nextBatch()) {
                if (setback) {
                    return
                }

                // A drop over the height difference would be banked as a fall and settled when the
                // route lands; reported as standing, each descent settles nothing and banks nothing.
                val previous = lastSent?.position ?: run.route.start
                val descending = step.position.y < previous.y
                val onGround = step.onGround || (spoofOnGround && descending)

                sendPacketSilently(ServerboundMovePlayerPacket.Pos(step.position, onGround, false))
                lastSent = step

                val reached = run.route.points[step.segment + 1]
                if (run.finish != Teleport.Finish.RESYNC &&
                    step.position.distanceToSqr(reached) < Route.SAME_POSITION_SQ) {
                    trail += reached
                }
            }
        }

        if (run.isSent) {
            complete(run)
        }
    }

    private fun complete(run: Teleport) {
        run.end(Teleport.State.DONE)
        when (run.finish) {
            Teleport.Finish.LAND -> land(run.route.destination)
            Teleport.Finish.HOLD -> {}
            Teleport.Finish.RESYNC -> release()
        }
    }

    private fun land(position: Vec3) {
        GlobalSettingsTeleport.teleportPlayer(position.x, position.y, position.z, resetMotion = true)
        player.resetFallDistance()
        release()
    }

    private fun release() {
        lastSent = null
        holder = null
        trail.clear()
    }

    private fun reset(state: Teleport.State) {
        run?.takeIf { it.isMoving || holder != null }?.end(state)
        release()
        setback = false
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        // A position noted without a run has no owner to clear it: a spray that ends outside an excursion
        // would leave the server standing there while the player walks away. Expire it instead.
        if (holder == null && lastSent != null && --pinTtl <= 0) {
            releasePosition()
        }

        var run = run ?: return@handler
        if (mc.player == null) {
            return@handler
        }

        if (run.finish == Teleport.Finish.RESYNC && run.isMoving && redirectsToClient(run)) {
            run = this.run ?: return@handler
        }

        val holder = holder
        when {
            setback -> reset(Teleport.State.REJECTED)
            holder != null && !holder.running && run.finish != Teleport.Finish.RESYNC -> undo(holder)
            !run.isMoving -> run.settle()
            run.isBlocked -> {
                run.end(Teleport.State.INVALID)
                undo(run.owner)
            }
            else -> send(run)
        }
    }

    /** The client walked on while the route brings the server back, so the route ends where it is now. */
    private fun redirectsToClient(run: Teleport): Boolean {
        if (player.position().distanceToSqr(run.route.destination) <= REDIRECT_SQ) {
            return false
        }

        run.end(Teleport.State.CANCELLED)
        undo(run.owner)
        return true
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
        reset(Teleport.State.CANCELLED)
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        reset(Teleport.State.CANCELLED)
    }

    /** How far the player may walk off the route start before the route must be re-linked from here. */
    /** How far the player may have walked off a route before it counts as stale; callers may widen it. */
    const val MAX_DRIFT = 2.0

    /** Ticks a noted position may live without a run owning it. */
    private const val MAX_PIN_TICKS = 20

    /** Ticks left until an unowned tracked position is dropped. */
    private var pinTtl = 0

    /** How far the client may walk on before the return route follows it. */
    private const val REDIRECT_SQ = 1.0 * 1.0

}

/** The way from [position] back along [trail] to where the client is now, without the points the server is already at. */
private fun wayBack(position: Vec3, trail: List<Vec3>): List<Vec3>? {
    val client = player.position()
    val points = arrayListOf(position)

    // Only the trail points the server has not passed yet: walking the whole trail again from a
    // position in the middle would send it back out to the end and make it run the path twice.
    val budget = position.distanceToSqr(client)
    for (point in trail.asReversed()) {
        if (point.distanceToSqr(client) >= budget - Route.SAME_POSITION_SQ) {
            continue
        }
        if (point.distanceToSqr(points.last()) > Route.SAME_POSITION_SQ) {
            points += point
        }
    }

    if (client.distanceToSqr(points.last()) > Route.SAME_POSITION_SQ) {
        points += client
    }
    return points.takeIf { it.size >= 2 }
}
