package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.ConstantPositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

/**
 * Calculates the shooting angle which hits the supplied target
 */
abstract class ProjectileAngleCalculator {
    abstract fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3d,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions,
    ): Rotation?

    fun calculateAngleForStaticTarget(
        projectileInfo: TrajectoryInfo,
        target: Vec3d,
        shape: EntityDimensions
    ): Rotation? {
        return this.calculateAngleFor(
            projectileInfo,
            sourcePos = player.eyePos,
            targetPosFunction = ConstantPositionExtrapolation(target),
            targetShape = shape
        )
    }

    fun calculateAngleForEntity(projectileInfo: TrajectoryInfo, entity: LivingEntity): Rotation? {
        return this.calculateAngleFor(
            projectileInfo,
            sourcePos = player.eyePos,
            targetPosFunction = PositionExtrapolation.getBestForEntity(entity),
            targetShape = entity.dimensions
        )
    }
}
