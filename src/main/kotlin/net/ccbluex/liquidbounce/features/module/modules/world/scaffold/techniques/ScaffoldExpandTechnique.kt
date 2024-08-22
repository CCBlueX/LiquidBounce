/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.AngleLine
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.cos
import kotlin.math.sin

/**
 * Normal technique, which is basically just normal scaffold.
 */
object ScaffoldExpandTechnique : ScaffoldTechnique("Expand") {

    private val expandLength by int("Length", 4, 1..10, "blocks")

    override fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        for(i in 0..expandLength) {
            val position = getTargetedPosition(expandPos(predictedPos, i))

            val searchOptions = BlockPlacementTargetFindingOptions(
                listOf(Vec3i(0, 0, 0)),
                bestStack,
                CenterTargetPositionFactory,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                // THIS IS A HACK!! - we should use the player's position instead of the
                // block position, but we cannot aim at something that we don't see
                // from our position
                position.toVec3d(),
                // we don't need to predict the pose, as we throw away this data on [getRotations] anyway
                // predictedPose
            )

            return findBestBlockPlacementTarget(position, searchOptions) ?: continue
        }

        return null
    }

    override fun getRotations(target: BlockPlacementTarget?): AngleLine? {
        return AngleLine(toPoint = target?.placedBlock?.toCenterPos() ?: return null)
    }

    override fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Orientation): BlockHitResult? {
        val crosshairTarget = super.getCrosshairTarget(target ?: return null, rotation)

        if (crosshairTarget != null && target.doesCrosshairTargetFullFillRequirements(crosshairTarget)) {
            return crosshairTarget
        }

        return target.blockHitResult
    }

    private fun expandPos(position: Vec3d, expand: Int, yaw: Float = player.yaw) = position.toBlockPos().add(
        (-sin(yaw.toRadians()) * expand).toInt(),
        0,
        (cos(yaw.toRadians()) * expand).toInt()
    )

}
