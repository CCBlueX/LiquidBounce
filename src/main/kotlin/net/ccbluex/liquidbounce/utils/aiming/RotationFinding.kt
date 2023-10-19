package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.jvm.optionals.getOrNull

fun raytraceBlock(
    eyes: Vec3d,
    pos: BlockPos,
    state: BlockState,
    range: Double,
    wallsRange: Double,
): VecRotation? {
    val offset = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    val shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player))

    for (box in shape.boundingBoxes.sortedBy { -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ) }) {
        return raytraceBox(
            eyes,
            box.offset(offset),
            range,
            wallsRange,
            rotationPreference = LeastDifferencePreference(RotationManager.makeRotation(pos.toCenterPos(), eyes))
        ) ?: continue
    }

    return null
}

/**
 * Find the best spot of the upper side of the block
 */
fun canSeeUpperBlockSide(
    eyes: Vec3d,
    pos: BlockPos,
    range: Double,
    wallsRange: Double,
): Boolean {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val minX = pos.x.toDouble()
    val y = pos.y + 0.99
    val minZ = pos.z.toDouble()

    for (x in 0.1..0.9 step 0.4) {
        for (z in 0.1..0.9 step 0.4) {
            val vec3 =
                Vec3d(
                    minX + x,
                    y,
                    minZ + z,
                )

            // skip because of out of range
            val distance = eyes.squaredDistanceTo(vec3)

            if (distance > rangeSquared) {
                continue
            }

            // check if target is visible to eyes
            val visible = facingBlock(eyes, vec3, pos, Direction.UP)

            // skip because not visible in range
            if (!visible && distance > wallsRangeSquared) {
                continue
            }

            return true
        }
    }

    return false
}

private class BestRotationTracker(val comparator: Comparator<Rotation>) {
    var bestInvisible: VecRotation? = null
        private set
    var bestVisible: VecRotation? = null
        private set

    fun considerRotation(
        rotation: VecRotation,
        visible: Boolean = true,
    ) {
        if (visible) {
            val isRotationBetter = getIsRotationBetter(base = this.bestVisible, rotation)

            if (isRotationBetter) {
                bestVisible = rotation
            }
        } else {
            val isRotationBetter = getIsRotationBetter(base = this.bestInvisible, rotation)

            if (isRotationBetter) {
                bestInvisible = rotation
            }
        }
    }

    private fun getIsRotationBetter(
        base: VecRotation?,
        newRotation: VecRotation,
    ): Boolean {
        return base?.let { currentlyBest ->
            this.comparator.compare(currentlyBest.rotation, newRotation.rotation) > 0
        } ?: true
    }
}

interface VisibilityPredicate {
    fun isVisible(
        eyesPos: Vec3d,
        targetSpot: Vec3d,
    ): Boolean
}

interface RotationPreference : Comparator<Rotation> {
    fun getPreferredSpot(
        eyesPos: Vec3d,
        range: Double,
    ): Vec3d
}

class BlockVisibilityPredicate(private val expectedTarget: BlockPos) : VisibilityPredicate {
    override fun isVisible(
        eyesPos: Vec3d,
        targetSpot: Vec3d,
    ): Boolean {
        return facingBlock(eyesPos, targetSpot, this.expectedTarget)
    }
}

class BoxVisibilityPredicate(private val expectedTarget: Box) : VisibilityPredicate {
    override fun isVisible(
        eyesPos: Vec3d,
        targetSpot: Vec3d,
    ): Boolean {
        return canSeePointFrom(eyesPos, targetSpot)
    }
}

/**
 * Find the best spot of a box to aim at.
 */
@Suppress("detekt:complexity.LongParameterList")
fun raytraceBox(
    eyes: Vec3d,
    box: Box,
    range: Double,
    wallsRange: Double,
    visibilityPredicate: VisibilityPredicate = BoxVisibilityPredicate(box),
    rotationPreference: RotationPreference = LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION,
): VecRotation? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val preferredSpot = rotationPreference.getPreferredSpot(eyes, range)
    val preferredSpotOnBox = box.raycast(eyes, preferredSpot).getOrNull()

    if (preferredSpotOnBox != null) {
        val preferredSpotDistance = eyes.squaredDistanceTo(preferredSpotOnBox)

        // If pattern-generated spot is visible or its distance is within wall range, then return right here.
        // No need to enter the loop when we already have a result.
        val validCauseBelowWallsRange = preferredSpotDistance < wallsRangeSquared

        val validCauseVisible = visibilityPredicate.isVisible(eyesPos = eyes, targetSpot = preferredSpotOnBox)

        if (validCauseBelowWallsRange || validCauseVisible && preferredSpotDistance < rangeSquared) {
            return VecRotation(RotationManager.makeRotation(preferredSpot, eyes), preferredSpot)
        }
    }

    val bestRotationTracker = BestRotationTracker(rotationPreference)

    // There are some spots that loops cannot detect, therefore this is used
    // since it finds the nearest spot within the requested range.
    val nearestSpot = getNearestPoint(eyes, box)

    considerSpot(
        preferredSpot,
        box,
        eyes,
        visibilityPredicate,
        rangeSquared,
        wallsRangeSquared,
        nearestSpot,
        bestRotationTracker,
    )

    for (x in 0.0..1.0 step 0.1) {
        for (y in 0.0..1.0 step 0.1) {
            for (z in 0.0..1.0 step 0.1) {
                val spot =
                    Vec3d(
                        box.minX + (box.maxX - box.minX) * x,
                        box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z,
                    )

                considerSpot(
                    spot,
                    box,
                    eyes,
                    visibilityPredicate,
                    rangeSquared,
                    wallsRangeSquared,
                    spot,
                    bestRotationTracker,
                )
            }
        }
    }

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

@Suppress("detekt:complexity.LongParameterList")
private fun considerSpot(
    preferredSpot: Vec3d,
    box: Box,
    eyes: Vec3d,
    visibilityPredicate: VisibilityPredicate,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    spot: Vec3d,
    bestRotationTracker: BestRotationTracker,
) {
    val spotOnBox = box.raycast(eyes, preferredSpot).getOrNull() ?: return
    val distance = eyes.squaredDistanceTo(spotOnBox)

    val visible = visibilityPredicate.isVisible(eyes, spotOnBox)

    // Is either spot visible or distance within wall range?
    if ((!visible || distance > rangeSquared) && distance > wallsRangeSquared) {
        return
    }

    val rotation = RotationManager.makeRotation(spot, eyes)

    bestRotationTracker.considerRotation(VecRotation(rotation, spot), visible)
}

/**
 * Determines if the player is able to see a box
 */
fun canSeeBox(
    eyes: Vec3d,
    box: Box,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos? = null,
): Boolean {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    scanPositionsInBox(box) { posInBox ->
        // skip because of out of range
        val distance = eyes.squaredDistanceTo(posInBox)

        if (distance > rangeSquared) {
            return@scanPositionsInBox
        }

        // check if target is visible to eyes
        val visible =
            if (expectedTarget != null) {
                facingBlock(eyes, posInBox, expectedTarget)
            } else {
                canSeePointFrom(eyes, posInBox)
            }

        // skip because not visible in range
        if (!visible && distance > wallsRangeSquared) {
            return@scanPositionsInBox
        }

        return true
    }

    return false
}

private inline fun scanPositionsInBox(
    box: Box,
    step: Double = 0.1,
    fn: (Vec3d) -> Unit,
) {
    for (x in 0.1..0.9 step step) {
        for (y in 0.1..0.9 step step) {
            for (z in 0.1..0.9 step step) {
                val vec3 =
                    Vec3d(
                        box.minX + (box.maxX - box.minX) * x,
                        box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z,
                    )

                fn(vec3)
            }
        }
    }
}

/**
 * Find the best spot of the upper block side
 */
fun raytraceUpperBlockSide(
    eyes: Vec3d,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos,
    rotationPreference: RotationPreference = LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION,
): VecRotation? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val vec3d = Vec3d.of(expectedTarget).add(0.0, 0.9, 0.0)

    val bestRotationTracker = BestRotationTracker(rotationPreference)

    for (x in 0.1..0.9 step 0.1) {
        for (z in 0.1..0.9 step 0.1) {
            val vec3 = vec3d.add(x, 0.0, z)

            // skip because of out of range
            val distance = eyes.squaredDistanceTo(vec3)

            if (distance > rangeSquared) {
                continue
            }

            // check if target is visible to eyes
            val visible = facingBlock(eyes, vec3, expectedTarget, Direction.UP)

            // skip because not visible in range
            if (!visible && distance > wallsRangeSquared) {
                continue
            }

            val rotation = RotationManager.makeRotation(vec3, eyes)

            bestRotationTracker.considerRotation(VecRotation(rotation, vec3), visible)
        }
    }

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}
