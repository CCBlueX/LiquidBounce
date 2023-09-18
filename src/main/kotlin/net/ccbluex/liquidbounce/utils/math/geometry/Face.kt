package net.ccbluex.liquidbounce.utils.math.geometry

import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A face. Axis aligned
 */
class Face(from: Vec3d, to: Vec3d) {
    val from: Vec3d
    val to: Vec3d

    init {
        this.from = Vec3d(
            min(from.x, to.x),
            min(from.y, to.y),
            min(from.z, to.z),
        )
        this.to = Vec3d(
            max(from.x, to.x),
            max(from.y, to.y),
            max(from.z, to.z),
        )
    }

    val area: Double
        get() {
            val dims = dimensions

            return (dims.x * dims.y + dims.y * dims.z + dims.x * dims.z) * 2.0
        }

    val center: Vec3d
        get() = Vec3d(
            (to.x + from.x) * 0.5,
            (to.y + from.y) * 0.5,
            (to.z + from.z) * 0.5
        )
    val dimensions: Vec3d
        get() = Vec3d(
            to.x - from.x,
            to.y - from.y,
            to.z - from.z,
        )

    /**
     * If this face is empty, return null, otherwise return this face
     */
    fun requireNonEmpty(): Face? {
        if (MathHelper.approximatelyEquals(this.area, 0.0))
            return null

        return this
    }

    fun truncateY(minY: Double): Face {
        val newFace = Face(
            Vec3d(this.from.x, this.from.y.coerceAtLeast(minY), this.from.z),
            Vec3d(this.to.x, this.to.y.coerceAtLeast(minY), this.to.z)
        )

        return newFace
    }

    fun clamp(box: Box): Face {
        val xRange = box.minX..box.maxX
        val yRange = box.minY..box.maxY
        val zRange = box.minZ..box.maxZ

        val newFrom = Vec3d(
            this.from.x.coerceIn(xRange),
            this.from.y.coerceIn(yRange),
            this.from.z.coerceIn(zRange)
        )
        val newTo = Vec3d(
            this.to.x.coerceIn(xRange),
            this.to.y.coerceIn(yRange),
            this.to.z.coerceIn(zRange)
        )

        return Face(newFrom, newTo)
    }

    fun offset(vec: Vec3d): Face {
        return Face(this.from.add(vec), this.to.add(vec))
    }


    fun randomPointOnFace(): Vec3d {
        return Vec3d(
            if (from.x == to.x) from.x else Random.nextDouble(from.x, to.x),
            if (from.y == to.y) from.y else Random.nextDouble(from.y, to.y),
            if (from.z == to.z) from.z else Random.nextDouble(from.z, to.z),
        )
    }

    fun coerceInFace(point: Vec3d): Vec3d {
        return Vec3d(
            point.x.coerceIn(this.from.x, this.to.x),
            point.y.coerceIn(this.from.y, this.to.z),
            point.z.coerceIn(this.from.z, this.to.z),
        )
    }

    fun toPlane(): NormalizedPlane {
        val dims = this.dimensions

        val xy = Vec3d(
            dims.x,
            dims.y,
            0.0
        )

        val zy = Vec3d(
            0.0,
            dims.y,
            dims.z
        )

        return NormalizedPlane.fromParams(this.from, xy, zy)
    }

    fun nearestPointTo(line: Line): Vec3d? {
        val dims = this.dimensions

        val xy = Vec3d(
            dims.x,
            dims.y,
            0.0
        )

        val zy = Vec3d(
            0.0,
            dims.y,
            dims.z
        )

        val plane = NormalizedPlane.fromParams(this.from, xy, zy)

        val intersection = plane.intersection(line) ?: return null

        val xyLen = xy.lengthSquared()
        val zyLen = zy.lengthSquared()

        val phiRange = 0.0..1.0

        val lines = listOf(
            Pair(LineSegment(this.from, xy, phiRange), zyLen),
            Pair(LineSegment(this.from, zy, phiRange), xyLen),
            Pair(LineSegment(this.to, xy.negate(), phiRange), zyLen),
            Pair(LineSegment(this.to, zy.negate(), phiRange), xyLen),
        )

        val lineDistances = lines.map {
            val nearestPoint = it.first.getNearestPointTo(intersection)

            Triple(nearestPoint, it.first.squaredDistanceTo(intersection), it.second)
        }

        val isInFace = lineDistances.all { it.second <= it.third }

        if (isInFace)
            return intersection

        return lineDistances.minBy { it.second }.first
    }

}
