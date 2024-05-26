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
package net.ccbluex.liquidbounce.utils.block.targetFinding

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.canBeReplacedWith
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.getFace
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.geometry.Face
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.block.SideShapeType
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

enum class AimMode(override val choiceName: String) : NamedChoice {
    CENTER("Center"),
    RANDOM("Random"),
    STABILIZED("Stabilized"),
    NEAREST_ROTATION("NearestRotation"),
    REVERSE_YAW("ReverseYaw")
}

/**
 * Parameters used when generating a targeting plan for a block placement.
 *
 * @param offsetsToInvestigate the offsets (to the position) which the targeting algorithm will consider to place.
 * Prioritized with [offsetPriorityGetter]
 * @param offsetPriorityGetter compares two offsets by their priority. The offset with the higher priority will be
 * prioritized.
 * @param playerPositionOnPlacement the position the player will be at when placing the block
 */
class BlockPlacementTargetFindingOptions(
    val offsetsToInvestigate: List<Vec3i>,
    val stackToPlaceWith: ItemStack,
    val facePositionFactory: FaceTargetPositionFactory,
    val offsetPriorityGetter: (Vec3i) -> Double,
    val playerPositionOnPlacement: Vec3d,
    val playerPoseOnPlacement: EntityPose = EntityPose.STANDING
) {
    companion object {
        val PRIORITIZE_LEAST_BLOCK_DISTANCE: (Vec3i) -> Double = { vec ->
            -Vec3d.of(vec).add(0.5, 0.5, 0.5).squaredDistanceTo(mc.player!!.pos)
        }
    }
}

/**
 * A draft of a block placement
 *
 * @param blockPosToInteractWith the blockPos the player is eventually clicking on. Might not be the target pos, because
 * you need to interact with a neighboring block in order to place a block at a position
 * @param interactionDirection the direction the interaction should take place in. If the [blockPosToInteractWith] is
 * not the target pos, this will always point to it
 */
data class BlockTargetPlan(
    val blockPosToInteractWith: BlockPos,
    val interactionDirection: Direction,
) {
    /**
     * The center of the target block face
     */
    val targetPositionOnBlock =
        blockPosToInteractWith
            .toCenterPos()
            .add(Vec3d.of(interactionDirection.vector).multiply(0.5))

    /**
     * cosine of the angle between the expected player's eye position and the normal of the targeted face.
     */
    fun calculateAngleToPlayerEyeCosine(playerPos: Vec3d): Double {
        val deltaToPlayerPos = playerPos
            .add(0.0, mc.player!!.standingEyeHeight.toDouble(), 0.0)
            .subtract(targetPositionOnBlock)

        return deltaToPlayerPos.dotProduct(Vec3d.of(interactionDirection.vector)) / deltaToPlayerPos.length()
    }

}

enum class BlockTargetingMode {
    PLACE_AT_NEIGHBOR,
    REPLACE_EXISTING_BLOCK
}

private fun findBestTargetPlanForTargetPosition(
    posToInvestigate: BlockPos,
    mode: BlockTargetingMode,
    targetFindingOptions: BlockPlacementTargetFindingOptions
): BlockTargetPlan? {
    val directions = Direction.values()

    val options = directions.mapNotNull { direction ->
        val targetPlan =
            getTargetPlanForPositionAndDirection(posToInvestigate, direction, mode)
                ?: return@mapNotNull null

        // Check if the target face is pointing away from the player
        if (targetPlan.calculateAngleToPlayerEyeCosine(targetFindingOptions.playerPositionOnPlacement) < 0)
            return@mapNotNull null

        return@mapNotNull targetPlan
    }

    val currentRotation = RotationManager.serverRotation

    return options.minByOrNull {
        val rotation = RotationManager.makeRotation(
            it.targetPositionOnBlock,
            targetFindingOptions.playerPositionOnPlacement.add(0.0, player.standingEyeHeight.toDouble(), 0.0)
        )

        RotationManager.rotationDifference(rotation, currentRotation)
    }
}

/**
 * @return null if it is impossible to target the block with the given parameters
 */
fun getTargetPlanForPositionAndDirection(
    pos: BlockPos,
    direction: Direction,
    mode: BlockTargetingMode
): BlockTargetPlan? {
    when (mode) {
        BlockTargetingMode.PLACE_AT_NEIGHBOR -> {
            val currPos = pos.add(direction.opposite.vector)
            val currState = currPos.getState() ?: return null

            if (currState.isAir || currState.isReplaceable) {
                return null
            }

            return BlockTargetPlan(currPos, direction)
        }
        BlockTargetingMode.REPLACE_EXISTING_BLOCK -> {
            return BlockTargetPlan(pos, direction)
        }
    }
}

class PointOnFace(val face: Face, val point: Vec3d)

fun findBestBlockPlacementTarget(
    pos: BlockPos,
    options: BlockPlacementTargetFindingOptions
): BlockPlacementTarget? {
    val state = pos.getState()!!

    // We cannot place blocks when there is already a block at that position
    if (isBlockSolid(state, pos)) {
        return null
    }

    val offsetsToInvestigate = options.offsetsToInvestigate.sortedByDescending {
        options.offsetPriorityGetter(pos.add(it))
    }

    for (offset in offsetsToInvestigate) {
        val posToInvestigate = pos.add(offset)
        val blockStateToInvestigate = posToInvestigate.getState()!!

        // Already a block in that position?
        if (isBlockSolid(blockStateToInvestigate, posToInvestigate)) {
            continue
        }

        // Do we want to replace a block or place a block at a neighbor? This makes a difference as we would need to
        // target the block in order to replace it. If there is no block at the target position yet, we need to target
        // a neighboring block
        val targetMode = if (blockStateToInvestigate.isAir || blockStateToInvestigate.fluidState != null) {
            BlockTargetingMode.PLACE_AT_NEIGHBOR
        } else {
            BlockTargetingMode.REPLACE_EXISTING_BLOCK
        }

        // Check if we can actually replace the block?
        if (targetMode == BlockTargetingMode.REPLACE_EXISTING_BLOCK
            && !blockStateToInvestigate.canBeReplacedWith(posToInvestigate, options.stackToPlaceWith)
        ) {
            continue
        }

        // Find the best plan to do the placement
        val targetPlan = findBestTargetPlanForTargetPosition(posToInvestigate, targetMode, options) ?: continue

        val currPos = targetPlan.blockPosToInteractWith

        // We found the optimal block to place the block/face to place at. Now we need to find a point on the face.
        // to rotate to
        val pointOnFace = findTargetPointOnFace(currPos.getState()!!, currPos, targetPlan, options) ?: continue

        val rotation = RotationManager.makeRotation(
            pointOnFace.point.add(Vec3d.of(currPos)),
            options.playerPositionOnPlacement.add(
                0.0,
                mc.player!!.getEyeHeight(options.playerPoseOnPlacement).toDouble(),
                0.0
            )
        )

        return BlockPlacementTarget(
            currPos,
            posToInvestigate,
            targetPlan.interactionDirection,
            pointOnFace.face.from.y + currPos.y,
            rotation
        )
    }

    return null
}

private fun findTargetPointOnFace(
    currState: BlockState,
    currPos: BlockPos,
    targetPlan: BlockTargetPlan,
    options: BlockPlacementTargetFindingOptions
): PointOnFace? {
    val shapeBBs = currState.getOutlineShape(world, currPos, ShapeContext.of(player)).boundingBoxes

    val face = shapeBBs.mapNotNull {
        val face = it.getFace(targetPlan.interactionDirection)

        var searchFace = face

        // Try to aim at the upper portion of the block which makes it easier to switch from full blocks to half blocks
        if (searchFace.to.y >= 0.9) {
            searchFace = searchFace.truncateY(0.6).requireNonEmpty() ?: face
        }

        val targetPos = options.facePositionFactory.producePositionOnFace(searchFace, currPos)

        PointOnFace(
            face,
            targetPos
        )
    }.maxWithOrNull(
        Comparator.comparingDouble<PointOnFace> {
            it.point.subtract(
                Vec3d(
                    0.5,
                    0.5,
                    0.5
                )
            ).multiply(Vec3d.of(targetPlan.interactionDirection.vector)).lengthSquared()
        }.thenComparingDouble { it.point.y }
    )
    return face
}


data class BlockPlacementTarget(
    /**
     * BlockPos which is right-clicked
     */
    val interactedBlockPos: BlockPos,
    /**
     * Block pos at which a new block is placed
     */
    val placedBlock: BlockPos,
    val direction: Direction,
    /**
     * Some blocks must be placed above a certain height of the block. For example stairs and slabs must be placed
     * at the upper half (=> minY = 0.5) in order to be placed correctly
     */
    val minPlacementY: Double,
    val rotation: Rotation
) {
    fun doesCrosshairTargetFullFillRequirements(crosshairTarget: BlockHitResult): Boolean {
        if (crosshairTarget.type != HitResult.Type.BLOCK)
            return false
        if (crosshairTarget.blockPos != this.interactedBlockPos)
            return false
        if (crosshairTarget.side != this.direction)
            return false
        if (crosshairTarget.pos.y < this.minPlacementY)
            return false

        return true
    }
}

private fun isBlockSolid(state: BlockState, pos: BlockPos) =
    state.isSideSolid(mc.world!!, pos, Direction.UP, SideShapeType.CENTER)
