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

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getFacePositionFactoryForConfig
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldDownFeature
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

/**
 * Normal technique, which is basically just normal scaffold.
 */
object ScaffoldNormalTechnique : ScaffoldTechnique("Normal") {

    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1, -2, 2))
    internal val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1))

    override fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        // Prioritize the block that is closest to the line, if there was no line found, prioritize the nearest block
        val priorityGetter: (Vec3i) -> Double = if (optimalLine != null) {
            { vec -> -optimalLine.squaredDistanceTo(Vec3d.of(vec).add(0.5, 0.5, 0.5)) }
        } else {
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        }

        // Face position factory for current config
        val facePositionFactory = getFacePositionFactoryForConfig(predictedPos, predictedPose)

        val searchOptions = BlockPlacementTargetFindingOptions(
            if (ScaffoldDownFeature.shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
            bestStack,
            facePositionFactory,
            priorityGetter,
            predictedPos,
            predictedPose
        )

        return findBestBlockPlacementTarget(getTargetedPosition(predictedPos.toBlockPos()), searchOptions)
    }

    private fun commonOffsetToInvestigate(xzOffsets: List<Int>): List<Vec3i> {
        return xzOffsets.flatMap { x ->
            xzOffsets.flatMap { z ->
                (0 downTo -1).flatMap { y ->
                    listOf(Vec3i(x, y, z))
                }
            }
        }
    }


}
