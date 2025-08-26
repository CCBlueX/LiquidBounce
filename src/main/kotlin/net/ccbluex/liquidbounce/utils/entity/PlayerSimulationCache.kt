package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object PlayerSimulationCache: EventListener {
    private val otherPlayerCache = ConcurrentHashMap<PlayerEntity, SimulatedPlayerCache>()
    private var localPlayerCache: SimulatedPlayerCache? = null

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        this.otherPlayerCache.clear()
    }

    @Suppress("unused")
    private val criticalMovementHandler = handler<MovementInputEvent>(
        priority = CRITICAL_MODIFICATION
    ) { event ->
        this.localPlayerCache = null
        updatePlayerCache(event.directionalInput)
    }

    @Suppress("unused")
    private val movementHandler = handler<MovementInputEvent> { event ->
        updatePlayerCache(event.directionalInput, verify = true)
    }

    @Suppress("unused")
    private val modalMovementHandler = handler<MovementInputEvent>(
        priority = MODEL_STATE
    ) { event ->
        updatePlayerCache(event.directionalInput, verify = true)
    }

    /**
     * Updates the cache for the local player,
     * this will be called on every movement input event
     * to ensure the cache is up to date.
     *
     * @param directionalInput the input to update the cache with
     */
    private fun updatePlayerCache(directionalInput: DirectionalInput, verify: Boolean = false) {
        // Check if we even need to update the cache
        if (verify && localPlayerCache?.simulatedPlayer?.input?.directionalInput == directionalInput) {
            return
        }

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
        )

        localPlayerCache = SimulatedPlayerCache(simulatedPlayer)
    }

    fun getSimulationForOtherPlayers(player: PlayerEntity): SimulatedPlayerCache {
        return otherPlayerCache.computeIfAbsent(player) {
            val simulatedPlayer = SimulatedPlayer.fromOtherPlayer(
                player,
                SimulatedPlayer.SimulatedPlayerInput.guessInput(player)
            )

            SimulatedPlayerCache(simulatedPlayer)
        }
    }

    fun getSimulationForLocalPlayer(): SimulatedPlayerCache {
        val cached = localPlayerCache

        if (cached != null) {
            return cached
        }

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input))
        )

        val simulatedPlayerCache = SimulatedPlayerCache(simulatedPlayer)

        localPlayerCache = simulatedPlayerCache

        return simulatedPlayerCache
    }
}

class SimulatedPlayerCache(internal val simulatedPlayer: SimulatedPlayer) {
    private var currentSimulationStep = 0
    private val simulationSteps = ArrayList<SimulatedPlayerSnapshot>().apply {
        add(SimulatedPlayerSnapshot(simulatedPlayer))
    }
    private val lock = ReentrantReadWriteLock()

    fun simulateUntil(ticks: Int) {
        check(ticks >= 0) { "ticks may not be negative" }

        if (currentSimulationStep >= ticks) {
            return
        }

        lock.write {
            while (currentSimulationStep < ticks) {
                simulatedPlayer.tick()
                simulationSteps.add(SimulatedPlayerSnapshot(simulatedPlayer))

                this.currentSimulationStep++
            }
        }
    }

    fun getSnapshotAt(ticks: Int): SimulatedPlayerSnapshot {
        simulateUntil(ticks)

        lock.read {
            return simulationSteps[ticks]
        }
    }

    fun simulate() = sequence<SimulatedPlayerSnapshot> {
        var idx = 0

        while (true) {
            yield(getSnapshotAt(idx))

            idx++
        }
    }

    fun getSnapshotsBetween(tickRange: IntRange): List<SimulatedPlayerSnapshot> {
        check(tickRange.last < 60 * 20) { "tried to simulate a player for more than a minute!" }

        simulateUntil(tickRange.last + 1)

        return lock.read {
            ArrayList(simulationSteps.subList(tickRange.first, tickRange.last + 1))
        }
    }

    fun simulateBetween(tickRange: IntRange): Sequence<SimulatedPlayerSnapshot> {
        check(tickRange.last < 60 * 20) { "tried to simulate a player for more than a minute!" }

        simulateUntil(tickRange.last + 1)

        return sequence<SimulatedPlayerSnapshot> {
            for (i in tickRange) {
                yield(getSnapshotAt(i))
            }
        }
    }

}

data class SimulatedPlayerSnapshot(
    val pos: Vec3d,
    val fallDistance: Float,
    val velocity: Vec3d,
    val onGround: Boolean,
    val clipLedged: Boolean
) {
    constructor(s: SimulatedPlayer): this(
        s.pos,
        s.fallDistance,
        s.velocity,
        s.onGround,
        s.clipLedged
    )
}

/**
 * Yes, this name sucks as [SimulatedPlayerCache] already exists, but I don't know a better name :/
 */
class CachedPlayerSimulation(val simulatedPlayer: SimulatedPlayerCache): PlayerSimulation {
    override val pos: Vec3d
        get() = this.simulatedPlayer.getSnapshotAt(this.ticks).pos

    private var ticks = 0

    override fun tick() {
        this.ticks++
    }
}
