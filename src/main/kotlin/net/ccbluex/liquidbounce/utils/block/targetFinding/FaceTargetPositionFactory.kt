package net.ccbluex.liquidbounce.utils.block.targetFinding

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.geometry.Face
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d


data class PositionFactoryConfiguration(
    val eyePos: Vec3d,
    val xRange: ClosedFloatingPointRange<Double>,
    val yRange: ClosedFloatingPointRange<Double>,
    val zRange: ClosedFloatingPointRange<Double>,
    val scanStep: Double,
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
    protected fun trimFaceToConfigRanges(face: Face, config: PositionFactoryConfiguration): Face {
        // The box in with our pick must be (according to config)
        val possibleTargetBox = Box(
            config.xRange.start,
            config.yRange.start,
            config.zRange.start,
            config.xRange.endInclusive,
            config.yRange.endInclusive,
            config.zRange.endInclusive,
        ).offset(Vec3d(-0.5, -0.5, -0.5)).offset(face.center)

        // Trim our face in order to only contain positions valid by config
        return face.clamp(possibleTargetBox)
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
        val trimmedFace = super.trimFaceToConfigRanges(face, config)

        return aimAtNearestPointToRotationLine(targetPos, trimmedFace)
    }

    fun aimAtNearestPointToRotationLine(
        targetPos: BlockPos,
        face: Face
    ): Vec3d {
        if (MathHelper.approximatelyEquals(face.area, 0.0))
            return face.from

        val currentRotation = RotationManager.rotationForServer

        val rotationLine = Line(config.eyePos.subtract(Vec3d.of(targetPos)), currentRotation.rotationVec)

        return face.nearestPointTo(rotationLine) ?: face.center
    }
}

/**
 * Always targets the point with the nearest rotation angle to the current rotation angle
 */
class StabilizedRotationTargetPositionFactory(val config: PositionFactoryConfiguration, val optimalLine: Line?) :
    FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = super.trimFaceToConfigRanges(face, config).offset(Vec3d.of(targetPos))

        val player = mc.player!!

        val targetFace = getTargetFace(player, trimmedFace, face) ?: trimmedFace

        return NearestRotationTargetPositionFactory(this.config).producePositionOnFace(
            targetFace.offset(
                Vec3d.of(
                    targetPos
                ).negate()
            ), targetPos
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
        val targetFace = clampedFace

        ModuleDebug.debugGeometry(ModuleScaffold, "optimalLine", ModuleDebug.DebuggedLine(optimalLine, Color4b.GREEN))
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "optimalLineFromPlayer",
            ModuleDebug.DebuggedLine(optimalLineFromPlayer, Color4b.WHITE)
        )
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "facePreCrop",
            ModuleDebug.DebuggedBox(Box(trimmedFace.from, trimmedFace.to), Color4b(255, 0, 0, 64))
        )
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "cropBox",
            ModuleDebug.DebuggedBox(cropBox, Color4b(0, 127, 127, 64))
        )

        // Not much left of the area? Then don't try to sample a point on the face
        if (targetFace.area < 0.0001)
            return null

        return targetFace
    }
}

class RandomTargetPositionFactory(val config: PositionFactoryConfiguration) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = super.trimFaceToConfigRanges(face, config)

        return trimmedFace.randomPointOnFace()
    }
}

object CenterTargetPositionFactory : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        return face.center
    }
}
