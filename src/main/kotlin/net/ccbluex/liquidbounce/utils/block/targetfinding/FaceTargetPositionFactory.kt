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

import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.utils.VisibilityPredicate
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.math.vertices
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.unaryMinus
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

private object PositionFactoryDebug : DebuggedOwner

/**
 * Trims a face to be only as wide as the config allows it to be
 *
 * @param scale fraction of the face's width removed on each side
 */
fun trimFace(face: AlignedFace, scale: Double = 0.15): AlignedFace {
    val offsets = face.dimensions.scale(scale)

    val lowX = face.from.x + offsets.x
    val highX = face.to.x - offsets.x
    val lowY = face.from.y + offsets.y
    val highY = face.to.y - offsets.y
    val lowZ = face.from.z + offsets.z
    val highZ = face.to.z - offsets.z

    // Collapse to the center when the interval inverts (scale >= 0.5 or a zero-width axis)
    val fromX = if (lowX > highX) face.center.x else lowX
    val toX = if (lowX > highX) face.center.x else highX
    val fromY = if (lowY > highY) face.center.y else lowY
    val toY = if (lowY > highY) face.center.y else highY
    val fromZ = if (lowZ > highZ) face.center.z else lowZ
    val toZ = if (lowZ > highZ) face.center.z else highZ

    return AlignedFace(Vec3(fromX, fromY, fromZ), Vec3(toX, toY, toZ))
}

sealed interface FaceTargetPositionFactory {

    /**
     * Samples a position (relative to [targetPos]).
     *
     * @param face is relative to origin.
     * @param playerLocation the player's state on placement. All geometry must be derived from it, never from the
     * live player, so that the aim stays consistent with [PlayerLocationOnPlacement.eyePos].
     */
    fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement,
    ): Vec3?

}

/**
 * Always targets the point with the nearest rotation angle to the current rotation angle
 */
object NearestRotationTargetPositionFactory : FaceTargetPositionFactory {

    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 = aimAtNearestPointToRotationLine(targetPos, trimFace(face), playerLocation)

    fun aimAtNearestPointToRotationLine(
        targetPos: BlockPos,
        face: AlignedFace,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 {
        if (Mth.equal(face.area, 0.0)) {
            return face.from
        }

        val rotation = playerLocation.rotation
        val rotationLine = Line(playerLocation.eyePos - targetPos, rotation.directionVector)

        val pointOnFace = face.nearestPointTo(rotationLine)

        PositionFactoryDebug.debugGeometry("targetFace") {
            ModuleDebug.DebuggedBox(face.asBox().move(targetPos), Color4b.RED)
        }

        PositionFactoryDebug.debugGeometry("targetPoint") {
            ModuleDebug.DebuggedPoint(
                pointOnFace + targetPos,
                Color4b.BLUE,
                size = 0.05
            )
        }

        PositionFactoryDebug.debugGeometry("daLine") {
            ModuleDebug.DebuggedLine(
                Line(
                    playerLocation.eyePos,
                    rotation.directionVector
                ), Color4b.BLUE
            )
        }

        return pointOnFace
    }
}

/**
 * Always targets the point with the nearest rotation angle to the current rotation angle.
 * If you have questions, you have to ask @superblaubeere27 because I am too stupid to explain this without a picture.
 */
class StabilizedRotationTargetPositionFactory(
    private val optimalLine: Line?
) : FaceTargetPositionFactory {
    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 {
        val trimmedFace = trimFace(face).offset(targetPos)

        val targetFace = getTargetFace(playerLocation, trimmedFace) ?: trimmedFace

        return NearestRotationTargetPositionFactory.aimAtNearestPointToRotationLine(
            targetPos,
            targetFace.offset(-targetPos),
            playerLocation
        )
    }

    private fun getTargetFace(
        playerLocation: PlayerLocationOnPlacement,
        trimmedFace: AlignedFace
    ): AlignedFace? {
        val optimalLine = optimalLine ?: return null

        val playerPosition = playerLocation.position
        val nearestPointToOptimalLine = optimalLine.getNearestPointTo(playerPosition)
        val directionToOptimalLine = playerPosition.subtract(nearestPointToOptimalLine).normalize()

        val optimalLineFromPlayer = Line(playerLocation.eyePos, optimalLine.direction)
        val collisionWithFacePlane = trimmedFace.toPlane().intersection(optimalLineFromPlayer) ?: return null

        val b = playerPosition.add(directionToOptimalLine.scale(2.0))

        val cropBox = AABB(
            collisionWithFacePlane.x,
            playerPosition.y - 2.0,
            collisionWithFacePlane.z,
            b.x,
            playerPosition.y + 1.0,
            b.z,
        )

        val clampedFace = trimmedFace.clamp(cropBox)

        // Not much left of the area? Then don't try to sample a point on the face
        if (clampedFace.area < 0.0001) {
            return null
        }

        return clampedFace
    }
}

object RandomTargetPositionFactory : FaceTargetPositionFactory {
    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 = trimFace(face).randomPointOnFace()
}

object CenterTargetPositionFactory : FaceTargetPositionFactory {
    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 = face.center
}

/**
 * Like [CenterTargetPositionFactory], but prefers an unobstructed point on the face.
 *
 * The face center can be occluded by the block itself when only a small part of the face is visible
 * from the player's eyes. In that case the click point (and therefore the rotation / reach checks)
 * would be wrong, so fall back to sampling the face for the first visible point. If nothing is
 * visible, the center is kept so wall-range placements keep working.
 */
object ClickableCenterTargetPositionFactory : FaceTargetPositionFactory {
    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 {
        val center = face.center
        return if (VisibilityPredicate.Outline.isVisible(playerLocation.eyePos, center + targetPos)) {
            center
        } else {
            findVisiblePointOnFace(face, targetPos, playerLocation.eyePos) ?: center
        }
    }

    private val FACE_SAMPLE_PROPORTIONS = doubleArrayOf(0.05, 0.1, 0.175, 0.3, 0.5, 0.7, 0.825, 0.9, 0.95)

    private fun findVisiblePointOnFace(face: AlignedFace, targetPos: BlockPos, eyePos: Vec3): Vec3? {
        for (a in FACE_SAMPLE_PROPORTIONS) {
            for (b in FACE_SAMPLE_PROPORTIONS) {
                val point = face.samplePointOnFace(a, b)
                if (VisibilityPredicate.Outline.isVisible(eyePos, point + targetPos)) {
                    return point
                }
            }
        }
        return null
    }

}

abstract class BaseYawTargetPositionFactory : FaceTargetPositionFactory {

    private val yawTolerance = 5f

    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 {
        ModuleDebug.debugParameter(PositionFactoryDebug, "TargetPos", targetPos)
        val trimmedFace = trimFace(face)

        // If the player is not moving, we can just aim at the nearest point
        return if (!playerLocation.movingHorizontally) {
            NearestRotationTargetPositionFactory.aimAtNearestPointToRotationLine(targetPos, trimmedFace, playerLocation)
        } else {
            aimAtNearestPointToYaw(targetPos, trimmedFace, playerLocation)
                ?: NearestRotationTargetPositionFactory.aimAtNearestPointToRotationLine(
                    targetPos, trimmedFace, playerLocation)
        }
    }

    protected fun aimAtNearestPointToYaw(
        targetPos: BlockPos,
        face: AlignedFace,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3? {
        if (Mth.equal(face.area, 0.0)) {
            ModuleDebug.debugParameter(PositionFactoryDebug, "FaceArea", face.area)
            ModuleDebug.debugParameter(PositionFactoryDebug, "ReturnedPoint", face.from)
            return face.from
        }

        // The face and its intersection segments are block-local. The eye has to be in that same space, or the yaw
        // of (local point - world eye) points at the origin instead of at the face.
        val localEye = playerLocation.eyePos - targetPos
        val yaw = Mth.wrapDegrees(playerLocation.rotation.yaw)
        val angle = getAngle()
        val highTargetYaw = Mth.wrapDegrees(yaw + angle)
        val lowTargetYaw = Mth.wrapDegrees(yaw - angle)

        ModuleDebug.debugParameter(PositionFactoryDebug, "PlayerYaw", yaw)
        ModuleDebug.debugParameter(PositionFactoryDebug, "Angle", angle)
        ModuleDebug.debugParameter(PositionFactoryDebug, "HighTargetYaw", highTargetYaw)
        ModuleDebug.debugParameter(PositionFactoryDebug, "LowTargetYaw", lowTargetYaw)

        val highPlane = NormalizedPlane.fromParams(
            localEye,
            Vec3.Z_AXIS.yRot(highTargetYaw.toRadians()),
            Vec3.Y_AXIS
        )

        val lowPlane = NormalizedPlane.fromParams(
            localEye,
            Vec3.Z_AXIS.yRot(lowTargetYaw.toRadians()),
            Vec3.Y_AXIS
        )

        val highIntersectLine = face.toPlane().intersection(highPlane)
        val lowIntersectLine = face.toPlane().intersection(lowPlane)

        val highLineSegment = runCatching { highIntersectLine?.let { line -> face.coerceInFace(line) } }.getOrNull()
        val lowLineSegment = runCatching { lowIntersectLine?.let { line -> face.coerceInFace(line) } }.getOrNull()

        ModuleDebug.debugParameter(PositionFactoryDebug, "HighLineSegment", highLineSegment)
        ModuleDebug.debugParameter(PositionFactoryDebug, "LowLineSegment", lowLineSegment)

        if (highLineSegment == null && lowLineSegment == null) {
            return null
        }

        val highClosestPoint = highLineSegment?.let { segment ->
            findClosestPointToYaw(segment, highTargetYaw, localEye)
        }
        val lowClosestPoint = lowLineSegment?.let { segment ->
            findClosestPointToYaw(segment, lowTargetYaw, localEye)
        }

        ModuleDebug.debugParameter(PositionFactoryDebug, "HighClosestPoint", highClosestPoint)
        ModuleDebug.debugParameter(PositionFactoryDebug, "LowClosestPoint", lowClosestPoint)

        val highTolerance = highClosestPoint?.let { point -> calculateYawDifference(point, highTargetYaw, localEye) }
            ?: Float.MAX_VALUE
        val lowTolerance = lowClosestPoint?.let { point -> calculateYawDifference(point, lowTargetYaw, localEye) }
            ?: Float.MAX_VALUE

        ModuleDebug.debugParameter(PositionFactoryDebug, "HighTolerance", highTolerance)
        ModuleDebug.debugParameter(PositionFactoryDebug, "LowTolerance", lowTolerance)

        val result = when {
            highTolerance <= yawTolerance && lowTolerance <= yawTolerance -> {
                if (highTolerance < lowTolerance) highClosestPoint else lowClosestPoint
            }
            highTolerance <= yawTolerance -> highClosestPoint
            lowTolerance <= yawTolerance -> lowClosestPoint
            else -> null
        }

        ModuleDebug.debugParameter(PositionFactoryDebug, "ReturnedPoint", result)
        return result
    }

    private fun findClosestPointToYaw(lineSegment: LineSegment, targetYaw: Float, eyePos: Vec3): Vec3 {
        val start = lineSegment.start
        val end = lineSegment.end
        val segmentDelta = end.subtract(start)

        val startYaw = calculateYaw(start, eyePos)
        val endYaw = calculateYaw(end, eyePos)
        val yawDiff = Mth.wrapDegrees(endYaw - startYaw)
        val targetYawDiff = Mth.wrapDegrees(targetYaw - startYaw)
        val t = if (yawDiff != 0f) targetYawDiff / yawDiff else 0f
        return start.add(segmentDelta.scale(t.toDouble().coerceIn(0.0, 1.0)))
    }

    private fun calculateYaw(point: Vec3, eyePos: Vec3): Float {
        return point.subtract(eyePos).yaw
    }

    private fun calculateYawDifference(point: Vec3, targetYaw: Float, eyePos: Vec3): Float {
        val pointYaw = calculateYaw(point, eyePos)
        return abs(Mth.wrapDegrees(pointYaw - targetYaw))
    }

    protected abstract fun getAngle(): Float
}

object ReverseYawTargetPositionFactory : BaseYawTargetPositionFactory() {
    override fun getAngle() = 180f // 180 degrees
}

object DiagonalYawTargetPositionFactory : BaseYawTargetPositionFactory() {
    override fun getAngle() = 75f // 75 degrees
}

object AngleYawTargetPositionFactory : BaseYawTargetPositionFactory() {
    override fun getAngle() = 45f // 45 degrees
}

object EdgePointTargetPositionFactory : FaceTargetPositionFactory {

    override fun producePositionOnFace(
        face: AlignedFace,
        targetPos: BlockPos,
        playerLocation: PlayerLocationOnPlacement
    ): Vec3 {
        val trimmedFace = trimFace(face)

        // If the player is not moving, we can just aim at the nearest point
        return if (!playerLocation.movingHorizontally) {
            NearestRotationTargetPositionFactory.aimAtNearestPointToRotationLine(targetPos, trimmedFace, playerLocation)
        } else {
            aimAtFurthestPointToPlayerPosition(targetPos, trimmedFace, playerLocation.position)
                ?: NearestRotationTargetPositionFactory.aimAtNearestPointToRotationLine(
                    targetPos, trimmedFace, playerLocation)
        }
    }

    private fun aimAtFurthestPointToPlayerPosition(
        targetPos: BlockPos,
        face: AlignedFace,
        playerPosition: Vec3
    ): Vec3? {
        val box = face.asBox()
        val playerPositionRelativeToTarget = playerPosition - targetPos
        val edge = box.vertices.maxByOrNull { edge ->
            edge.distanceToSqr(playerPositionRelativeToTarget)
        } ?: return null

        PositionFactoryDebug.debugGeometry("Face") {
            ModuleDebug.DebuggedBox(box.move(targetPos), Color4b.RED)
        }

        PositionFactoryDebug.debugGeometry("Edge") {
            ModuleDebug.DebuggedPoint(
                edge + targetPos,
                Color4b.BLUE,
                size = 0.05
            )
        }

        return edge
    }

}
