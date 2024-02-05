package net.ccbluex.liquidbounce.features.module.modules.movement.parkour

import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.math.horizontalComponent
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.joml.Vector2d
import kotlin.math.sqrt

fun findNearestPlatform(platforms: List<Platform>, nextGround: Vec3d): Platform? {
    return platforms.minByOrNull { platform ->
        platform.platformBlocks.minOf { Vec3d.ofCenter(it, 1.0).squaredDistanceTo(nextGround) }
    }
}

/**
 * With two line segments (A: a1 -> a2, B: b1 -> b2) find the closest point of A to B
 */
fun intersectLineSegmentLineSegment(a1: Vector2d, a2: Vector2d, b1: Vector2d, b2: Vector2d): Vector2d? {
    val x_1 = a1.x
    val x_2 = a2.x
    val x_3 = b1.x
    val x_4 = b2.x

    val y_1 = a1.y
    val y_2 = a2.y
    val y_3 = b1.y
    val y_4 = b2.y

    val denominator = (x_1 - x_2) * (y_3 - y_4) - (y_1 - y_2) * (x_3 - x_4)

    if (MathHelper.approximatelyEquals(denominator, 0.0)) {
        return null
    }

    val t = ((x_1 - x_3) * (y_3 - y_4) - (y_1 - y_3) * (x_3 - x_4)) / denominator
    val v = -((x_1 - x_2) * (y_1 - y_3) - (y_1 - y_2) * (x_1 - x_3)) / denominator

    if (t !in 0.0..1.0 || v !in 0.0..1.0) {
        return null
    }

    // t =  \frac{(x_1 - x_3)(y_3-y_4)-(y_1-y_3)(x_3-x_4)}{(x_1-x_2)(y_3-y_4)-(y_1-y_2)(x_3-x_4)}
    // v = -\frac{(x_1 - x_2)(y_1-y_3)-(y_1-y_2)(x_1-x_3)}{(x_1-x_2)(y_3-y_4)-(y_1-y_2)(x_3-x_4)},

    return Vector2d(x_1 + t * (x_2 - x_1), y_1 + t * (y_2 - y_1))
}

/**
 * See 'jumpLength' notebook
 */
fun approxJumpDistance(fallDistance: Double): Double? {
    // This approximation function has no good approximation above 10 blocks fall distance
    if (fallDistance > 10) {
        return null
    }

    return when {
        fallDistance < -1.25 -> null
        fallDistance < 0 -> 2.7
        else -> -0.0148951 * fallDistance * fallDistance + 0.4674965 * fallDistance + 3.65566434 - 0.1
    }
}

fun tagReachable(platforms: List<Platform>, jumpOffPoint: Vec3d, currentPos: Vec3d) {
    for (platform in platforms) {
        val hasReachableEdge = platform.edges.any {
            val platformY = (platform.platformBlocks[0].y + 1).toDouble()
            val distance = sqrt(
                it.squaredDistanceTo(
                    Vec3d(
                        jumpOffPoint.x,
                        platformY,
                        jumpOffPoint.z
                    )
                )
            ) // Overhang of 0.3 on every block

            val yDistance = currentPos.y - platformY

            val jumpDistance = approxJumpDistance(yDistance) ?: return@any false

            distance < jumpDistance
        }

        platform.reachable = hasReachableEdge
    }
}

fun findJumpOffPosition(
    currentPos: Vec3d,
    rotVec: Vec3d,
    currentPlatform: Platform
): Pair<LineSegment, Vec3d>? {
    val targetLineSegment = LineSegment(currentPos, rotVec, 0.0..6.0)

    val edgeCandidates = findIntersects(currentPlatform, targetLineSegment, currentPos.y)

    val eee = edgeCandidates.minByOrNull { targetLineSegment.squaredDistanceTo(it.second) }

    return eee
}

fun findIntersects(
    platform: Platform,
    targetLineSegment: LineSegment,
    platformY: Double
): List<Pair<LineSegment, Vec3d>> {
    val edgeCandidates = platform.edges.mapNotNull {
        val (a1, a2) = targetLineSegment.points
        val (b1, b2) = it.points

        val point = intersectLineSegmentLineSegment(
            a1.horizontalComponent(),
            a2.horizontalComponent(),
            b1.horizontalComponent(),
            b2.horizontalComponent(),
        )
        if (point == null)
            return@mapNotNull null

        val jumpOffPoint = Vec3d(point.x, platformY, point.y)

        return@mapNotNull it to jumpOffPoint
    }

    return edgeCandidates
}
