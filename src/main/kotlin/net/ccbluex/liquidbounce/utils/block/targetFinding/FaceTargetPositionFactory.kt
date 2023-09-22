package net.ccbluex.liquidbounce.utils.block.targetFinding

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.geometry.Face
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
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
class StabilizedTargetPositionFactory(val config: PositionFactoryConfiguration) : FaceTargetPositionFactory() {
    override fun producePositionOnFace(face: Face, targetPos: BlockPos): Vec3d {
        val trimmedFace = super.trimFaceToConfigRanges(face, config)

        val player = mc.player!!

        val currentRotation = RotationManager.currentRotation ?: player.rotation

        val rotationLine = Line(config.eyePos.subtract(Vec3d.of(targetPos)), currentRotation.rotationVec)

        return trimmedFace.nearestPointTo(rotationLine) ?: return trimmedFace.center
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
