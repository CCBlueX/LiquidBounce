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
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.preference.RotationPreference
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.kotlin.range
import net.ccbluex.liquidbounce.utils.math.firstHit
import net.ccbluex.liquidbounce.utils.math.isHitByLine
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.raytracing.facingBlock
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import kotlin.math.max

private val ITERATION_PROPORTIONS = 0.05..0.95 step 0.1
private val ITERATION_PROPORTIONS_PRECISE = 0.05..0.95 step 0.05

fun raytraceBlockRotation(
    eyes: Vec3,
    pos: BlockPos,
    state: BlockState,
    range: Double,
    wallsRange: Double,
): RotationWithVector? {
    val offset = Vec3.atLowerCornerOf(pos)
    val shape = state.getShape(world, pos, CollisionContext.of(player))

    for (box in shape.toAabbs().sortedByDescending { it.size }) {
        return raytraceBox(
            eyes,
            box.move(offset),
            range,
            wallsRange,
            visibilityPredicate = BlockVisibilityPredicate(pos),
            rotationPreference = LeastDifferencePreference(Rotation.lookingAt(point = pos.center, from = eyes))
        ) ?: continue
    }

    return null
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

    val rangeXZ = doubleArrayOf(0.1, 0.5, 0.9)

    for (x in rangeXZ) {
        for (z in rangeXZ) {
            val vec3 = Vec3(minX + x, y, minZ + z)

            // skip because of out of range
            val distance = eyes.distanceToSqr(vec3)

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

/**
 * This should not be reused, as it caches the current player eye position!
 */
private class PrePlaningTracker(
    comparator: Comparator<Rotation>,
    private val futureTarget: AABB,
    ignoreVisibility: Boolean = false
) : BestRotationTracker(comparator, ignoreVisibility) {

    private val eyes = player.eyePosition
    private val bestVisibleIntersects = false
    private val bestInvisibleIntersects = false

    override fun getIsRotationBetter(base: RotationWithVector?, newRotation: RotationWithVector,
                                     visible: Boolean): Boolean {
        val intersects = futureTarget.isHitByLine(eyes, newRotation.vec)

        val isBetterWhenVisible = visible && !bestVisibleIntersects
        val isBetterWhenInvisible = !visible && !bestInvisibleIntersects
        val shouldPreferNewRotation = intersects && (isBetterWhenVisible || isBetterWhenInvisible)

        val isWorseWhenVisible = visible && bestVisibleIntersects
        val isWorseWhenInvisible = !visible && bestInvisibleIntersects
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
}

class BlockVisibilityPredicate(private val expectedTarget: BlockPos) : VisibilityPredicate {
    override fun isVisible(
        eyesPos: Vec3,
        targetSpot: Vec3,
    ): Boolean {
        return facingBlock(eyesPos, targetSpot, this.expectedTarget)
    }
}

object BoxVisibilityPredicate : VisibilityPredicate, DebuggedOwner {
    override fun isVisible(
        eyesPos: Vec3,
        targetSpot: Vec3,
    ): Boolean {
//        debugGeometry("TargetSpot") { ModuleDebug.DebuggedPoint(targetSpot, color = Color4b.LIQUID_BOUNCE) }

        return hasLineOfSight(eyesPos, targetSpot)
    }
}

private fun pointOnBlockSide(
    side: Direction,
    a: Double,
    b: Double,
    box: AABB,
): Vec3 {
    val spot = when (side) {
        Direction.DOWN -> Vec3(a, 0.0, b)
        Direction.UP -> Vec3(a, 1.0, b)
        Direction.NORTH -> Vec3(a, b, 0.0)
        Direction.SOUTH -> Vec3(a, b, 1.0)
        Direction.WEST -> Vec3(0.0, a, b)
        Direction.EAST -> Vec3(1.0, a, b)
    }

    return Vec3(spot.x * box.xsize, spot.y * box.ysize, spot.z * box.zsize)
}

@Suppress("detekt:complexity.LongParameterList", "detekt.NestedBlockDepth")
fun raytraceBlockSide(
    side: Direction,
    pos: BlockPos,
    eyes: Vec3,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    shapeContext: CollisionContext
): RotationWithVector? {
    pos.getState()?.getShape(world, pos, shapeContext)?.let { shape ->
        val sortedShapes = shape.toAabbs().sortedByDescending { it.size }
        for (boxShape in sortedShapes) {
            val box = boxShape.move(pos)
            val visibilityPredicate = BoxVisibilityPredicate

            val bestRotationTracker = BestRotationTracker(LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION)

//            val nearestSpotOnSide = getNearestPointOnSide(eyes, box, side)

//            considerSpot(
//                nearestSpotOnSide,
//                box,
//                eyes,
//                visibilityPredicate,
//                rangeSquared,
//                wallsRangeSquared,
//                nearestSpotOnSide,
//                bestRotationTracker,
//            )
//            chat(side.toString())


            range(ITERATION_PROPORTIONS, ITERATION_PROPORTIONS) { a, b ->
                val spot = pointOnBlockSide(side, a, b, box) + pos.toVec3d()

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

            bestRotationTracker.bestVisible?.let {
                return it
            }

            bestRotationTracker.bestInvisible?.let {
                // if we found a point we can place a block on, on this face there is no need to look at the others
                return it
            }

        }
    }
    return null
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
    visibilityPredicate: VisibilityPredicate = BoxVisibilityPredicate,
    rotationPreference: RotationPreference = LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION,
    futureTarget: AABB? = null,
    prioritizeVisible: Boolean = true
): RotationWithVector? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val preferredSpot = rotationPreference.getPreferredSpotOnBox(box, eyes, range)
    val preferredSpotOnBox = box.firstHit(from = eyes, to = preferredSpot)

    if (preferredSpotOnBox != null) {
        val preferredSpotDistance = eyes.distanceToSqr(preferredSpotOnBox)

        // If a pattern-generated spot is visible or its distance is within wall range, then return right here.
        // No need to enter the loop when we already have a result.
        val validCauseBelowWallsRange = preferredSpotDistance < wallsRangeSquared

        val validCauseVisible = visibilityPredicate.isVisible(eyesPos = eyes, targetSpot = preferredSpotOnBox)

        if (validCauseBelowWallsRange || validCauseVisible && preferredSpotDistance < rangeSquared) {
            return RotationWithVector(Rotation.lookingAt(point = preferredSpot, from = eyes), preferredSpot)
        }
    }

    val bestRotationTracker = futureTarget?.let {
        PrePlaningTracker(rotationPreference, it, !prioritizeVisible)
    } ?: BestRotationTracker(rotationPreference, !prioritizeVisible)

    // There are some spots that loops cannot detect, therefore this is used
    // since it finds the nearest spot within the requested range.
    val nearestSpot = box.getNearestPoint(eyes)

    bestRotationTracker.considerSpot(
        preferredSpot,
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

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

@Suppress("detekt:complexity.LongParameterList")
private fun BestRotationTracker.considerSpot(
    preferredSpot: Vec3,
    box: AABB,
    eyes: Vec3,
    visibilityPredicate: VisibilityPredicate,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    spot: Vec3
) {
    // Elongate the line so we have no issues with fp-precision
    val raycastTarget = (preferredSpot - eyes) * 2.0 + eyes

    val spotOnBox = box.firstHit(eyes, raycastTarget) ?: return
    val distSq = eyes.distanceToSqr(spotOnBox)

    val visible = visibilityPredicate.isVisible(eyes, spotOnBox)

    // Is either spot visible or distance within wall range?
    if ((!visible || distSq >= rangeSquared) && distSq >= wallsRangeSquared) {
        return
    }

    val rotation = Rotation.lookingAt(point = spot, from = eyes)

    considerRotation(RotationWithVector(rotation, spot), visible)
}

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
                facingBlock(eyes, posInBox, expectedTarget)
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
        range(ITERATION_PROPORTIONS, ITERATION_PROPORTIONS, ITERATION_PROPORTIONS) { x, y, z ->
            val vec3 = Vec3(
                box.minX + box.xsize * x,
                box.minY + box.ysize * y,
                box.minZ + box.zsize * z,
            )

            fn(vec3)
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
    rotationPreference: RotationPreference = LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION,
    rotationsNotToMatch: Collection<Rotation>? = null
): RotationWithVector? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val vec3d = Vec3.atLowerCornerOf(expectedTarget)

    val bestRotationTracker = BestRotationTracker(rotationPreference)

    val proportions = rotationsNotToMatch?.let { ITERATION_PROPORTIONS_PRECISE } ?: ITERATION_PROPORTIONS
    range(proportions, proportions) { x, z ->
        val vec3 = vec3d.add(x, 0.9, z)

        // skip because of out of range
        val distance = eyes.distanceToSqr(vec3)

        if (distance > rangeSquared) {
            return@range
        }

        // check if target is visible to eyes
        val visible = facingBlock(eyes, vec3, expectedTarget, Direction.UP)

        // skip because not visible in range
        if (!visible && distance > wallsRangeSquared) {
            return@range
        }

        val rotation = Rotation.lookingAt(point = vec3, from = eyes)
        if (rotationsNotToMatch != null && rotation in rotationsNotToMatch) {
            return@range
        }

        bestRotationTracker.considerRotation(RotationWithVector(rotation, vec3), visible)
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

        val coordinate = it.axis.choose(eyes.x, eyes.y, eyes.z)
        if (notFacingAway && !blockBB.contains(eyes) && when (it) {
            Direction.NORTH, Direction.WEST, Direction.DOWN -> coordinate > blockBB.min(it.axis)
            Direction.SOUTH, Direction.EAST, Direction.UP -> coordinate < blockBB.max(it.axis)
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
            if (distance > wallsRangeSquared && !facingBlock(eyes, vec3, expectedTarget, it)) {
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
        expectedTarget.getState()!!
    )

    if (currentHit == null || currentHit.type != HitResult.Type.BLOCK || currentHit.blockPos != expectedTarget) {
        return null
    }

    val pos = currentHit.location
    val intersects = predictedCrystal.isHitByLine(eyes, pos)
    val distance = eyes.distanceToSqr(pos)

    val visibleThroughWalls = distance <= wallsRange.sq() ||
            facingBlock(eyes, pos, expectedTarget, currentHit.direction)

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
