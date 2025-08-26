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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.toRadians
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
                BlockOffsetOptions(
                    listOf(Vec3i.ZERO),
                    BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                ),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = bestStack,
                PlayerLocationOnPlacement(position = position.toVec3d(), pose = predictedPose),
            )

            return findBestBlockPlacementTarget(position, searchOptions) ?: continue
        }

        return null
    }

    override fun getRotations(target: BlockPlacementTarget?): Rotation? {
        val blockCenter = target?.placedBlock?.toCenterPos() ?: return null

        return Rotation.lookingAt(point = blockCenter, from = player.eyePos)
    }

    override fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? {
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
