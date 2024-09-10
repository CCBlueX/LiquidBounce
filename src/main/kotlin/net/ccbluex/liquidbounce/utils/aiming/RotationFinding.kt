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
@file:Suppress("Detekt.TooManyFunctions")

package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toVec3d
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
    val shape = state.getOutlineShape(world, pos, ShapeContext.of(player))

    for (box in shape.boundingBoxes.sortedBy { -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ) }) {
        return raytraceBox(
            eyes,
            box.offset(offset),
            range,
            wallsRange,
            visibilityPredicate = BlockVisibilityPredicate(pos),
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
            val vec3 = Vec3d(minX + x, y, minZ + z)

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

fun ClosedRange<Double>.shrinkBy(value: Double): ClosedFloatingPointRange<Double> {
    val dif = endInclusive - start
    if (dif < value) return (dif / 2)..(dif / 2)
    return (start + value)..(endInclusive - value)
}

fun pointOnBlockSide(
    side: Direction,
    a: Double,
    b: Double,
    box: Box,
): Vec3d {
    val spot = when (side) {
        Direction.DOWN -> Vec3d(a, 0.0, b)
        Direction.UP -> Vec3d(a, 1.0, b)
        Direction.NORTH -> Vec3d(a, b, 0.0)
        Direction.SOUTH -> Vec3d(a, b, 1.0)
        Direction.WEST -> Vec3d(0.0, a, b)
        Direction.EAST -> Vec3d(1.0, a, b)
    }

    return Vec3d(spot.x * box.lengthX, spot.y * box.lengthY, spot.z * box.lengthZ)
}

@Suppress("detekt:complexity.LongParameterList", "detekt.NestedBlockDepth")
fun raytraceBlockSide(
    side: Direction,
    pos: BlockPos,
    eyes: Vec3d,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    shapeContext: ShapeContext
): VecRotation? {
    pos.getState()?.getOutlineShape(world, pos, shapeContext)?.let { shape ->
        val sortedShapes = shape.boundingBoxes.sortedBy {
            -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ)
        }
        for (boxShape in sortedShapes) {
            val box = boxShape.offset(pos)
            val visibilityPredicate = BoxVisibilityPredicate(box)

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


            for (a in 0.05..0.95 step 0.1) {
                for (b in 0.05..0.95 step 0.1) {
                    val spot = pointOnBlockSide(side, a, b, box) + pos.toVec3d()

                    ModuleDebug.debugGeometry(ModuleAutoFarm, "deddee", ModuleDebug.DebuggedPoint(spot, Color4b.RED))
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

val sidesToCheck = arrayOf(
    Direction.DOWN, // first down because it is the most likely that you can place a block on top a the one below
    Direction.NORTH, // then the sides
    Direction.SOUTH,
    Direction.WEST,
    Direction.EAST,
    Direction.UP // last and least UP because it is the most unlikely place to be able to place a block on
)

fun raytracePlaceBlock(
    eyes: Vec3d,
    pos: BlockPos,
    range: Double,
    wallsRange: Double,
): VecRotation? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange
    val shapeContext = ShapeContext.of(player)
    val eyeOffsetFromBlock = pos.toCenterPos() - eyes
    for (side in sidesToCheck) {
        // The difference between the eyes and the center of the face.
        // Using 0.45 instead of 0.5 to avoid too narrow angles
        val eyeOffsetFromFace = eyeOffsetFromBlock.offset(side, 0.45)
        val dotProduct = eyeOffsetFromFace.dotProduct(side.vector.toVec3d())
        if (dotProduct <= 0) continue // We are behind the face

        val newPos = pos.offset(side)

        raytraceBlockSide(side.opposite, newPos, eyes, rangeSquared, wallsRangeSquared, shapeContext)?.let {
            return it
        }
    }

    return null
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
                val spot = Vec3d(
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
    // Elongate the line so we have no issues with fp-precision
    val raycastTarget = (preferredSpot - eyes) * 2.0 + eyes
    val spotOnBox = box.raycast(eyes, raycastTarget).getOrNull() ?: return
    val distance = eyes.squaredDistanceTo(spotOnBox)

    val visible = visibilityPredicate.isVisible(eyes, raycastTarget)

    // Is either spot visible or distance within wall range?
    if ((!visible || distance >= rangeSquared) && distance >= wallsRangeSquared) {
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
                val vec3 = Vec3d(
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
