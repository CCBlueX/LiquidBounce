package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object BasicLandingPointAngleCalculator : ProjectileAngleCalculator() {
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)
        val diff = basePos.subtract(sourcePos)

        val horizontalDistance = MathHelper.sqrt((diff.x * diff.x + diff.z * diff.z).toFloat()).toDouble()
        val velocity = projectileInfo.initialVelocity
        val gravity = projectileInfo.gravity

        val v2 = velocity * velocity
        val v4 = v2 * v2
        val y = diff.y
        val sqrt = v4 - gravity * (gravity * horizontalDistance * horizontalDistance + 2 * y * v2)
        if (sqrt < 0) {
            return null
        }

        val pitchRad = atan((v2 - sqrt(sqrt)) / (gravity * horizontalDistance))
        val yawRad = atan2(diff.z, diff.x)

        return Rotation(
            MathHelper.wrapDegrees(Math.toDegrees(yawRad).toFloat() - 90f),
            MathHelper.wrapDegrees((-Math.toDegrees(pitchRad)).toFloat())
        )
    }
}
