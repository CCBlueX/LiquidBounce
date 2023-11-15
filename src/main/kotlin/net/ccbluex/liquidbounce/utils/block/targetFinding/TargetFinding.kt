package net.ccbluex.liquidbounce.utils.block.targetFinding

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.canBeReplacedWith
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.client.getFace
import net.ccbluex.liquidbounce.utils.math.geometry.Face
import net.minecraft.block.*
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*


enum class AimMode(override val choiceName: String) : NamedChoice {
    CENTER("Center"),
    RANDOM("Random"),
    STABILIZED("Stabilized"),
    NEAREST_ROTATION("NearestRotation"),
}

class BlockPlacementTargetFindingOptions(
    val offsetsToInvestigate: List<Vec3i>,
    val stackToPlaceWith: ItemStack,
    val facePositionFactory: FaceTargetPositionFactory,
    /**
     * Compares two offsets by their priority. The offset with the higher priority will be prioritized.
     */
    val offsetPriorityGetter: (Vec3i) -> Double
) {
    companion object {
        val PRIORITIZE_LEAST_BLOCK_DISTANCE: (Vec3i) -> Double = { vec ->
            -Vec3d.of(vec).add(0.5, 0.5, 0.5).squaredDistanceTo(mc.player!!.pos)
        }
    }
}

data class BlockTargetPlan(
    val blockPosToInteractWith: BlockPos,
    val interactionDirection: Direction,
    val angleToPlayerEyeCosine: Double
) {
    constructor(pos: BlockPos, direction: Direction) : this(pos, direction, calculateAngleCosine(pos, direction))

    companion object {
        private fun calculateAngleCosine(pos: BlockPos, direction: Direction): Double {
            val targetPositionOnBlock = pos.toCenterPos().add(Vec3d.of(direction.vector).multiply(0.5))
            val deltaToPlayerPos = mc.player!!.eyes.subtract(targetPositionOnBlock)

            return deltaToPlayerPos.dotProduct(Vec3d.of(direction.vector)) / deltaToPlayerPos.length()
        }
    }

}

enum class BlockTargetingMode {
    PLACE_AT_NEIGHBOR,
    REPLACE_EXISTING_BLOCK
}

private fun findBestTargetPlanForTargetPosition(posToInvestigate: BlockPos, mode: BlockTargetingMode): BlockTargetPlan? {
    val directions = Direction.values()

    val options = directions.mapNotNull { direction ->
        val targetPlan =
            getTargetPlanForPositionAndDirection(posToInvestigate, direction, mode)
                ?: return@mapNotNull null

        // Check if the target face is pointing away from the player
        if (targetPlan.angleToPlayerEyeCosine < 0)
            return@mapNotNull null

        return@mapNotNull targetPlan
    }

    return options.maxByOrNull { it.angleToPlayerEyeCosine }
}

/**
 * @return null if it is impossible to target the block with the given parameters
 */
fun getTargetPlanForPositionAndDirection(pos: BlockPos, direction: Direction, mode: BlockTargetingMode): BlockTargetPlan? {
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

fun findBestBlockPlacementTarget(pos: BlockPos, options: BlockPlacementTargetFindingOptions): BlockPlacementTarget? {
    val state = pos.getState()!!

    // We cannot place blocks when there is already a block at that position
    if (isBlockSolid(state, pos)) {
        return null
    }

    val offsetsToInvestigate =
        options.offsetsToInvestigate.sortedByDescending {
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
        val targetMode = if (blockStateToInvestigate.isAir) {
            BlockTargetingMode.PLACE_AT_NEIGHBOR
        } else {
            BlockTargetingMode.REPLACE_EXISTING_BLOCK
        }

        // Check if we can actually replace the block?
        if (targetMode == BlockTargetingMode.REPLACE_EXISTING_BLOCK
            && !blockStateToInvestigate.canBeReplacedWith(posToInvestigate, options.stackToPlaceWith)
        )
            continue

        // Find the best plan to do the placement
        val targetPlan = findBestTargetPlanForTargetPosition(posToInvestigate, targetMode) ?: continue

        val currPos = targetPlan.blockPosToInteractWith

        // We found the optimal block to place the block/face to place at. Now we need to find a point on the face.
        // to rotate to
        val pointOnFace = findTargetPointOnFace(currPos.getState()!!, currPos, targetPlan, options) ?: continue

        val rotation = RotationManager.makeRotation(pointOnFace.point.add(Vec3d.of(currPos)), mc.player!!.eyes)

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
    val currBlock = currPos.getState()!!.block
    val truncate = currBlock is StairsBlock || currBlock is SlabBlock // TODO Find this out

    val shapeBBs = currState.getOutlineShape(mc.world!!, currPos, ShapeContext.of(mc.player!!)).boundingBoxes

    val face = shapeBBs.mapNotNull {
        var face = it.getFace(targetPlan.interactionDirection)

        if (truncate) {
            face = face.truncateY(0.5).requireNonEmpty() ?: return@mapNotNull null
        }

        val targetPos = options.facePositionFactory.producePositionOnFace(face, currPos)

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
    fun doesCrosshairTargetFullfitRequirements(crosshairTarget: BlockHitResult): Boolean {
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
