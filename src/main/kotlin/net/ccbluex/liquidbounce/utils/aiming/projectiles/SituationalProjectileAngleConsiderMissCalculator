@file:Suppress("unused","all")
package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.EntityDimensions
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

object SituationalProjectileAngleConsiderMissCalculator : ProjectileAngleCalculator() {

    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)
        val candidate = if (basePos.distanceTo(sourcePos) < 5.0) {
            PolynomialProjectileAngleCalculator.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
        } else {
            CydhranianProjectileAngleCalculator.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
        } ?: return null

        val world = player.world

        return if (isTrajectoryClear(world,
                sourcePos,
                candidate.pitch,
                candidate.yaw, projectileInfo, targetPosFunction, targetShape)) {
            candidate
        } else {
            null
        }
    }

    private fun isTrajectoryClear(
        world: World,
        startPos: Vec3d,
        pitch: Float,
        yaw: Float,
        projectileInfo: TrajectoryInfo,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Boolean {
        val rotation = Rotation(yaw, pitch)
        val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            entity = player,
            trajectoryInfo = projectileInfo,
            rotation = rotation
        )

        val result = renderer.runSimulation(300)
        val hit = result.hitResult ?: return false

        val baseTargetPos = targetPosFunction.getPositionInTicks(0.0)
        val targetBox = targetShape.getBoxAt(baseTargetPos)

        return when (hit) {
            is EntityHitResult -> hit.entity.boundingBox.intersects(targetBox)
            is BlockHitResult -> false
            else -> false
        }
    }
}
