package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.NormalizedPlane
import net.ccbluex.liquidbounce.utils.math.geometry.PlaneSection
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

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
 * Finds the minimal plane section which covers all of the [targetBox] from the perspective of the [virtualEye].
 */
fun findBoxTargetingFrame(virtualEye: Vec3d, targetBox: Box): PlaneSection? {
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

    return TODO()
}
