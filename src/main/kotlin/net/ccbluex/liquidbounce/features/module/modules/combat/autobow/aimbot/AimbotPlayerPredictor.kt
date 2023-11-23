package net.ccbluex.liquidbounce.features.module.modules.combat.autobow.aimbot

import net.ccbluex.liquidbounce.features.module.modules.combat.autobow.ModuleAutoBowAimbot
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.centerOffset
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.truncate

private const val MAX_PREDICTION_STEPS: Int = 5

interface EntityPredictor {
    val entity: Entity

    /**
     * Predict where the entity will be in [deltaTicks] ticks.
     *
     * @return the delta to the current entity position
     */
    fun predictDeltaPos(deltaTicks: Float): Vec3d

    companion object {
        fun createOptimalForEntity(entity: Entity): EntityPredictor {
            return when (entity) {
                is PlayerEntity -> PlayerPredictionProducer(entity)
                else -> EntityPredictionProducer(entity)
            }
        }
    }
}

class EntityPredictionProducer(override val entity: Entity) : EntityPredictor {
    override fun predictDeltaPos(deltaTicks: Float): Vec3d {
        val velocity = entity.pos - entity.prevPos

        return Vec3d(
            velocity.x * deltaTicks,
            velocity.y * deltaTicks,
            velocity.z * deltaTicks,
        )
    }

}

class PlayerPredictionProducer(override val entity: PlayerEntity) : EntityPredictor {
    override fun predictDeltaPos(deltaTicks: Float): Vec3d {
        val simulatedPlayer = SimulatedPlayer.fromOtherPlayer(
            entity,
            SimulatedPlayer.SimulatedPlayerInput.guessInput(this.entity)
        )

        for (ignored in 0 until floor(deltaTicks).toInt()) {
            simulatedPlayer.tick()
        }

        val tickPosBefore = simulatedPlayer.pos

        simulatedPlayer.tick()

        val tickPosAfter = simulatedPlayer.pos

        val interpol = (tickPosAfter - tickPosBefore) * (deltaTicks.toDouble() - truncate(deltaTicks))

        return tickPosBefore + interpol - this.entity.pos
    }

}

/**
 *
 */
fun predictEntityPositionOnArrowHit(entityPredictor: EntityPredictor, eyePos: Vec3d, minPull: Float): Vec3d {
    val target = entityPredictor.entity

    var currentEntityPos = Vec3d(target.x, target.y, target.z)
    val boxCenter = target.dimensions.centerOffset

    for (ignored in 0..MAX_PREDICTION_STEPS) {
        val basePrediction = AimPlanner.predictBow(currentEntityPos.subtract(eyePos).add(boxCenter), minPull)

        // Since the target will have moved between the time the arrow starts and hits the target, we need to
        // account for that
        val realTravelTime =
            ModuleAutoBowAimbot.getTravelTime(
                basePrediction.travelledOnX,
                cos(basePrediction.rotation.pitch.toRadians()) * basePrediction.velocity * 3.0 * 0.7,
            )

        if (realTravelTime.isNaN()) {
            break
        }

        val delta = entityPredictor.predictDeltaPos(realTravelTime + 2.25F)

        if (delta.lengthSquared() < 0.1 * 0.1) {
            break
        }

        currentEntityPos = target.pos.add(delta)
    }

    return currentEntityPos
}
