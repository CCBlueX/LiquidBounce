package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object PlayerSimulationCache: Listenable {
    private val otherPlayerCache = ConcurrentHashMap<PlayerEntity, SimulatedPlayerCache>()
    private var localPlayerCache: SimulatedPlayerCache? = null

    private val gameTickHandler = handler<GameTickEvent> {
        this.otherPlayerCache.clear()
    }

    private val movementHandler = handler<MovementInputEvent> {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(it.directionalInput)
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

class SimulatedPlayerCache(private val simulatedPlayer: SimulatedPlayer) {
    private var currentSimulationStep = 0
    private val simulationSteps = ArrayList<SimulatedPlayerSnapshot>().apply {
        add(SimulatedPlayerSnapshot(simulatedPlayer))
    }
    private val lock = ReentrantReadWriteLock()

    fun simulateUntil(ticks: Int) {
        check(ticks >= 0) { "ticks must be greater than 0" }

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
        check(tickRange.endInclusive < 60 * 20) { "tried to simulate a player for more than a minute!" }

        simulateUntil(tickRange.endInclusive + 1)

        return lock.read {
            ArrayList(simulationSteps.subList(tickRange.start, tickRange.endInclusive + 1))
        }
    }

    fun simulateBetween(tickRange: IntRange): Sequence<SimulatedPlayerSnapshot> {
        check(tickRange.endInclusive < 60 * 20) { "tried to simulate a player for more than a minute!" }

        simulateUntil(tickRange.endInclusive + 1)

        return sequence<SimulatedPlayerSnapshot> {
            for (i in tickRange) {
                yield(getSnapshotAt(i))
            }
        }
    }

}

class SimulatedPlayerSnapshot(s: SimulatedPlayer) {
    val pos = s.pos
    val fallDistance = s.fallDistance
    val velocity = s.velocity
    val onGround = s.onGround
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
