package net.ccbluex.liquidbounce.features.module.modules.combat.autobow.aimbot

import com.google.common.collect.Range
import com.google.common.collect.TreeRangeSet
import net.ccbluex.liquidbounce.features.module.modules.combat.autobow.ModuleAutoBowAimbot
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.client.QuickAccess
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

class ArrowTrajectoryObstacleHeuristic(
    private val eyePos: Vec3d,
    private val targetPos: Vec3d,
    private val deltaPos: Vec3d,
    private val rotation: Rotation,
    private val pullProgress: Float,
) {

    fun findTrajectoryBlockCollisionWithHotspots(
        hotspots: List<Vec3d>,
        lengthToCheckAroundHotspots: Double = 1.0
    ): Vec3d? {
        val lineSegments = getLineSegmentsApproximatingTrajectory(deltaPos, rotation, pullProgress, eyePos, targetPos)

        val hotspotSegments = createHotspotSegments(lineSegments, hotspots, lengthToCheckAroundHotspots)

        val hotspotHit = hotspotSegments.firstNotNullOfOrNull { findBlockCollision(it) }

        if (hotspotHit != null) {
            return hotspotHit
        }

        return lineSegments.firstNotNullOfOrNull { findBlockCollision(it) }
    }

    private fun createHotspotSegments(
        lineSegments: List<LineSegment>,
        hotspots: List<Vec3d>,
        lengthToCheckAroundHotspots: Double
    ): List<LineSegment> {
        val hotspotSegments = mutableListOf<LineSegment>()

        for (lineSegment in lineSegments) {
            val hotspotRanges = TreeRangeSet.create<Double>()

            for (hotspot in hotspots) {
                val line = Line(lineSegment.position, lineSegment.direction)
                val nearestPhiToLine = line.getNearestPhiTo(hotspot)

                hotspotRanges.add(
                    Range.closed(
                        nearestPhiToLine - lengthToCheckAroundHotspots,
                        nearestPhiToLine + lengthToCheckAroundHotspots
                    )
                )
            }

            hotspotSegments.addAll(
                hotspotRanges.asRanges().map {
                    lineSegment.subSegment(it.lowerEndpoint()..it.upperEndpoint())
                }
            )
        }

        return hotspotSegments
    }

    fun findTrajectoryBlockCollision(): Vec3d? {
        val lineSegments = getLineSegmentsApproximatingTrajectory(deltaPos, rotation, pullProgress, eyePos, targetPos)

        return lineSegments.firstNotNullOfOrNull { findBlockCollision(it) }
    }

    private fun findBlockCollision(lineSegment: LineSegment): Vec3d? {
        val raycast =
            QuickAccess.world.raycast(
                RaycastContext(
                    lineSegment.startPoint,
                    lineSegment.endPoint,
                    RaycastContext.ShapeType.COLLIDER, // TODO is COLLIDER better?
                    RaycastContext.FluidHandling.ANY,
                    QuickAccess.player,
                ),
            )

        if (raycast.type != HitResult.Type.BLOCK) {
            return null
        }

        return raycast.pos
    }

    /**
     * Approximates the parabola of the bow by one to two line segments.
     */
    private fun getLineSegmentsApproximatingTrajectory(
        deltaPos: Vec3d,
        rotation: Rotation,
        pullProgress: Float,
        eyePos: Vec3d,
        targetPos: Vec3d
    ): List<LineSegment> {
        val vertex = ModuleAutoBowAimbot.getHighestPointOfTrajectory(deltaPos, rotation, pullProgress)

        val lineSegments = if (vertex == null) {
            listOf(LineSegment.fromPoints(eyePos, targetPos))
        } else {
            listOf(
                LineSegment.fromPoints(eyePos, vertex),
                LineSegment.fromPoints(vertex, targetPos)
            )
        }

        return lineSegments
    }

}
