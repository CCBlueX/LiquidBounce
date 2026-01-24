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
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.edgePoints
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.any
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs


data class PositionFactoryConfiguration(
    val eyePos: Vec3,
    /**
     * Random number [[-1;1]]. Can also be constant.
     */
    val randomNumber: Double
)


sealed class FaceTargetPositionFactory {


    /**
     * Samples a position (relative to [targetPos]).
     * @param face is relative to origin.
     */
    abstract fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3?

    /**
     * Trims a face to be only as wide as the config allows it to be
     */
    protected fun trimFace(face: AlignedFace): AlignedFace {
        val offsets = face.dimensions.scale(0.15)

        var rangeX = face.from.x + offsets.x..face.to.x - offsets.x
        var rangeY = face.from.y + offsets.y..face.to.y - offsets.y
        var rangeZ = face.from.z + offsets.z..face.to.z - offsets.z

        if (rangeX.isEmpty()) {
            rangeX = face.center.x..face.center.x
        }
        if (rangeY.isEmpty()) {
            rangeY = face.center.y..face.center.y
        }
        if (rangeZ.isEmpty()) {
            rangeZ = face.center.z..face.center.z
        }

        val trimmedFace = AlignedFace(
            Vec3(
                face.from.x.coerceIn(rangeX),
                face.from.y.coerceIn(rangeY),
                face.from.z.coerceIn(rangeZ),
            ),
            Vec3(
                face.to.x.coerceIn(rangeX),
                face.to.y.coerceIn(rangeY),
                face.to.z.coerceIn(rangeZ),
            )
        )

        return trimmedFace
    }

}

/**
 * Always targets the point with the nearest rotation angle to the current rotation angle
 */
class NearestRotationTargetPositionFactory(val config: PositionFactoryConfiguration) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3 {
        val trimmedFace = trimFace(face)

        return aimAtNearestPointToRotationLine(targetPos, trimmedFace)
    }

    fun aimAtNearestPointToRotationLine(
        targetPos: BlockPos,
        face: AlignedFace
    ): Vec3 {
        if (Mth.equal(face.area, 0.0)) {
            return face.from
        }

        val currentRotation = RotationManager.serverRotation

        val rotationLine = Line(config.eyePos - targetPos, currentRotation.directionVector)

        val pointOnFace = face.nearestPointTo(rotationLine)

        ModuleScaffold.debugGeometry("targetFace") {
            ModuleDebug.DebuggedBox(
                AABB(
                face.from,
                face.to
            ).move(Vec3.atLowerCornerOf(targetPos)), Color4b(255, 0, 0, 255))
        }

        ModuleScaffold.debugGeometry("targetPoint") {
            ModuleDebug.DebuggedPoint(
                pointOnFace + targetPos,
                Color4b(0, 0, 255, 255),
                size = 0.05
            )
        }

        ModuleScaffold.debugGeometry("daLine") {
            ModuleDebug.DebuggedLine(
                Line(
                    config.eyePos,
                    currentRotation.directionVector
                ), Color4b(0, 0, 255, 255)
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
    val config: PositionFactoryConfiguration,
    private val optimalLine: Line?
) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3 {
        val trimmedFace = trimFace(face).offset(Vec3.atLowerCornerOf(targetPos))

        val targetFace = getTargetFace(player, trimmedFace) ?: trimmedFace

        return NearestRotationTargetPositionFactory(this.config).aimAtNearestPointToRotationLine(
            targetPos,
            targetFace.offset(Vec3.atLowerCornerOf(targetPos).reverse())
        )
    }

    private fun getTargetFace(
        player: LocalPlayer,
        trimmedFace: AlignedFace
    ): AlignedFace? {
        val optimalLine = optimalLine ?: return null

        val nearsetPointToOptimalLine = optimalLine.getNearestPointTo(player.position())
        val directionToOptimalLine = player.position().subtract(nearsetPointToOptimalLine).normalize()

        val optimalLineFromPlayer = Line(config.eyePos, optimalLine.direction)
        val collisionWithFacePlane = trimmedFace.toPlane().intersection(optimalLineFromPlayer) ?: return null

        val b = player.position().add(directionToOptimalLine.scale(2.0))

        val cropBox = AABB(
            collisionWithFacePlane.x,
            player.position().y - 2.0,
            collisionWithFacePlane.z,
            b.x,
            player.position().y + 1.0,
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

object RandomTargetPositionFactory : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3 {
        val trimmedFace = trimFace(face)

        return trimmedFace.randomPointOnFace()
    }
}

object CenterTargetPositionFactory : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3 {
        return face.center
    }
}

private object PositionFactoryDebug : DebuggedOwner

abstract class BaseYawTargetPositionFactory(
    protected val config: PositionFactoryConfiguration,
    private val yawTolerance: Float = 5f
) : FaceTargetPositionFactory() {

    override fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3 {
        ModuleDebug.debugParameter(PositionFactoryDebug, "TargetPos", targetPos)
        val trimmedFace = trimFace(face)

        // If the player is not moving, we can just aim at the nearest point
        return if (!player.input.keyPresses.any) {
            return aimAtNearestPointToRotationLine(targetPos, trimmedFace)
        } else {
            aimAtNearestPointToYaw(targetPos, trimmedFace) ?: aimAtNearestPointToRotationLine(targetPos, trimmedFace)
        }
    }

    protected fun aimAtNearestPointToRotationLine(
        targetPos: BlockPos,
        face: AlignedFace
    ) = NearestRotationTargetPositionFactory(config).aimAtNearestPointToRotationLine(targetPos, face)

    protected fun aimAtNearestPointToYaw(
        targetPos: BlockPos,
        face: AlignedFace
    ): Vec3? {
        if (Mth.equal(face.area, 0.0)) {
            ModuleDebug.debugParameter(PositionFactoryDebug, "FaceArea", face.area)
            ModuleDebug.debugParameter(PositionFactoryDebug, "ReturnedPoint", face.from)
            return face.from
        }

        val yaw = Mth.wrapDegrees(player.yRot)
        val angle = getAngle()
        val highTargetYaw = Math.toRadians(Mth.wrapDegrees(yaw + angle).toDouble()).toFloat()
        val lowTargetYaw = Math.toRadians(Mth.wrapDegrees(yaw - angle).toDouble()).toFloat()

        ModuleDebug.debugParameter(PositionFactoryDebug, "PlayerYaw", yaw)
        ModuleDebug.debugParameter(PositionFactoryDebug, "Angle", angle)
        ModuleDebug.debugParameter(PositionFactoryDebug, "HighTargetYaw", highTargetYaw)
        ModuleDebug.debugParameter(PositionFactoryDebug, "LowTargetYaw", lowTargetYaw)

        val highPlane = NormalizedPlane.fromParams(
            config.eyePos - targetPos,
            Vec3(0.0, 0.0, 1.0).yRot(highTargetYaw),
            Vec3(0.0, 1.0, 0.0)
        )

        val lowPlane = NormalizedPlane.fromParams(
            config.eyePos - targetPos,
            Vec3(0.0, 0.0, 1.0).yRot(lowTargetYaw),
            Vec3(0.0, 1.0, 0.0)
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

        val highClosestPoint = highLineSegment?.let { segment -> findClosestPointToYaw(segment, highTargetYaw) }
        val lowClosestPoint = lowLineSegment?.let { segment -> findClosestPointToYaw(segment, lowTargetYaw) }

        ModuleDebug.debugParameter(PositionFactoryDebug, "HighClosestPoint", highClosestPoint)
        ModuleDebug.debugParameter(PositionFactoryDebug, "LowClosestPoint", lowClosestPoint)

        val highTolerance = highClosestPoint?.let { point -> calculateYawDifference(point, highTargetYaw) }
            ?: Float.MAX_VALUE
        val lowTolerance = lowClosestPoint?.let { point -> calculateYawDifference(point, lowTargetYaw) }
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

    private fun findClosestPointToYaw(lineSegment: LineSegment, targetYaw: Float): Vec3 {
        val start = lineSegment.endPoints.first
        val end = lineSegment.endPoints.second
        val direction = end.subtract(start).normalize()

        val startYaw = calculateYaw(start)
        val endYaw = calculateYaw(end)
        val yawDiff = Mth.wrapDegrees(endYaw - startYaw)
        val targetYawDiff = Mth.wrapDegrees(targetYaw - startYaw)
        val t = if (yawDiff != 0f) targetYawDiff / yawDiff else 0f
        return start.add(direction.scale(t.toDouble().coerceIn(0.0, 1.0)))
    }

    private fun calculateYaw(point: Vec3): Float {
        val dx = point.x - config.eyePos.x
        val dz = point.z - config.eyePos.z
        return Mth.atan2(dz, dx).toFloat()
    }

    private fun calculateYawDifference(point: Vec3, targetYaw: Float): Float {
        val pointYaw = calculateYaw(point)
        return abs(Mth.wrapDegrees(pointYaw - targetYaw))
    }

    protected abstract fun getAngle(): Float
}

class ReverseYawTargetPositionFactory(config: PositionFactoryConfiguration) : BaseYawTargetPositionFactory(config) {
    override fun getAngle() = 180f // 180 degrees
}

class DiagonalYawTargetPositionFactory(config: PositionFactoryConfiguration) : BaseYawTargetPositionFactory(config) {
    override fun getAngle() = 75f // 75 degrees
}

class AngleYawTargetPositionFactory(config: PositionFactoryConfiguration) : BaseYawTargetPositionFactory(config) {
    override fun getAngle() = 45f // 45 degrees
}

class EdgePointTargetPositionFactory(
    val config: PositionFactoryConfiguration,
) : FaceTargetPositionFactory() {

    override fun producePositionOnFace(face: AlignedFace, targetPos: BlockPos): Vec3 {
        val trimmedFace = trimFace(face)

        // If the player is not moving, we can just aim at the nearest point
        return if (!player.input.keyPresses.any) {
            return aimAtNearestPointToRotationLine(targetPos, trimmedFace)
        } else {
            aimAtFurthestPointToPlayerPosition(targetPos, trimmedFace)
                ?: aimAtNearestPointToRotationLine(targetPos, trimmedFace)
        }
    }

    private fun aimAtNearestPointToRotationLine(
        targetPos: BlockPos,
        face: AlignedFace
    ) = NearestRotationTargetPositionFactory(config).aimAtNearestPointToRotationLine(targetPos, face)

    private fun aimAtFurthestPointToPlayerPosition(
        targetPos: BlockPos,
        face: AlignedFace
    ): Vec3? {
        val box = AABB(face.from, face.to)
        val edge = box.edgePoints.maxByOrNull { edge ->
            edge.distanceToSqr(player.position() - player.blockPosition())
        } ?: return null

        ModuleScaffold.debugGeometry("Face") {
            ModuleDebug.DebuggedBox(
                AABB(
                    face.from,
                    face.to
                ).move(Vec3.atLowerCornerOf(targetPos)), Color4b(255, 0, 0, 255)
            )
        }

        ModuleScaffold.debugGeometry("Edge") {
            ModuleDebug.DebuggedPoint(
                edge + targetPos,
                Color4b(0, 0, 255, 255),
                size = 0.05
            )
        }

        return edge
    }

}
