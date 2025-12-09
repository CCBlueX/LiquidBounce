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
