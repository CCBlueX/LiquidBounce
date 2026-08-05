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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.math

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.function.ToDoubleFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.toList

@OptIn(ExperimentalContracts::class)
inline fun VoxelShape.ifEmpty(defaultValue: () -> VoxelShape): VoxelShape {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (isEmpty) defaultValue() else this
}

inline fun VoxelShape?.orEmpty(): VoxelShape = this ?: Shapes.empty()

fun Iterable<VoxelShape>.allEmpty(): Boolean = all { it.isEmpty }

fun Iterable<VoxelShape>.anyNotEmpty(): Boolean = any { !it.isEmpty }

/**
 * @return null if shape is empty
 */
fun VoxelShape.boundsOrNull(): AABB? = if (isEmpty) null else bounds()

fun VoxelShape.distanceToSqr(position: Vec3): Double =
    this.closestPointTo(position).orElse(null)?.distanceToSqr(position) ?: Double.POSITIVE_INFINITY

private val AABB_BIGGER_FIRST = Comparator.comparingDouble(ToDoubleFunction(AABB::getSize)).reversed()

private const val SHAPE_EPSILON = 1.0E-7

fun interface DoubleFaceConsumer {
    fun consume(
        direction: Direction,
        minX: Double,
        minY: Double,
        minZ: Double,
        maxX: Double,
        maxY: Double,
        maxZ: Double,
    )
}

/**
 * Order: bigger first
 */
fun VoxelShape.toSortedAabbs(): MutableList<AABB> {
    val list = ArrayList<AABB>()
    this.toAabbs(list)
    list.sortWith(AABB_BIGGER_FIRST)
    return list
}

fun VoxelShape.toAabbs(destination: MutableCollection<in AABB>) {
    this.forAllBoxes { x1, y1, z1, x2, y2, z2 -> destination.add(AABB(x1, y1, z1, x2, y2, z2)) }
}

fun VoxelShape.clipAllBoxes(
    base: BlockPos,
    from: Vec3,
    to: Vec3,
): List<Vec3> {
    return when {
        this.isEmpty -> emptyList()

        this == Shapes.block() ->
            AABB.clip(
                base.x.toDouble(),
                base.y.toDouble(),
                base.z.toDouble(),
                1.0 + base.x,
                1.0 + base.y,
                1.0 + base.z,
                from,
                to,
            ).toList()

        else -> buildList {
            forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                AABB.clip(
                    minX + base.x,
                    minY + base.y,
                    minZ + base.z,
                    maxX + base.x,
                    maxY + base.y,
                    maxZ + base.z,
                    from,
                    to,
                ).orElse(null)?.let {
                    this.add(it)
                }
            }
        }
    }
}

fun VoxelShape.forAllFaces(action: DoubleFaceConsumer) {
    if (isEmpty) {
        return
    }

    ShapeSurfaceMesh.of(this).forAllFaces(action)
}

fun VoxelShape.forAllSideFaces(
    side: Direction,
    hitPos: Vec3,
    action: DoubleFaceConsumer,
) {
    if (isEmpty) {
        return
    }

    ShapeSurfaceMesh.of(this).forAllSideFaces(side, hitPos, action)
}

fun VoxelShape.forAllSideOutlineEdges(
    side: Direction,
    hitPos: Vec3,
    action: Shapes.DoubleLineConsumer,
) {
    if (isEmpty) {
        return
    }

    ShapeSurfaceMesh.of(this).forAllSideOutlineEdges(side, hitPos, action)
}

/**
 * Shrinks a [VoxelShape] by the specified amounts on selected axes.
 */
@Suppress("CognitiveComplexMethod")
fun VoxelShape.shrink(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): VoxelShape {
    return when {
        this.isEmpty -> this
        this == Shapes.block() -> Shapes.box(
            x, y, z,
            1.0 - x, 1.0 - y, 1.0 - z
        )

        else -> {
            val shape = ShapeJoiner()

            this.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                val width = maxX - minX
                val height = maxY - minY
                val depth = maxZ - minZ

                val canShrinkX = x == 0.0 || width > x * 2
                val canShrinkY = y == 0.0 || height > y * 2
                val canShrinkZ = z == 0.0 || depth > z * 2

                if (canShrinkX && canShrinkY && canShrinkZ) {
                    val shrunkBox = Shapes.box(
                        minX + (if (x > 0) x else 0.0),
                        minY + (if (y > 0) y else 0.0),
                        minZ + (if (z > 0) z else 0.0),
                        maxX - (if (x > 0) x else 0.0),
                        maxY - (if (y > 0) y else 0.0),
                        maxZ - (if (z > 0) z else 0.0)
                    )

                    shape.add(shrunkBox)
                }
            }

            shape.value
        }
    }
}

private class ShapeSurfaceMesh(
    private val xs: DoubleArray,
    private val ys: DoubleArray,
    private val zs: DoubleArray,
    private val occupancy: BooleanArray,
) {

    private val xSize = xs.size - 1
    private val ySize = ys.size - 1
    private val zSize = zs.size - 1

    fun forAllFaces(action: DoubleFaceConsumer) {
        for (direction in Direction.entries) {
            forEachMergedFace(direction, null, action)
        }
    }

    fun forAllSideFaces(
        side: Direction,
        hitPos: Vec3,
        action: DoubleFaceConsumer,
    ) {
        val component = findConnectedComponent(side, hitPos) ?: return
        forEachMergedFace(side, component, action)
    }

    fun forAllSideOutlineEdges(
        side: Direction,
        hitPos: Vec3,
        action: Shapes.DoubleLineConsumer,
    ) {
        val component = findConnectedComponent(side, hitPos) ?: return
        component.mask.forEachPerimeterSegment { startU, startV, endU, endV ->
            val start = planePoint(side, component.planeIndex, startU, startV)
            val end = planePoint(side, component.planeIndex, endU, endV)
            action.consume(start.x, start.y, start.z, end.x, end.y, end.z)
        }
    }

    private fun forEachMergedFace(
        direction: Direction,
        component: FaceComponent?,
        action: DoubleFaceConsumer,
    ) {
        if (component != null) {
            component.mask.forEachMergedRect { startU, startV, endU, endV ->
                val face = faceBounds(direction, component.planeIndex, startU, startV, endU, endV)
                action.consume(direction, face.minX, face.minY, face.minZ, face.maxX, face.maxY, face.maxZ)
            }
            return
        }

        for (planeIndex in facePlaneIndices(direction)) {
            val mask = buildFaceMask(direction, planeIndex)
            if (!mask.hasAny) {
                continue
            }

            mask.forEachMergedRect { startU, startV, endU, endV ->
                val face = faceBounds(direction, planeIndex, startU, startV, endU, endV)
                action.consume(direction, face.minX, face.minY, face.minZ, face.maxX, face.maxY, face.maxZ)
            }
        }
    }

    private fun findConnectedComponent(direction: Direction, hitPos: Vec3): FaceComponent? {
        for (planeIndex in facePlaneIndices(direction)) {
            val mask = buildFaceMask(direction, planeIndex)
            if (!mask.hasAny) {
                continue
            }

            val seed = findSeedCell(mask, direction, planeIndex, hitPos)
            if (seed == -1L) continue
            return FaceComponent(planeIndex, floodFill(mask, seed.high32(), seed.low32()))
        }

        return null
    }

    private fun findSeedCell(mask: PlaneMask, direction: Direction, planeIndex: Int, hitPos: Vec3): Long {
        for (v in 0 until mask.height) {
            for (u in 0 until mask.width) {
                if (!mask[u, v]) {
                    continue
                }

                if (faceContainsPoint(direction, planeIndex, u, v, hitPos)) {
                    return longFrom32(u, v)
                }
            }
        }

        return -1
    }

    private fun faceContainsPoint(direction: Direction, planeIndex: Int, u: Int, v: Int, hitPos: Vec3): Boolean {
        val face = faceBounds(direction, planeIndex, u, v, u + 1, v + 1)
        return when (direction.axis) {
            Direction.Axis.Y ->
                approximatelyEquals(hitPos.y, face.minY) &&
                    hitPos.x >= face.minX - SHAPE_EPSILON &&
                    hitPos.x <= face.maxX + SHAPE_EPSILON &&
                    hitPos.z >= face.minZ - SHAPE_EPSILON &&
                    hitPos.z <= face.maxZ + SHAPE_EPSILON

            Direction.Axis.Z ->
                approximatelyEquals(hitPos.z, face.minZ) &&
                    hitPos.x >= face.minX - SHAPE_EPSILON &&
                    hitPos.x <= face.maxX + SHAPE_EPSILON &&
                    hitPos.y >= face.minY - SHAPE_EPSILON &&
                    hitPos.y <= face.maxY + SHAPE_EPSILON

            Direction.Axis.X ->
                approximatelyEquals(hitPos.x, face.minX) &&
                    hitPos.z >= face.minZ - SHAPE_EPSILON &&
                    hitPos.z <= face.maxZ + SHAPE_EPSILON &&
                    hitPos.y >= face.minY - SHAPE_EPSILON &&
                    hitPos.y <= face.maxY + SHAPE_EPSILON
        }
    }

    private fun floodFill(mask: PlaneMask, seedU: Int, seedV: Int): PlaneMask {
        val component = BooleanArray(mask.mask.size)
        val stack = IntArrayList()
        stack.push(mask.index(seedU, seedV))

        while (stack.isNotEmpty()) {
            val current = stack.popInt()
            if (component[current] || !mask.mask[current]) {
                continue
            }

            component[current] = true

            val u = current % mask.width
            val v = current / mask.width

            if (u > 0) {
                stack.push(mask.index(u - 1, v))
            }
            if (u + 1 < mask.width) {
                stack.push(mask.index(u + 1, v))
            }
            if (v > 0) {
                stack.push(mask.index(u, v - 1))
            }
            if (v + 1 < mask.height) {
                stack.push(mask.index(u, v + 1))
            }
        }

        return PlaneMask(mask.width, mask.height, component, hasAny = true)
    }

    private fun facePlaneIndices(direction: Direction): IntRange = when (direction) {
        Direction.DOWN -> 0 until ySize
        Direction.UP -> 1..ySize
        Direction.NORTH -> 0 until zSize
        Direction.SOUTH -> 1..zSize
        Direction.WEST -> 0 until xSize
        Direction.EAST -> 1..xSize
    }

    private fun buildFaceMask(direction: Direction, planeIndex: Int): PlaneMask = when (direction) {
        Direction.DOWN -> buildMask(xSize, zSize) { u, v ->
            isFull(u, planeIndex, v) && !isFullWide(u, planeIndex - 1, v)
        }

        Direction.UP -> buildMask(xSize, zSize) { u, v ->
            val y = planeIndex - 1
            isFull(u, y, v) && !isFullWide(u, y + 1, v)
        }

        Direction.NORTH -> buildMask(xSize, ySize) { u, v ->
            isFull(u, v, planeIndex) && !isFullWide(u, v, planeIndex - 1)
        }

        Direction.SOUTH -> buildMask(xSize, ySize) { u, v ->
            val z = planeIndex - 1
            isFull(u, v, z) && !isFullWide(u, v, z + 1)
        }

        Direction.WEST -> buildMask(zSize, ySize) { u, v ->
            isFull(planeIndex, v, u) && !isFullWide(planeIndex - 1, v, u)
        }

        Direction.EAST -> buildMask(zSize, ySize) { u, v ->
            val x = planeIndex - 1
            isFull(x, v, u) && !isFullWide(x + 1, v, u)
        }
    }

    private inline fun buildMask(width: Int, height: Int, predicate: (u: Int, v: Int) -> Boolean): PlaneMask {
        val mask = BooleanArray(width * height)
        var hasAny = false

        for (v in 0 until height) {
            for (u in 0 until width) {
                val filled = predicate(u, v)
                mask[v * width + u] = filled
                hasAny = hasAny || filled
            }
        }

        return PlaneMask(width, height, mask, hasAny)
    }

    private fun faceBounds(
        direction: Direction,
        planeIndex: Int,
        startU: Int,
        startV: Int,
        endU: Int,
        endV: Int,
    ): AABB = when (direction.axis) {
        Direction.Axis.Y -> {
            val y = ys[planeIndex]
            AABB(xs[startU], y, zs[startV], xs[endU], y, zs[endV])
        }

        Direction.Axis.Z -> {
            val z = zs[planeIndex]
            AABB(xs[startU], ys[startV], z, xs[endU], ys[endV], z)
        }

        Direction.Axis.X -> {
            val x = xs[planeIndex]
            AABB(x, ys[startV], zs[startU], x, ys[endV], zs[endU])
        }
    }

    private fun planePoint(direction: Direction, planeIndex: Int, u: Int, v: Int): Vec3 = when (direction.axis) {
        Direction.Axis.Y -> Vec3(xs[u], ys[planeIndex], zs[v])
        Direction.Axis.Z -> Vec3(xs[u], ys[v], zs[planeIndex])
        Direction.Axis.X -> Vec3(xs[planeIndex], ys[v], zs[u])
    }

    private fun isFull(x: Int, y: Int, z: Int): Boolean = occupancy[index(x, y, z)]

    private fun isFullWide(x: Int, y: Int, z: Int): Boolean {
        return x >= 0 && x < xSize && y >= 0 && y < ySize && z >= 0 && z < zSize && isFull(x, y, z)
    }

    private fun index(x: Int, y: Int, z: Int): Int = (x * ySize + y) * zSize + z

    companion object {
        /**
         * @see VoxelShape.findIndex
         */
        private fun DoubleArray.findIndex(value: Double): Int {
            var low = 0
            var high = size
            while (low < high) {
                val mid = (low + high) ushr 1
                if (value < this[mid]) high = mid else low = mid + 1
            }
            val index = low - 1
            if (index !in indices || this[index] != value) {
                throw IllegalArgumentException("Could not resolve coordinate index for $value")
            }

            return index
        }

        fun of(shape: VoxelShape): ShapeSurfaceMesh {
            val xs = shape.getCoords(Direction.Axis.X).toDoubleArray()
            val ys = shape.getCoords(Direction.Axis.Y).toDoubleArray()
            val zs = shape.getCoords(Direction.Axis.Z).toDoubleArray()

            val xSize = xs.size - 1
            val ySize = ys.size - 1
            val zSize = zs.size - 1
            val occupancy = BooleanArray(xSize * ySize * zSize)
            val mesh = ShapeSurfaceMesh(xs, ys, zs, occupancy)

            shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                val startX = xs.findIndex(minX)
                val startY = ys.findIndex(minY)
                val startZ = zs.findIndex(minZ)
                val endX = xs.findIndex(maxX)
                val endY = ys.findIndex(maxY)
                val endZ = zs.findIndex(maxZ)

                for (x in startX until endX) {
                    for (y in startY until endY) {
                        for (z in startZ until endZ) {
                            occupancy[mesh.index(x, y, z)] = true
                        }
                    }
                }
            }

            return mesh
        }
    }
}

private class PlaneMask(
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val mask: BooleanArray,
    @JvmField val hasAny: Boolean,
) {
    operator fun get(u: Int, v: Int): Boolean = mask[index(u, v)]

    fun index(u: Int, v: Int): Int = v * width + u

    @Suppress("NestedBlockDepth", "CognitiveComplexMethod")
    inline fun forEachMergedRect(action: (startU: Int, startV: Int, endU: Int, endV: Int) -> Unit) {
        val visited = BooleanArray(mask.size)

        for (v in 0 until height) {
            var u = 0
            while (u < width) {
                val index = index(u, v)
                if (!mask[index] || visited[index]) {
                    u++
                    continue
                }

                var endU = u + 1
                while (endU < width && mask[index(endU, v)] && !visited[index(endU, v)]) {
                    endU++
                }

                var endV = v + 1
                while (endV < height) {
                    var canExtend = true
                    for (currentU in u until endU) {
                        val currentIndex = index(currentU, endV)
                        if (!mask[currentIndex] || visited[currentIndex]) {
                            canExtend = false
                            break
                        }
                    }

                    if (!canExtend) {
                        break
                    }
                    endV++
                }

                for (currentV in v until endV) {
                    for (currentU in u until endU) {
                        visited[index(currentU, currentV)] = true
                    }
                }

                action(u, v, endU, endV)
                u = endU
            }
        }
    }

    inline fun forEachPerimeterSegment(action: (startU: Int, startV: Int, endU: Int, endV: Int) -> Unit) {
        for (v in 0 until height) {
            emitHorizontalRuns(v, isUpperBoundary = false, action)
            emitHorizontalRuns(v, isUpperBoundary = true, action)
        }

        for (u in 0 until width) {
            emitVerticalRuns(u, isRightBoundary = false, action)
            emitVerticalRuns(u, isRightBoundary = true, action)
        }
    }

    private inline fun emitHorizontalRuns(
        row: Int,
        isUpperBoundary: Boolean,
        action: (startU: Int, startV: Int, endU: Int, endV: Int) -> Unit,
    ) {
        var runStart = -1

        for (u in 0..width) {
            val hasBoundary = u < width && this[u, row] && when {
                !isUpperBoundary -> row == 0 || !this[u, row - 1]
                else -> row == height - 1 || !this[u, row + 1]
            }

            if (hasBoundary) {
                if (runStart == -1) {
                    runStart = u
                }
                continue
            }

            if (runStart != -1) {
                val y = if (isUpperBoundary) row + 1 else row
                action(runStart, y, u, y)
                runStart = -1
            }
        }
    }

    private inline fun emitVerticalRuns(
        column: Int,
        isRightBoundary: Boolean,
        action: (startU: Int, startV: Int, endU: Int, endV: Int) -> Unit,
    ) {
        var runStart = -1

        for (v in 0..height) {
            val hasBoundary = v < height && this[column, v] && when {
                !isRightBoundary -> column == 0 || !this[column - 1, v]
                else -> column == width - 1 || !this[column + 1, v]
            }

            if (hasBoundary) {
                if (runStart == -1) {
                    runStart = v
                }
                continue
            }

            if (runStart != -1) {
                val x = if (isRightBoundary) column + 1 else column
                action(x, runStart, x, v)
                runStart = -1
            }
        }
    }
}

@JvmRecord
private data class FaceComponent(
    val planeIndex: Int,
    val mask: PlaneMask,
)

private fun approximatelyEquals(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) <= SHAPE_EPSILON
