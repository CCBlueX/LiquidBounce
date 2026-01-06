@file:Suppress("unused","all")
package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

object SituationalProjectileAngleConsiderMissCalculator : ProjectileAngleCalculator() {

    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)
        val candidate = if (basePos.distanceTo(sourcePos) < 5.0) {
            PolynomialProjectileAngleCalculator.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
        } else {
            CydhranianProjectileAngleCalculator.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
        } ?: return null

        return if (isTrajectoryClear(player.level(),
                sourcePos,
                candidate.pitch,
                candidate.yaw, projectileInfo, targetPosFunction, targetShape)) {
            candidate
        } else {
            null
        }
    }

    private fun isTrajectoryClear(
        world: Level,
        startPos: Vec3,
        pitch: Float,
        yaw: Float,
        projectileInfo: TrajectoryInfo,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Boolean {
        val rotation = Rotation(yaw, pitch)
        val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            owner = player,
            trajectoryInfo = projectileInfo,
            rotation = rotation
        )

        val result = renderer.runSimulation(300)
        val hit = result.hitResult ?: return false

        val baseTargetPos = targetPosFunction.getPositionInTicks(0.0)
        val targetBox = targetShape.makeBoundingBox(baseTargetPos)

        return when (hit) {
            is EntityHitResult -> hit.entity.boundingBox.intersects(targetBox)
            is BlockHitResult -> false
            else -> false
        }
    }
}
