/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */

package net.ccbluex.liquidbounce.utils.aiming.projectiles

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.phys.Vec3

/**
 * Uses the best available implementation of [ProjectileAngleCalculator]
 */
object SituationalProjectileAngleCalculator: ProjectileAngleCalculator() {
    override fun calculateAngleFor(
        projectileInfo: TrajectoryInfo,
        sourcePos: Vec3,
        targetPosFunction: PositionExtrapolation,
        targetShape: EntityDimensions
    ): Rotation? {
        val basePos = targetPosFunction.getPositionInTicks(0.0)

        val actualImplementation = when {
            // Our flagship implementation is unstable at low distances...
            basePos.distanceToSqr(sourcePos) < 5.0 * 5.0 -> PolynomialProjectileAngleCalculator
            else -> CydhranianProjectileAngleCalculator
        }

        return actualImplementation.calculateAngleFor(projectileInfo, sourcePos, targetPosFunction, targetShape)
    }
}
