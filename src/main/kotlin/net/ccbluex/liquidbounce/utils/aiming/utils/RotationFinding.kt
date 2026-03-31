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
@file:Suppress("Detekt.TooManyFunctions")

package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.fastutil.step
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.preference.RotationPreference
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.kotlin.range
import net.ccbluex.liquidbounce.utils.math.firstHit
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.ccbluex.liquidbounce.utils.math.isHitByLine
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.pointAtProportion
import net.ccbluex.liquidbounce.utils.math.samplePointOnSide
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toSortedAabbs
import net.ccbluex.liquidbounce.utils.raytracing.clip
import net.ccbluex.liquidbounce.utils.raytracing.isFacingBlock
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.math.max

private val ITERATION_PROPORTIONS_LOOSE = doubleArrayOf(0.1, 0.5, 0.9)
private val ITERATION_PROPORTIONS = doubleArrayOf(0.05, 0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95)
private val ITERATION_PROPORTIONS_PRECISE = doubleArrayOf(
    0.05, 0.1, 0.15, 0.2, 0.25, 0.30, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95
)

fun raytraceBlockRotation(
    eyes: Vec3,
    pos: BlockPos,
    state: BlockState,
    range: Double,
    wallsRange: Double,
): RotationWithVector? {
    val outlineShape = state.getShape(world, pos, CollisionContext.of(player))
    if (outlineShape.isEmpty) {
        return null
    }

    return raytraceBoxes(
        eyes = eyes,
        boxes = outlineShape.move(pos).toSortedAabbs(),
        range = range,
        wallsRange = wallsRange,
        visibilityPredicate = VisibilityPredicate.Block(pos, null),
        rotationPreference = LeastDifferencePreference(
            Rotation.lookingAt(point = pos.center, from = eyes)
        ),
    )
}

/**
 * Find the best spot of the upper side of the block
 */
fun canSeeUpperBlockSide(
    eyes: Vec3,
    pos: BlockPos,
    range: Double,
    wallsRange: Double,
): Boolean {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val minX = pos.x.toDouble()
    val y = pos.y + 0.99
    val minZ = pos.z.toDouble()

    for (x in ITERATION_PROPORTIONS_LOOSE) {
        for (z in ITERATION_PROPORTIONS_LOOSE) {
            // skip because of out of range
            val distanceSq = eyes.distanceToSqr(minX + x, y, minZ + z)

            if (distanceSq > rangeSquared) {
                continue
            }

            val vec3 = Vec3(minX + x, y, minZ + z)

            // check if target is visible to eyes
            val visible = player.isFacingBlock(eyes, vec3, pos, Direction.UP)

            // skip because not visible in range
            if (!visible && distanceSq > wallsRangeSquared) {
                continue
            }

            return true
        }
    }

    return false
}

private open class BestRotationTracker(val comparator: Comparator<Rotation>, val ignoreVisibility: Boolean = false) {

    var bestInvisible: RotationWithVector? = null
        private set
    var bestVisible: RotationWithVector? = null
        private set

    fun considerRotation(rotation: RotationWithVector, visible: Boolean = true) {
        if (visible || ignoreVisibility) {
            val isRotationBetter = getIsRotationBetter(base = this.bestVisible, rotation, true)

            if (isRotationBetter) {
                bestVisible = rotation
            }
        } else {
            val isRotationBetter = getIsRotationBetter(base = this.bestInvisible, rotation, false)

            if (isRotationBetter) {
                bestInvisible = rotation
            }
        }
    }

    open fun getIsRotationBetter(
        base: RotationWithVector?,
        newRotation: RotationWithVector,
        visible: Boolean,
    ): Boolean {
        base ?: return true
        return this.comparator.compare(base.rotation, newRotation.rotation) > 0
    }

}

private class PrePlaningTracker(
    comparator: Comparator<Rotation>,
    private val eyes: Vec3,
    private val futureTarget: AABB,
    ignoreVisibility: Boolean = false
) : BestRotationTracker(comparator, ignoreVisibility) {

    override fun getIsRotationBetter(base: RotationWithVector?, newRotation: RotationWithVector,
                                     visible: Boolean): Boolean {
        val currentIntersects = base?.let { futureTarget.isHitByLine(eyes, it.vec) } ?: false
        val intersects = futureTarget.isHitByLine(eyes, newRotation.vec)

        val isBetterWhenVisible = visible && !currentIntersects
        val isBetterWhenInvisible = !visible && !currentIntersects
        val shouldPreferNewRotation = intersects && (isBetterWhenVisible || isBetterWhenInvisible)

        val isWorseWhenVisible = visible && currentIntersects
        val isWorseWhenInvisible = !visible && currentIntersects
        val shouldPreferCurrentRotation = !intersects && (isWorseWhenVisible || isWorseWhenInvisible)

        return when {
            shouldPreferNewRotation -> true
            shouldPreferCurrentRotation -> false
            else -> super.getIsRotationBetter(base, newRotation, visible)
        }
    }

}

fun interface VisibilityPredicate {

    fun isVisible(
        eyesPos: Vec3,
        targetSpot: Vec3,
    ): Boolean

    @JvmRecord
    data class Block(
        val blockPos: BlockPos,
        val side: Direction?,
    ) : VisibilityPredicate {
        override fun isVisible(eyesPos: Vec3, targetSpot: Vec3): Boolean =
            player.isFacingBlock(eyesPos, targetSpot, this.blockPos, this.side)
    }

    data object Outline : VisibilityPredicate {
        override fun isVisible(
            eyesPos: Vec3,
            targetSpot: Vec3
        ): Boolean = world.clip(
            eyesPos,
            targetSpot,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player,
        ).type == HitResult.Type.MISS
    }

    data object Collider : VisibilityPredicate {
        override fun isVisible(
            eyesPos: Vec3,
            targetSpot: Vec3
        ): Boolean = world.clip(
            eyesPos,
            targetSpot,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player,
        ).type == HitResult.Type.MISS
    }
}

@Suppress("detekt:complexity.LongParameterList", "detekt.NestedBlockDepth")
fun raytraceBlockSide(
    side: Direction,
    pos: BlockPos,
    eyes: Vec3,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    collisionContext: CollisionContext,
): RotationWithVector? {
    val outlineShape = pos.state?.getShape(world, pos, collisionContext) ?: return null
    if (outlineShape.isEmpty) {
        return null
    }

    return raytraceBlockSideBoxes(
        side = side,
        boxes = outlineShape.toSortedAabbs(),
        offset = pos,
        eyes = eyes,
        rangeSquared = rangeSquared,
        wallsRangeSquared = wallsRangeSquared,
        rotationPreference = LeastDifferencePreference.leastDifferenceToCurrentRotation(),
        visibilityPredicate = VisibilityPredicate.Outline,
    )
}

/**
 * Samples one block face across every box in a voxel shape and returns the globally best rotation.
 *
 * Visible hits are constrained by [rangeSquared]; non-visible hits may still be accepted within
 * [wallsRangeSquared].
 */
@Suppress("LongParameterList")
internal fun raytraceBlockSideBoxes(
    side: Direction,
    boxes: Iterable<AABB>,
    offset: BlockPos,
    eyes: Vec3,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    rotationPreference: Comparator<Rotation>,
    visibilityPredicate: VisibilityPredicate,
): RotationWithVector? {
    // Compare candidates across the full voxel shape so later sub-boxes can still beat earlier ones.
    val bestRotationTracker = BestRotationTracker(rotationPreference)

    for (box in boxes) {
        val boxWithOffset = box + offset

        for (a in ITERATION_PROPORTIONS) {
            for (b in ITERATION_PROPORTIONS) {
                val spot = boxWithOffset.samplePointOnSide(side, a, b)

                bestRotationTracker.considerSpot(
                    spot,
                    boxWithOffset,
                    eyes,
                    visibilityPredicate,
                    rangeSquared,
                    wallsRangeSquared,
                    spot,
                )
            }
        }
    }

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

/**
 * Samples all boxes that make up a shape and returns the globally best rotation across them.
 */
@Suppress("LongParameterList")
internal fun raytraceBoxes(
    eyes: Vec3,
    boxes: Iterable<AABB>,
    range: Double,
    wallsRange: Double,
    visibilityPredicate: VisibilityPredicate,
    rotationPreference: RotationPreference,
    futureTarget: AABB? = null,
    prioritizeVisible: Boolean = true,
): RotationWithVector? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange
    val bestRotationTracker = futureTarget?.let {
        PrePlaningTracker(rotationPreference, eyes, it, !prioritizeVisible)
    } ?: BestRotationTracker(rotationPreference, !prioritizeVisible)

    for (box in boxes) {
        val preferredSpot = rotationPreference.getPreferredSpotOnBox(box, eyes, range)
        bestRotationTracker.considerSpot(
            preferredSpot,
            box,
            eyes,
            visibilityPredicate,
            rangeSquared,
            wallsRangeSquared,
            preferredSpot,
        )

        val nearestSpot = box.getNearestPoint(eyes)
        bestRotationTracker.considerSpot(
            nearestSpot,
            box,
            eyes,
            visibilityPredicate,
            rangeSquared,
            wallsRangeSquared,
            nearestSpot,
        )

        scanBoxPoints(eyes, box) { spot ->
            bestRotationTracker.considerSpot(
                spot,
                box,
                eyes,
                visibilityPredicate,
                rangeSquared,
                wallsRangeSquared,
                spot,
            )
        }
    }

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

/**
 * Find the best spot of a box to aim at.
 */
@Suppress("detekt:complexity.LongParameterList")
fun raytraceBox(
    eyes: Vec3,
    box: AABB,
    range: Double,
    wallsRange: Double,
    visibilityPredicate: VisibilityPredicate = VisibilityPredicate.Outline,
    rotationPreference: RotationPreference = LeastDifferencePreference.leastDifferenceToCurrentRotation(),
    futureTarget: AABB? = null,
    prioritizeVisible: Boolean = true
): RotationWithVector? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    if (futureTarget == null) {
        val preferredSpot = rotationPreference.getPreferredSpotOnBox(box, eyes, range)
        val preferredSpotOnBox = box.firstHit(from = eyes, to = preferredSpot)

        if (preferredSpotOnBox != null) {
            val preferredSpotDistance = eyes.distanceToSqr(preferredSpotOnBox)
            val visible = visibilityPredicate.isVisible(eyesPos = eyes, targetSpot = preferredSpotOnBox)

            // Fast-path only applies when we do not need to compare the ray against a future target.
            if (isWithinAllowedRange(preferredSpotDistance, visible, rangeSquared, wallsRangeSquared)) {
                return RotationWithVector(
                    Rotation.lookingAt(point = preferredSpotOnBox, from = eyes),
                    preferredSpotOnBox
                )
            }
        }
    }

    return raytraceBoxes(
        eyes = eyes,
        boxes = listOf(box),
        range = range,
        wallsRange = wallsRange,
        visibilityPredicate = visibilityPredicate,
        rotationPreference = rotationPreference,
        futureTarget = futureTarget,
        prioritizeVisible = prioritizeVisible,
    )
}

@Suppress("detekt:complexity.LongParameterList")
private fun BestRotationTracker.considerSpot(
    preferredSpot: Vec3,
    box: AABB,
    eyes: Vec3,
    visibilityPredicate: VisibilityPredicate,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    spot: Vec3,
) {
    // Elongate the line so we have no issues with fp-precision
    val raycastTarget = eyes.fma(2.0, preferredSpot - eyes)

    val spotOnBox = box.firstHit(eyes, raycastTarget) ?: return
    val distSq = eyes.distanceToSqr(spotOnBox)

    val visible = visibilityPredicate.isVisible(eyes, spotOnBox)

    // Visible points must satisfy the normal range; hidden points may still use the wall range fallback.
    if (!isWithinAllowedRange(distSq, visible, rangeSquared, wallsRangeSquared)) {
        return
    }

    val rotation = Rotation.lookingAt(point = spot, from = eyes)

    considerRotation(RotationWithVector(rotation, spot), visible)
}

private fun isWithinAllowedRange(
    distanceSquared: Double,
    visible: Boolean,
    rangeSquared: Double,
    wallsRangeSquared: Double,
): Boolean = distanceSquared < (if (visible) rangeSquared else wallsRangeSquared)

/**
 * Determines if the player is able to see a [AABB].
 *
 * Will return `true` if the player is inside the [box].
 */
fun canSeeBox(eyes: Vec3, box: AABB, range: Double, wallsRange: Double, expectedTarget: BlockPos? = null): Boolean {
    if (box.contains(eyes)) {
        return true
    }

    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    scanBoxPoints(eyes, box) { posInBox ->
        // skip because of out of range
        val distance = eyes.distanceToSqr(posInBox)

        if (distance > rangeSquared) {
            return@scanBoxPoints
        }

        // check if the target is visible to eyes
        val visible =
            if (expectedTarget != null) {
                player.isFacingBlock(eyes, posInBox, expectedTarget)
            } else {
                hasLineOfSight(eyes, posInBox)
            }

        // skip because not visible in range
        if (!visible && distance > wallsRangeSquared) {
            return@scanBoxPoints
        }

        return true
    }

    return false
}

private inline fun scanBoxPoints(
    eyes: Vec3,
    box: AABB,
    fn: (Vec3) -> Unit,
) {
    val isOutsideBox = projectPointsOnBox(eyes, box, maxPoints = 256, fn)

    // We cannot project points on something if we are inside the hitbox
    if (!isOutsideBox) {
        for (x in ITERATION_PROPORTIONS) {
            for (y in ITERATION_PROPORTIONS) {
                for (z in ITERATION_PROPORTIONS) {
                    fn(box.pointAtProportion(x, y, z))
                }
            }
        }
    }
}

/**
 * Find the best spot of the upper block side
 */
@Suppress("LongParameterList")
fun raytraceUpperBlockSide(
    eyes: Vec3,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos,
    rotationPreference: RotationPreference = LeastDifferencePreference.leastDifferenceToCurrentRotation(),
    rotationsNotToMatch: Collection<Rotation>? = null
): RotationWithVector? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val vec3d = Vec3.atLowerCornerOf(expectedTarget)

    val bestRotationTracker = BestRotationTracker(rotationPreference)

    val proportions = rotationsNotToMatch?.let { ITERATION_PROPORTIONS_PRECISE } ?: ITERATION_PROPORTIONS
    for (x in proportions) {
        for (z in proportions) {
            val vec3 = vec3d.add(x, 0.9, z)

            // skip because of out of range
            val distance = eyes.distanceToSqr(vec3)

            if (distance > rangeSquared) {
                continue
            }

            // check if target is visible to eyes
            val visible = player.isFacingBlock(eyes, vec3, expectedTarget, Direction.UP)

            // skip because not visible in range
            if (!visible && distance > wallsRangeSquared) {
                continue
            }

            val rotation = Rotation.lookingAt(point = vec3, from = eyes)
            if (rotationsNotToMatch != null && rotation in rotationsNotToMatch) {
                continue
            }

            bestRotationTracker.considerRotation(RotationWithVector(rotation, vec3), visible)
        }
    }

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

/**
 * Finds the rotation to the closest point on the [expectedTarget], that if possible also points to the crystal that
 * will that could be above the position.
 *
 * [notFacingAway] will make the function not return any rotation to a face that is pointing away from the player.
 *
 * The function also takes [rotationsNotToMatch].
 * Those rotations will be skipped, except if the current rotation equals one of them, then the list is simply ignored,
 * and the current list is returned.
 */
@Suppress("CognitiveComplexMethod", "LongParameterList")
fun findClosestPointOnBlockInLineWithCrystal(
    eyes: Vec3,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos,
    notFacingAway: Boolean,
    rotationsNotToMatch: List<Rotation>? = null
): Pair<RotationWithVector, Direction>? {
    var best: Pair<RotationWithVector, Direction>? = null
    var bestIntersects = false
    var bestDistance = Double.MAX_VALUE

    val predictedCrystal = AABB(
        expectedTarget.x.toDouble() - 0.5,
        expectedTarget.y.toDouble() + 1.0,
        expectedTarget.z.toDouble() - 0.5,
        expectedTarget.x.toDouble() + 1.5,
        expectedTarget.y.toDouble() + 3.0,
        expectedTarget.z.toDouble() + 1.5
    )

    mc.execute {
        ModuleDebug.debugGeometry(
            ModuleCrystalAura,
            "predictedCrystal",
            ModuleDebug.DebuggedBox(predictedCrystal, Color4b.RED.fade(0.4f))
        )
    }

    checkCurrentRotation(range, wallsRange, expectedTarget, predictedCrystal, eyes)?.let { return it }

    val rangeSquared = range.sq()
    val wallsRangeSquared = wallsRange.sq()
    val blockBB = FULL_BOX.move(expectedTarget)

    val vec = expectedTarget.center
    Direction.entries.forEach {
        val vec3d = vec.relative(it, 0.5)

        val coordinate = eyes[it.axis]
        if (notFacingAway && !blockBB.contains(eyes) && when (it.axisDirection) {
            Direction.AxisDirection.NEGATIVE -> coordinate > blockBB.min(it.axis)
            Direction.AxisDirection.POSITIVE -> coordinate < blockBB.max(it.axis)
        }) {
            return@forEach
        }

        range(-0.45..0.45 step 0.05, -0.45..0.45 step 0.05) { x, y ->
            val vec3 = pointOnSide(it, x, y, vec3d)

            val intersects = predictedCrystal.isHitByLine(eyes, vec3)
            if (bestIntersects && !intersects) {
                return@range
            }

            val distance = eyes.distanceToSqr(vec3)

            // skip if out of range or the current best is closer
            if (distance > rangeSquared || bestDistance <= distance && (!intersects || bestIntersects)) {
                return@range
            }

            // skip because not visible in range
            if (distance > wallsRangeSquared && !player.isFacingBlock(eyes, vec3, expectedTarget, it)) {
                return@range
            }

            val rotation = Rotation.lookingAt(point = vec3, from = eyes)
            if (rotationsNotToMatch != null && rotation in rotationsNotToMatch) {
                return@range
            }

            best = RotationWithVector(rotation, vec3) to it
            bestDistance = distance
            bestIntersects = intersects
        }
    }

    return best
}

private fun checkCurrentRotation(
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos,
    predictedCrystal: AABB,
    eyes: Vec3
): Pair<RotationWithVector, Direction>? {
    val currentHit = raytraceBlock(
        max(range, wallsRange),
        RotationManager.serverRotation,
        expectedTarget,
        expectedTarget.stateOrEmpty,
    )

    if (currentHit == null || currentHit.type != HitResult.Type.BLOCK || currentHit.blockPos != expectedTarget) {
        return null
    }

    val pos = currentHit.location
    val intersects = predictedCrystal.isHitByLine(eyes, pos)
    val distance = eyes.distanceToSqr(pos)

    val visibleThroughWalls = distance <= wallsRange.sq() ||
        player.isFacingBlock(eyes, pos, expectedTarget, currentHit.direction)

    if (intersects && distance <= range.sq() && visibleThroughWalls) {
        val rotation = Rotation.lookingAt(point = pos, from = eyes)
        return RotationWithVector(rotation, pos) to currentHit.direction
    }

    return null
}

private fun pointOnSide(side: Direction, x: Double, y: Double, vec: Vec3): Vec3 {
    return when (side) {
        Direction.DOWN, Direction.UP -> vec.add(x, 0.0, y)
        Direction.NORTH, Direction.SOUTH -> vec.add(x, y, 0.0)
        Direction.WEST, Direction.EAST -> vec.add(0.0, x, y)
    }
}
