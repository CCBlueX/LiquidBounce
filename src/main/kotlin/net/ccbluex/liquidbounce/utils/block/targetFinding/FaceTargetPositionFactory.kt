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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.geometry.Face
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d


data class PositionFactoryConfiguration(
    val eyePos: Vec3d,
    /**
     * Random number [[-1;1]]. Can also be constant.
     */
    val randomNumber: Double
)


abstract class FaceTargetPositionFactory {


    /**
     * Samples a position (relative to [targetPos]).
     * @param face is relative to origin.
     */
    abstract fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d

    protected fun getFaceRelativeToTargetPosition(face: Face, targetPos: BlockPos): Face {
        return face.offset(Vec3d.of(targetPos).negate())
    }

    /**
     * Trims a face to be only as wide as the config allows it to be
     */
    protected fun trimFace(face: Face): Face {
        val offsets = face.dimensions.multiply(0.15)

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

        val trimmedFace = Face(
            Vec3d(
                face.from.x.coerceIn(rangeX),
                face.from.y.coerceIn(rangeY),
                face.from.z.coerceIn(rangeZ),
            ),
            Vec3d(
                face.to.x.coerceIn(rangeX),
                face.to.y.coerceIn(rangeY),
                face.to.z.coerceIn(rangeZ),
            )
        )

        return trimmedFace
    }

    protected fun getPositionsOnFace(face: Face, step: Double): MutableList<Vec3d> {
        // Collects all possible rotations
        val possiblePositions = mutableListOf<Vec3d>()

        val from = face.from
        val to = face.to

        for (x in from.x..to.x step step) {
            for (y in from.y..to.y step step) {
                for (z in from.z..to.z step step) {
                    val vec3 = Vec3d(x, y, z)

                    possiblePositions.add(vec3)
                }
            }
        }
        return possiblePositions
    }


}

/**
 * Always targets the point with the nearest rotation angle to the current rotation angle
 */
class NearestRotationTargetPositionFactory(val config: PositionFactoryConfiguration) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = trimFace(face)

        return aimAtNearestPointToRotationLine(targetPos, trimmedFace)
    }

    fun aimAtNearestPointToRotationLine(
        targetPos: BlockPos,
        face: Face
    ): Vec3d {
        if (MathHelper.approximatelyEquals(face.area, 0.0))
            return face.from

        val currentRotation = RotationManager.serverRotation

        val rotationLine = Line(config.eyePos.subtract(Vec3d.of(targetPos)), currentRotation.rotationVec)

        val pointOnFace = face.nearestPointTo(rotationLine)

        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "targetFace",
            ModuleDebug.DebuggedBox(Box(
                face.from,
                face.to
            ).offset(Vec3d.of(targetPos)), Color4b(255, 0, 0, 255))
        )
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "targetPoint",
            ModuleDebug.DebuggedPoint(
                pointOnFace.add(Vec3d.of(targetPos)),
                Color4b(0, 0, 255, 255),
                size = 0.05
            )
        )
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "daLine",
            ModuleDebug.DebuggedLine(Line(
                config.eyePos,
                currentRotation.rotationVec
            ), Color4b(0, 0, 255, 255))
        )

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
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = trimFace(face).offset(Vec3d.of(targetPos))

        val targetFace = getTargetFace(player, trimmedFace, face) ?: trimmedFace

        return NearestRotationTargetPositionFactory(this.config).aimAtNearestPointToRotationLine(
            targetPos,
            targetFace.offset(Vec3d.of(targetPos).negate())
        )
    }

    private fun getTargetFace(
        player: ClientPlayerEntity,
        trimmedFace: Face,
        face: Face
    ): Face? {
        val optimalLine = optimalLine ?: return null

        val nearsetPointToOptimalLine = optimalLine.getNearestPointTo(player.pos)
        val directionToOptimalLine = player.pos.subtract(nearsetPointToOptimalLine).normalize()

        val optimalLineFromPlayer = Line(config.eyePos, optimalLine.direction)
        val collisionWithFacePlane = trimmedFace.toPlane().intersection(optimalLineFromPlayer) ?: return null

        val b = player.pos.add(directionToOptimalLine.multiply(2.0))

        val cropBox = Box(
            collisionWithFacePlane.x,
            player.pos.y - 2.0,
            collisionWithFacePlane.z,
            b.x,
            player.pos.y + 1.0,
            b.z,
        )

        val clampedFace = trimmedFace.clamp(cropBox)

        // Not much left of the area? Then don't try to sample a point on the face
        if (clampedFace.area < 0.0001)
            return null

        return clampedFace
    }
}

class RandomTargetPositionFactory(val config: PositionFactoryConfiguration) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = trimFace(face)

        return trimmedFace.randomPointOnFace()
    }
}

object CenterTargetPositionFactory : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        return face.center
    }
}

class ReverseYawTargetPositionFactory(val config: PositionFactoryConfiguration) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = trimFace(face)

        val reverseYawRotation = aimAtNearestPointToReverseYaw(targetPos, trimmedFace)

        if (reverseYawRotation == null) {
            return NearestRotationTargetPositionFactory(config).aimAtNearestPointToRotationLine(targetPos, trimmedFace)
        }

        return reverseYawRotation
    }

    fun aimAtNearestPointToReverseYaw(
        targetPos: BlockPos,
        face: Face
    ): Vec3d? {
        if (MathHelper.approximatelyEquals(face.area, 0.0))
            return face.from

        val plane = NormalizedPlane.fromParams(
            config.eyePos.subtract(Vec3d.of(targetPos)),
            Vec3d(0.0, 0.0, 1.0).rotateY(-player.yaw.toRadians()),
            Vec3d(0.0, 1.0, 0.0)
        )

        val intersectLine = face.toPlane().intersection(plane) ?: return null

        val lineSegment = face.coerceInFace(intersectLine)

        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "daLineSegment",
            ModuleDebug.DebuggedLineSegment(
                lineSegment.endPoints.first.add(Vec3d.of(targetPos)),
                lineSegment.endPoints.second.add(Vec3d.of(targetPos)),
                Color4b(255, 0, 0, 255)
            )
        )

        val currentRotation = RotationManager.serverRotation

        val rotationLine = Line(config.eyePos.subtract(Vec3d.of(targetPos)), currentRotation.rotationVec)

        return lineSegment.getNearestPointsTo(rotationLine)?.first ?: return null
    }
}
