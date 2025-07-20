package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.math.Vec3d

/**
 * Uses the best available implementation of [ProjectileAngleCalculator]
 */
object SituationalProjectileAngleCalculator: ProjectileAngleCalculator() {
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)

        val actualImplementation = when {
            // Our flagship implementation is unstable at low distances...
            basePos.distanceTo(sourcePos) < 5.0 -> PolynomialProjectileAngleCalculator
            else -> CydhranianProjectileAngleCalculator
        }

        return actualImplementation.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
    }
}
