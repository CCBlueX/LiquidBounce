package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.pow

object TrapPlayerSimulation {
    private val predictedPlayerStatesCache = HashMap<PlayerEntity, ArrayDeque<PredictedPlayerPos>>()

    private const val SIMULATION_DISTANCE: Double = 10.0


    fun runSimulations(enemies: List<LivingEntity>) {
        val seenPlayers = HashSet<PlayerEntity>()

        for (enemy in enemies) {
            if (enemy !is PlayerEntity || enemy.squaredDistanceTo(player) > SIMULATION_DISTANCE.pow(2)) {
                continue
            }

            val simulation = PlayerSimulationCache.getSimulationForOtherPlayers(enemy)

            val predictedState = simulation.simulateBetween(0..25)

            var wasAirborne = !enemy.isOnGround

            var ticks = 1

            val predictedPos = predictedState.firstNotNullOfOrNull {
                if (wasAirborne && it.onGround) {
                    return@firstNotNullOfOrNull PredictedPlayerPos(it.pos, ticks, enemy.pos, false)
                }

                wasAirborne = !enemy.isOnGround
                ticks++

                null
            } ?: PredictedPlayerPos(
                null,
                null,
                enemy.pos,
                enemy.velocity.lengthSquared() < 0.05
            )

            seenPlayers.add(enemy)

            val simulationCache = this.predictedPlayerStatesCache.computeIfAbsent(enemy) { ArrayDeque() }

            simulationCache.addLast(predictedPos)

            while (simulationCache.size > 10) {
                simulationCache.removeFirst()
            }
        }

        this.predictedPlayerStatesCache.entries.removeIf { it.key !in seenPlayers }
    }

    /**
     * Searches for a position where a trap could be layed. Currently that is just the landing position of
     * a jumping/falling player.
     *
     * @return position for the trap. `null` if the trap should not be placed.
     */
    fun findPosForTrap(target: LivingEntity, isTargetLocked: Boolean): Vec3d? {
        if (target !is PlayerEntity) {
            return target.pos
        }

        val simulationCache = this.predictedPlayerStatesCache[target] ?: return null

        val positions = simulationCache.mapNotNull { it.nextOnGround }

        // Don't stop targeting if we already started
        val canLayTrapInTime = simulationCache.last().ticksToGround ?: 0 >= 8
        val sufficientEvidence = positions.size >= 5

        if (!sufficientEvidence || !canLayTrapInTime && !isTargetLocked) {
            return null
        }

        val avg = positions
            .fold(Vec3d.ZERO) { acc, vec -> acc.add(vec) }
            .multiply(1.0 / positions.size)
        val std = positions
            .fold(0.0) { acc, vec -> acc + vec.subtract(avg).lengthSquared() }
            .let { Math.sqrt(it / positions.size) }

        ModuleDebug.debugGeometry(
            ModuleAutoTrap,
            "PredictedPlayerPos",
            ModuleDebug.DebuggedBox(target.dimensions.getBoxAt(positions.last()), Color4b.RED.alpha(127))
        )
        ModuleDebug.debugGeometry(
            ModuleAutoTrap,
            "PredictedPlayerPosStd",
            ModuleDebug.DebuggedBox(
                EntityDimensions.fixed((target.dimensions.width * std).toFloat(), 0.5F).getBoxAt(avg),
                Color4b.BLUE.alpha(127)
            )
        )

        // The simulation is mostly certain that the player will land at that position
        if (std < 1.5) {
            return positions.last()
        }

        return null
    }

    private class PredictedPlayerPos(
        val nextOnGround: Vec3d?,
        val ticksToGround: Int?,
        val currPos: Vec3d,
        val isStationary: Boolean
    )
}
