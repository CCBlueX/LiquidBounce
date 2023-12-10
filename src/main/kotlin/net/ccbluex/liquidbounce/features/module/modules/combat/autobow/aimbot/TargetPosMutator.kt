package net.ccbluex.liquidbounce.features.module.modules.combat.autobow.aimbot

import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.math.Vec3d

class TargetPosMutator(
    val entityPositionOnHit: Vec3d,
    val eyePos: Vec3d,
    val minPull: Float,
    val targetDimensions: EntityDimensions,
) {

    companion object {
        val OFFSETS_TO_CHECK = run {
            val offsets = mutableListOf<Vec3d>()

            for (x in 0.0..1.0 step 0.5) {
                for (y in 0.0..1.0 step 0.5) {
                    for (z in 0.0..1.0 step 0.5) {
                        offsets.add(Vec3d(x, y, z))
                    }
                }
            }

            offsets.sortBy { it.squaredDistanceTo(0.5, 0.5, 0.5) }

            offsets
        }
    }

    /**
     * Returns a position (relative to entity bounding box) which is accessible for an arrow.
     */
    fun tryTargetEntity(): Rotation? {
        val offsetVec = Vec3d(
                targetDimensions.width.toDouble() + 0.4,
                targetDimensions.height.toDouble() + 0.25,
                targetDimensions.width.toDouble() + 0.4
            )

        val hotspots = mutableListOf<Vec3d>()

        for (offset in OFFSETS_TO_CHECK) {
            val currOffset = offsetVec.multiply(offset.add(-0.5, 0.0, -0.5))
            val hitPos = entityPositionOnHit + currOffset

            val deltaPos = hitPos.subtract(eyePos)

            val finalPrediction = AimPlanner.predictBow(deltaPos, minPull)
            val rotation = finalPrediction.rotation

            if (rotation.yaw.isNaN() || rotation.pitch.isNaN()) {
                continue
            }

            val pullProgress = finalPrediction.velocity

            val trajectoryHeuristic = ArrowTrajectoryObstacleHeuristic(
                eyePos = eyePos,
                targetPos = hitPos,
                deltaPos = deltaPos,
                rotation = rotation,
                pullProgress = pullProgress,
            )

            val blockHitPos = if (hotspots.isEmpty()) {
                trajectoryHeuristic.findTrajectoryBlockCollision()
            } else {
                trajectoryHeuristic.findTrajectoryBlockCollisionWithHotspots(hotspots)
            }

            if (blockHitPos == null) {
                return rotation
            }

            hotspots.add(blockHitPos)
        }

        return null
    }

}
