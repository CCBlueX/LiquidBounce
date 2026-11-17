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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Places the first missing block up to [length] blocks ahead in the direction the player looks.
 */
object ScaffoldExpandFeature : ToggleableValueGroup(ModuleScaffold, "Expand", false) {

    private val length by int("Length", 4, 1..10, "blocks")

    fun findPlacementTarget(predictedPos: Vec3, predictedPose: Pose, bestStack: ItemStack): BlockPlacementTarget? {
        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions.Default,
            FaceHandlingOptions(
                CenterTargetPositionFactory,
                considerFacingAwayFaces = true
            ),
            stackToPlaceWith = bestStack,
            PlayerLocationOnPlacement(position = predictedPos, pose = predictedPose)
        )

        for (i in 0..length) {
            val position = getTargetedPosition(expandPos(predictedPos, i))

            return findBestBlockPlacementTarget(position, searchOptions) ?: continue
        }

        return null
    }

    fun rotation(target: BlockPlacementTarget?): Rotation? {
        val blockCenter = target?.placedBlock?.center ?: return null

        return Rotation.lookingAt(point = blockCenter, from = player.eyePosition)
    }

    /** What a look has to meet to place [target]: the space it fills, seen through whatever is in front of it. */
    fun surface(target: BlockPlacementTarget) = Surface(AABB(target.placedBlock), throughWalls = true)

    /** A block ahead is behind others, so the planned hit stands in once the look meets [surface]. */
    fun click(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? {
        target ?: return null
        return target.verifyClick(rotation)
            ?: target.blockHitResult.takeIf { surface(target).isHitBy(player.eyePosition, rotation) }
    }

    private fun expandPos(position: Vec3, expand: Int, yaw: Float = player.yRot) = position.toBlockPos().offset(
        (-sin(yaw.toRadians()) * expand).toInt(),
        0,
        (cos(yaw.toRadians()) * expand).toInt()
    )

}
