/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

object SituationalProjectileAngleConsiderMissCalculator : ProjectileAngleCalculator {

    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        return SituationalProjectileAngleCalculator.calculateAngleFor(
            projectileInfo,
            sourcePos,
            targetPosFunction,
            targetShape
        )?.takeIf {
            isTrajectorySimulatedHitEntity(it, projectileInfo, targetPosFunction, targetShape)
        }
    }

    private fun isTrajectorySimulatedHitEntity(
        rotation: Rotation,
        projectileInfo: TrajectoryInfo,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Boolean {
        val renderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            owner = player,
            trajectoryInfo = projectileInfo,
            rotation = rotation
        )

        val result = renderer.runSimulation(300)
        val hit = result.hitResult ?: return false

        val baseTargetPos = targetPosFunction.getPositionInTicks(0.0)
        val targetBox = targetShape.makeBoundingBox(baseTargetPos)

        return hit is EntityHitResult && hit.entity.box.intersects(targetBox)
    }
}
