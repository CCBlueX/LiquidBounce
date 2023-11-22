package net.ccbluex.liquidbounce.features.module.modules.combat.autobow.aimbot

import net.ccbluex.liquidbounce.features.module.modules.combat.autobow.ModuleAutoBow
import net.ccbluex.liquidbounce.features.module.modules.combat.autobow.ModuleAutoBowAimbot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.client.QuickAccess
import net.ccbluex.liquidbounce.utils.entity.centerOffset
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt

object AimPlanner {

    fun planShot(target: Entity, eyePos: Vec3d, minPull: Float): Rotation? {
        val actualPositionOnHit = predictEntityPositionOnArrowHit(
            entityPredictor = EntityPredictor.createOptimalForEntity(target),
            eyePos = eyePos,
            minPull = minPull
        )

        ModuleDebug.debugGeometry(
            ModuleAutoBow,
            "PredictedTargetPos",
            ModuleDebug.DebuggedBox(target.dimensions.getBoxAt(actualPositionOnHit), Color4b(0, 0, 255, 127))
        )

        val deltaPos = actualPositionOnHit.subtract(eyePos).add(target.dimensions.centerOffset)

        val finalPrediction = predictBow(deltaPos, minPull)
        val rotation = finalPrediction.rotation

        if (rotation.yaw.isNaN() || rotation.pitch.isNaN()) {
            return null
        }

        val pullProgress = finalPrediction.pullProgress

        // If the planned trajectory would end in a block, don't shoot
        if (doesTrajectoryHitBlock(deltaPos, rotation, pullProgress, eyePos, actualPositionOnHit)) {
            return null
        }

        return rotation
    }

    private fun doesTrajectoryHitBlock(
        deltaPos: Vec3d,
        rotation: Rotation,
        pullProgress: Float,
        eyePos: Vec3d,
        targetPos: Vec3d
    ): Boolean {
        val vertex = ModuleAutoBowAimbot.getHighestPointOfTrajectory(deltaPos, rotation, pullProgress)

        val positions = mutableListOf<Vec3d>()

        positions.add(eyePos)

        if (vertex != null) positions.add(vertex.add(eyePos))

        positions.add(targetPos)

        for (i in 0 until positions.lastIndex) {
            val raycast =
                QuickAccess.world.raycast(
                    RaycastContext(
                        positions[i],
                        positions[i + 1],
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.ANY,
                        QuickAccess.player,
                    ),
                )

            if (raycast.type == HitResult.Type.BLOCK) {
                return true
            }
        }
        return false
    }

    @Suppress("MaxLineLength")
    fun predictBow(
        target: Vec3d,
        minPull: Float,
    ): ModuleAutoBowAimbot.BowPredictionResult {
        val player = QuickAccess.player

        val travelledOnX = sqrt(target.x * target.x + target.z * target.z)

        val velocity: Float = getHypotheticalArrowVelocity(player, minPull)

        return ModuleAutoBowAimbot.BowPredictionResult(
            Rotation(
                (atan2(target.z, target.x) * 180.0f / Math.PI).toFloat() - 90.0f,
                (
                    -Math.toDegrees(
                        atan(
                            (velocity * velocity - sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (travelledOnX * travelledOnX) + 2 * target.y * (velocity * velocity)))) / (0.006f * travelledOnX),
                        ),
                    )
                    ).toFloat(),
            ),
            velocity,
            travelledOnX,
        )
    }

    /**
     * What initial velocity do we currently expect from our arrow with the current pull?
     *
     * @param minPull the minimum pull we should expect to shoot with (0.0-1.0F). If the current pull is lower than
     * this, it will use this instead (useful for minExpectedPull and assumeElongated option)
     */
    fun getHypotheticalArrowVelocity(
        player: ClientPlayerEntity,
        minPull: Float = 0.0F
    ): Float {
        var velocity: Float = (player.itemUseTime / 20.0F).coerceAtLeast(minPull)

        velocity = (velocity * velocity + velocity * 2.0f) / 3.0f

        return velocity.coerceAtMost(1.0F)
    }

}
