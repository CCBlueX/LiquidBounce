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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.rotationTiming
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.hasAnySolidPlacementNeighbor
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.min

/**
 * Builds under the fall [ModuleClutch] predicts. Not selectable: scaffold switches to it for as long as the fall
 * lasts.
 */
internal object ScaffoldClutchTechnique : ScaffoldTechnique("Clutch") {

    // How far down the fall a block is aimed at already
    private const val TICKS_AHEAD = 10

    /**
     * An anticheat only takes a click for certain if the server had its rotation the tick before, so the aim is at
     * what the next tick clicks. The on tick timings send the rotation along with the click instead.
     */
    private val aimsAhead
        get() = rotationTiming == NORMAL

    override fun findPlacementTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        val fall = ModuleClutch.fall ?: return null
        val ahead = if (aimsAhead) 1 else 0
        val clickFrom = if (aimsAhead) fall.first() else player.position()
        var feetY = clickFrom.y

        for (position in fall.subList(ahead, min(fall.size, ahead + TICKS_AHEAD))) {
            // A block catches the player where its feet come down through the top of a block layer. At the click
            // the feet have to be above that layer already
            val top = floor(feetY)
            feetY = position.y
            if (position.y >= top || top > clickFrom.y) {
                continue
            }

            val landing = BlockPos.containing(position.x, top - 1, position.z)
            // Next to a block it only waits for that block's face to turn towards the player. Anything else needs
            // a row of blocks towards it first, or is left to a layer further down
            val supported = landing.hasAnySolidPlacementNeighbor()
            val searchOptions = BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    (if (supported) BlockPosOffsets.NO_OFFSET else BlockPosOffsets.FULL).offsets,
                    BlockPlacementTargetFindingOptions.leastBlockDistanceToPos(position),
                ),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = bestStack,
                PlayerLocationOnPlacement(position = clickFrom),
            )

            val target = findBestBlockPlacementTarget(landing, searchOptions)
            if (target != null || supported) {
                return target
            }
        }

        return null
    }

    override fun getCrosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? =
        target?.verifyClick(if (aimsAhead) RotationManager.serverRotation else rotation)

}
