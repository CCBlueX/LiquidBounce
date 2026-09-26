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
package net.ccbluex.liquidbounce.utils.block.targetfinding

import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.outlineShape
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.anyHorizontal
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.math.center
import net.ccbluex.liquidbounce.utils.math.contains
import net.ccbluex.liquidbounce.utils.math.distanceToSqr
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.SupportType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.Comparator.comparingDouble

enum class AimMode(override val tag: String) : Tagged {
    CENTER("Center"),
    RANDOM("Random"),
    STABILIZED("Stabilized"),
    NEAREST_ROTATION("NearestRotation"),
    REVERSE_YAW("ReverseYaw"),
    DIAGONAL_YAW("DiagonalYaw"),
    ANGLE_YAW("AngleYaw"),
    EDGE_POINT("EdgePoint"),
}

/**
 * Parameters used when generating a targeting plan for a block placement.
 */
data class BlockPlacementTargetFindingOptions(
    val offsetOptions: BlockOffsetOptions,
    val faceHandlingOptions: FaceHandlingOptions,
    val stackToPlaceWith: ItemStack,
    val playerLocationOnPlacement: PlayerLocationOnPlacement
) {
    companion object {
        @JvmStatic
        fun leastBlockDistanceToLine(line: Line): Comparator<BlockPos> =
            comparingDouble { blockPos ->
                val shape = blockPos.outlineShape.move(blockPos)
                if (shape.isEmpty) {
                    -line.distanceToSqr(blockPos.center)
                } else {
                    -(line.getNearestPointTo(shape)?.distanceSquared ?: Double.POSITIVE_INFINITY)
                }
            }

        @JvmStatic
        fun leastBlockDistanceToPos(pos: Vec3): Comparator<BlockPos> =
            comparingDouble { blockPos ->
                val shape = blockPos.outlineShape.move(blockPos)
                if (shape.isEmpty) {
                    -blockPos.distToCenterSqr(pos)
                } else {
                    -shape.distanceToSqr(pos)
                }
            }
    }
}

/**
 * Contains information about offsets (to the target pos) which should be investigated.
 *
 * @param offsetsToInvestigate the offsets (to the position) which the targeting algorithm will consider to place.
 * Prioritized with [priorityComparator]
 * @param priorityComparator compares two offsets by their priority. An offset which ranks higher is prioritized.
 */
data class BlockOffsetOptions(
    val offsetsToInvestigate: List<Vec3i>,
    val priorityComparator: Comparator<BlockPos>,
) {
    companion object {
        @JvmField
        val Default = BlockOffsetOptions(
            BlockPosOffsets.NO_OFFSET.offsets,
            comparingDouble { blockPos ->
                val pos = player.position()
                val shape = blockPos.outlineShape.move(blockPos)
                if (shape.isEmpty) {
                    -blockPos.distToCenterSqr(pos)
                } else {
                    -shape.distanceToSqr(pos)
                }
            },
        )
    }
}

/**
 * Decides how scaffold processes the faces of the considered target blocks.
 *
 * @param facePositionFactory given a face, it will yield a point on the face to target.
 * @param considerFacingAwayFaces decides whether scaffold will consider faces which point away from the player camera
 * as possible targets, as it is mostly nonsensical.
 * The expand-scaffold, for example, needs them to be considered to
 * work.
 */
data class FaceHandlingOptions(
    val facePositionFactory: FaceTargetPositionFactory,
    val considerFacingAwayFaces: Boolean = false,
)

/**
 * Contains the player's state on placement. Everything that depends on the player has to be read from here instead
 * of the live player, so that the aim is derived from one single state.
 *
 * The state may be a prediction of where the player will be when the block is actually placed (scaffold predicts it
 * to pre-aim). It is only what the _aim_ is derived from: whether the click is possible right now has to be verified
 * against the eye the server will use for the interaction, see [BlockPlacementTarget.doesCrosshairTargetMatchRequirements].
 *
 * @param position the player's position (on placement)
 * @param pose the player's pose (on placement)
 * @param rotation the rotation to aim from. Target finding keeps the rotation change minimal relative to it
 * @param movingHorizontally whether the player moves horizontally (on placement)
 */
data class PlayerLocationOnPlacement(
    val position: Vec3 = player.position(),
    val pose: Pose = player.pose,
    val rotation: Rotation = RotationManager.serverRotation,
    val movingHorizontally: Boolean = player.input.keyPresses.anyHorizontal,
) {
    val eyeHeight: Float = player.getEyeHeight(pose)
    val eyePos: Vec3 = position.add(0.0, eyeHeight.toDouble(), 0.0)
}

/**
 * A click a placement can be performed with
 *
 * @param clickedBlockPos the blockPos the player is eventually clicking on. Might not be the target pos, because
 * you need to interact with a neighboring block in order to place a block at a position
 * @param direction the clicked face of [clickedBlockPos]. If [clickedBlockPos] is not the target pos, this always
 * points to it
 * @param pointOnFace the exact point on [direction]'s face that is aimed at
 */
private data class BlockTargetPlan(
    val clickedBlockPos: BlockPos,
    val direction: Direction,
    val pointOnFace: PointOnFace,
) {
    /**
     * [pointOnFace] in world coordinates.
     */
    val interactionPoint: Vec3 = pointOnFace.point + clickedBlockPos
}

/**
 * Finds the click that places the stack at [posToInvestigate], preferring the face the click most directly reaches.
 *
 * The block either has to be placed against one of the neighbouring blocks or replaces [posToInvestigate] itself.
 * Which of both happens is decided by vanilla from the clicked face and point. `BlockItem.updatePlacementContext`
 * runs first — scaffolding moves the placed block off the block that was clicked — and [BlockPlaceContext.clickedPos]
 * of that result is what has to equal [posToInvestigate].
 *
 * @return null if there is no click placing the stack at [posToInvestigate]
 */
private fun findBestTargetPlan(
    posToInvestigate: BlockPos,
    options: BlockPlacementTargetFindingOptions
): BlockTargetPlan? {
    val eyePos = options.playerLocationOnPlacement.eyePos
    val considerFacingAwayFaces = options.faceHandlingOptions.considerFacingAwayFaces

    var bestPlan: BlockTargetPlan? = null
    var bestFacing = Double.NEGATIVE_INFINITY

    // Rank the candidates by how directly their face points at the eye: 1.0 when the eye is straight in front of the
    // face, 0.0 when it lies in the face plane. This is what decides which face a click reaches — a steeply
    // downward look clicks the top face, even though a side face's sampled point lies closer to the eye.
    fun consider(clickedBlockPos: BlockPos, direction: Direction, outlineShape: VoxelShape) {
        if (outlineShape.isEmpty) return

        val plan = findTargetPlan(clickedBlockPos, direction, posToInvestigate, outlineShape, options) ?: return

        val toEye = eyePos.subtract(plan.interactionPoint)
        val facing = toEye.dot(plan.direction.unitVec3) / toEye.length()

        if (!considerFacingAwayFaces && facing < 0.0) {
            return
        }

        if (facing > bestFacing) {
            bestPlan = plan
            bestFacing = facing
        }
    }

    // Scaffolding and light are a full cube only while that item is in the main hand. The placement raycast uses
    // CollisionContext.of(player), so the sampled face has to come from that same context. An empty one never does.
    val collisionContext = CollisionContext.of(player)
    val targetShape = posToInvestigate.outlineShape(collisionContext)

    for (direction in Direction.entries) {
        val neighbour = posToInvestigate.relative(direction.opposite)
        consider(neighbour, direction, neighbour.outlineShape(collisionContext))
        consider(posToInvestigate, direction, targetShape)
    }

    return bestPlan
}

/**
 * @return the click on [clickedBlockPos]'s [direction] face, or null if it would not place the stack at
 * [posToInvestigate]
 */
private fun findTargetPlan(
    clickedBlockPos: BlockPos,
    direction: Direction,
    posToInvestigate: BlockPos,
    outlineShape: VoxelShape,
    options: BlockPlacementTargetFindingOptions
): BlockTargetPlan? {
    val pointOnFace = findTargetPointOnFace(clickedBlockPos, direction, outlineShape, options) ?: return null

    val plan = BlockTargetPlan(clickedBlockPos, direction, pointOnFace)

    // Let vanilla decide where the clicked block ends up: it is replaced if it is replaceable with the stack,
    // otherwise the block is placed on the clicked face. Scaffolding then walks off that block.
    val eyeLocal = options.playerLocationOnPlacement.eyePos - clickedBlockPos
    val placementContext = BlockPlaceContext(
        player,
        InteractionHand.MAIN_HAND,
        options.stackToPlaceWith,
        BlockHitResult(plan.interactionPoint, direction, clickedBlockPos, outlineShape.contains(eyeLocal)),
    )

    if (placementContext.resolvePlacementAt(posToInvestigate) == null) {
        return null
    }

    return plan
}

private data class PointOnFace(
    val face: AlignedFace,
    val point: Vec3,
) {
    companion object {
        private val comparators = enumMapOf<Direction, Comparator<PointOnFace>> { direction ->
            comparingDouble<PointOnFace> {
                it.point.subtract(0.5, 0.5, 0.5)
                    .multiply(direction.unitVec3)
                    .lengthSqr()
            }.thenComparingDouble { it.point.y }
        }

        fun comparator(direction: Direction): Comparator<PointOnFace> =
            comparators[direction]!!
    }
}

/**
 * Whether this position already holds a solid block, i.e. one that carries the player or another block on its top
 * center.
 *
 * A partially filled position (a slab, a flower) does not count: the stack cannot go there, but the offsets are
 * still worth investigating, which is why the requested position must not be filtered by "can receive the stack".
 * That check belongs to the candidates, see [findTargetPlan].
 */
private fun BlockPos.holdsSolidBlock(): Boolean =
    stateOrEmpty.isFaceSturdy(world, this, Direction.UP, SupportType.CENTER)

/**
 * Finds the best way to place [BlockPlacementTargetFindingOptions.stackToPlaceWith] at [pos] or, if [pos] cannot
 * receive it, at one of the positions the offset options investigate (in priority order).
 *
 * @return null when neither [pos] nor any investigated offset can receive the stack
 */
fun findBestBlockPlacementTarget(pos: BlockPos, options: BlockPlacementTargetFindingOptions): BlockPlacementTarget? {
    // Nothing to place — searching the offsets would only find a spot for a stray block next to the one at [pos].
    if (pos.holdsSolidBlock()) {
        return null
    }

    val offsetsToInvestigate = options.offsetOptions.offsetsToInvestigate.sortedWith { a, b ->
        // Sort DESCENDING!
        options.offsetOptions.priorityComparator.compare(pos + b, pos + a)
    }

    for (offset in offsetsToInvestigate) {
        val posToInvestigate = pos.offset(offset)

        val targetPlan = findBestTargetPlan(posToInvestigate, options) ?: continue

        return BlockPlacementTarget(
            targetPlan.clickedBlockPos,
            posToInvestigate,
            targetPlan.direction,
            targetPlan.interactionPoint,
            targetPlan.pointOnFace.face.from.y + targetPlan.clickedBlockPos.y,
            Rotation.lookingAt(
                point = targetPlan.interactionPoint,
                from = options.playerLocationOnPlacement.eyePos,
            ),
        )
    }

    return null
}

private fun findTargetPointOnFace(
    clickedBlockPos: BlockPos,
    direction: Direction,
    outlineShape: VoxelShape,
    options: BlockPlacementTargetFindingOptions
): PointOnFace? {
    val comparator = PointOnFace.comparator(direction)
    var best: PointOnFace? = null

    outlineShape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
        val face = AlignedFace.get(direction, minX, minY, minZ, maxX, maxY, maxZ)

        var searchFace = face

        // Try to aim at the upper portion of the block which makes it easier to switch from full blocks to half blocks
        if (searchFace.to.y >= 0.9) {
            searchFace = searchFace.truncateY(0.51).requireNonEmpty() ?: face
        }

        val targetPos =
            options.faceHandlingOptions.facePositionFactory.producePositionOnFace(
                searchFace,
                clickedBlockPos,
                options.playerLocationOnPlacement,
            ) ?: return@forAllBoxes

        val pointOnFace = PointOnFace(face, targetPos)

        if (best == null || comparator.compare(pointOnFace, best) > 0) {
            best = pointOnFace
        }
    }

    return best
}


/**
 * Tolerance for [BlockPlacementTarget.minPlacementY]: the hit position is reconstructed by interpolating the ray,
 * so a hit exactly on the face plane can land a few ulps below the box boundary.
 */
private const val FACE_HIT_EPSILON = 1.0E-7

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
     * Exact point on [interactedBlockPos] selected by target finding.
     */
    val interactionPoint: Vec3,
    /**
     * Lower bound (world y) of the shape box [interactionPoint] was sampled from. One block face can consist of
     * several boxes lying in the same plane (a stair's north face is a lower slab box plus an upper step box), which
     * [direction] alone cannot tell apart.
     */
    val minPlacementY: Double,
    /**
     * The rotation aiming at [interactionPoint]. It is derived from the (possibly predicted)
     * [PlayerLocationOnPlacement.eyePos], so a click has to be verified from the eye the server will use.
     */
    val rotation: Rotation
) {

    val blockHitResult: BlockHitResult
        get() = BlockHitResult(
            interactionPoint,
            direction,
            interactedBlockPos,
            false
        )

    fun doesCrosshairTargetMatchRequirements(crosshairTarget: BlockHitResult): Boolean {
        return when {
            crosshairTarget.type != HitResult.Type.BLOCK -> false
            crosshairTarget.blockPos != this.interactedBlockPos -> false
            crosshairTarget.direction != this.direction -> false
            crosshairTarget.location.y < this.minPlacementY - FACE_HIT_EPSILON -> false
            else -> true
        }
    }
}

data class PlacementPlan(
    val targetPos: BlockPos,
    val placementTarget: BlockPlacementTarget,
    val hotbarItemSlot: HotbarItemSlot
)
