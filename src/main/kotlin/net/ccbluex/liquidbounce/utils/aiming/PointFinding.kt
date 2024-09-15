package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.geometry.PlaneSection
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.joml.Matrix3f
import org.joml.Vector3f
import kotlin.jvm.optionals.getOrNull
import kotlin.math.*

private val Box.edgePoints: Array<Vec3d>
    get() = arrayOf(
        Vec3d(minX, minY, minZ),
        Vec3d(minX, minY, maxZ),
        Vec3d(minX, maxY, minZ),
        Vec3d(minX, maxY, maxZ),
        Vec3d(maxX, minY, minZ),
        Vec3d(maxX, minY, maxZ),
        Vec3d(maxX, maxY, minZ),
        Vec3d(maxX, maxY, maxZ),
    )

private fun Vec3d.moveTowards(otherPoint: Vec3d, fraction: Double): Vec3d {
    val direction = otherPoint - this

    return this + direction.multiply(fraction)
}

/**
 * Creates rotation matrices: The first allows to turn the vec (1.0, 0.0, 0.0) into the given [vec].
 * The second allows to turn the given vec into (1.0, 0.0, 0.0).
 */
fun getRotationMatricesForVec(vec: Vec3d): Pair<Matrix3f, Matrix3f> {
    val hypotenuse = hypot(vec.x, vec.z)

    val yawAtan = atan2(vec.z, vec.x).toFloat()
    val pitchAtan = atan2(vec.y, hypotenuse).toFloat()

    val toMatrix = Matrix3f().rotateY(-yawAtan).mul(Matrix3f().rotateZ(pitchAtan))
    val backMatrix = Matrix3f().rotateZ(-pitchAtan).mul(Matrix3f().rotateY(yawAtan))

    return toMatrix to backMatrix
}

/**
 * Finds the minimal plane section which covers all of the [targetBox] from the perspective of the [virtualEye].
 */
fun projectPointsOnBox(virtualEye: Vec3d, targetBox: Box): ArrayList<Vec3d>? {
    if (targetBox.contains(virtualEye)) {
        return null
    }

    val playerToBoxLine = Line(position = virtualEye, direction = targetBox.center - virtualEye)

    // Find a point between the virtual eye and the target box such that every edge point of the box is behind it
    // (from the perspective of the virtual eye). This position is used to craft a the targeting frame
    val targetFrameOrigin = targetBox.edgePoints
        .map { playerToBoxLine.getNearestPointTo(it) }
        .minBy { it.squaredDistanceTo(virtualEye) }
        .moveTowards(virtualEye, 0.1)

    val plane = NormalizedPlane(targetFrameOrigin, playerToBoxLine.direction)
    val (toMatrix, backMatrix) = getRotationMatricesForVec(plane.normalVec)

    val projectedAndRotatedPoints = targetBox.edgePoints.map {
        plane.intersection(Line.fromPoints(virtualEye, it))!!.subtract(targetFrameOrigin).toVector3f().mul(backMatrix)
    }

    var minZ = 0.0F
    var maxZ = 0.0F
    var minY = 0.0F
    var maxY = 0.0F

    projectedAndRotatedPoints.forEach {
        minZ = min(minZ, it.z)
        maxZ = max(maxZ, it.z)
        minY = min(minY, it.y)
        maxY = max(maxY, it.y)
    }

    val posVec =
        Vec3d(0.0, minY.toDouble(), minZ.toDouble()).toVector3f().mul(toMatrix).toVec3d().add(targetFrameOrigin)
    val dirVecY = Vec3d(0.0, (maxY - minY).toDouble(), 0.0).toVector3f().mul(toMatrix).toVec3d()
    val dirVecZ = Vec3d(0.0, 0.0, (maxZ - minZ).toDouble()).toVector3f().mul(toMatrix).toVec3d()

    val planeSection = PlaneSection(posVec, dirVecY, dirVecZ)

    val points = ArrayList<Vec3d>()

    planeSection.castPointsOnUniformly(128) { point ->
        // Extent the point from the face on.
        val pointExtended = point.moveTowards(virtualEye, -100.0)

        val pos = targetBox.raycast(virtualEye, pointExtended).getOrNull() ?: return@castPointsOnUniformly

        points.add(pos)
    }

    return points
}

private fun Vector3f.toVec3d(): Vec3d = Vec3d(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
