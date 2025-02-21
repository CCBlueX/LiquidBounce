package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Solves this problem by approximating the trajectory as a second degree polynomial. This approximation is good for
 * ~20 ticks.
 *
 * Currently only used as backup
 */
object PolynomialProjectileAngleCalculator: ProjectileAngleCalculator() {
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)
        val estimatedTicksUntilImpact = basePos.distanceTo(sourcePos) / projectileInfo.initialVelocity

        val diff: Vec3d = targetPosFunction.getPositionInTicks(estimatedTicksUntilImpact).subtract(sourcePos)

        val horizontalDistance = MathHelper.sqrt((diff.x * diff.x + diff.z * diff.z).toFloat()).toDouble()
        val pearlInfo = TrajectoryInfo.GENERIC

        val velocity = pearlInfo.initialVelocity
        val gravity = pearlInfo.gravity

        val velocity2 = velocity * velocity
        val velocity4 = velocity2 * velocity2
        val y = diff.y

        val sqrt = velocity4 - gravity * (gravity * horizontalDistance * horizontalDistance + 2 * y * velocity2)

        if (sqrt < 0) {
            return null
        }

        val pitchRad = atan((velocity2 - sqrt(sqrt)) / (gravity * horizontalDistance))
        val yawRad = atan2(diff.z, diff.x)

        return Rotation(
            MathHelper.wrapDegrees(Math.toDegrees(yawRad).toFloat() - 90f),
            MathHelper.wrapDegrees((-Math.toDegrees(pitchRad)).toFloat())
        )
    }
}
