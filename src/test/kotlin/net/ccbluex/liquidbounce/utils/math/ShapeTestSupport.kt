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
package net.ccbluex.liquidbounce.utils.math

import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.math.roundToLong

private const val TEST_EPSILON = 1.0E-7

data class FaceSpec(
    val direction: Direction,
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double,
)

data class LineSpec(
    val startX: Double,
    val startY: Double,
    val startZ: Double,
    val endX: Double,
    val endY: Double,
    val endZ: Double,
)

data class PositionedShapeSpec<K>(
    val blockPos: Long,
    val key: K,
    val boxes: List<AABB>,
)

fun face(
    direction: Direction,
    minX: Double,
    minY: Double,
    minZ: Double,
    maxX: Double,
    maxY: Double,
    maxZ: Double,
) = FaceSpec(direction, minX, minY, minZ, maxX, maxY, maxZ)

fun line(
    startX: Double,
    startY: Double,
    startZ: Double,
    endX: Double,
    endY: Double,
    endZ: Double,
) = LineSpec(startX, startY, startZ, endX, endY, endZ)

fun VoxelShape.collectFacesForTest(): List<FaceSpec> = buildList {
    forAllFaces { direction, minX, minY, minZ, maxX, maxY, maxZ ->
        this += face(direction, minX, minY, minZ, maxX, maxY, maxZ)
    }
}

fun VoxelShape.collectSideFacesForTest(side: Direction, hitPos: Vec3): List<FaceSpec> = buildList {
    forAllSideFaces(side, hitPos) { direction, minX, minY, minZ, maxX, maxY, maxZ ->
        this += face(direction, minX, minY, minZ, maxX, maxY, maxZ)
    }
}

fun VoxelShape.collectSideOutlineEdgesForTest(side: Direction, hitPos: Vec3): List<LineSpec> = buildList {
    forAllSideOutlineEdges(side, hitPos) { startX, startY, startZ, endX, endY, endZ ->
        this += line(startX, startY, startZ, endX, endY, endZ)
    }
}

fun <K> List<PositionedVoxelShape<K>>.toShapeSpecs(): List<PositionedShapeSpec<K>> =
    map { shape ->
        PositionedShapeSpec(
            blockPos = shape.blockPos,
            key = shape.key,
            boxes = shape.shape.toAabbs(),
        )
    }

fun assertSameFaces(expected: Collection<FaceSpec>, actual: Collection<FaceSpec>) {
    assertEquals(expected.normalizedFaceKeys(), actual.normalizedFaceKeys())
}

fun assertSameLines(expected: Collection<LineSpec>, actual: Collection<LineSpec>) {
    assertEquals(expected.normalizedLineKeys(), actual.normalizedLineKeys())
}

fun <K> assertSamePositionedShapes(
    expected: Collection<PositionedShapeSpec<K>>,
    actual: Collection<PositionedShapeSpec<K>>,
) {
    assertEquals(expected.normalizedShapeKeys(), actual.normalizedShapeKeys())
}

fun assertHasNoZeroAreaFaces(faces: Collection<FaceSpec>) {
    val zeroAreaFaces = faces.filter { face ->
        when (face.direction.axis) {
            Direction.Axis.X -> face.minY.closeTo(face.maxY) || face.minZ.closeTo(face.maxZ)
            Direction.Axis.Y -> face.minX.closeTo(face.maxX) || face.minZ.closeTo(face.maxZ)
            Direction.Axis.Z -> face.minX.closeTo(face.maxX) || face.minY.closeTo(face.maxY)
        }
    }
    assertEquals(emptyList<FaceSpec>(), zeroAreaFaces)
}

private fun Collection<FaceSpec>.normalizedFaceKeys(): List<String> =
    map { face ->
        listOf(
            face.direction.name,
            face.minX.normalizedKeyPart(),
            face.minY.normalizedKeyPart(),
            face.minZ.normalizedKeyPart(),
            face.maxX.normalizedKeyPart(),
            face.maxY.normalizedKeyPart(),
            face.maxZ.normalizedKeyPart(),
        ).joinToString("|")
    }.sorted()

private fun Collection<LineSpec>.normalizedLineKeys(): List<String> =
    map(LineSpec::normalizedLineKey).sorted()

private fun <K> Collection<PositionedShapeSpec<K>>.normalizedShapeKeys(): List<String> =
    map { spec ->
        val boxesKey = spec.boxes.map(AABB::normalizedBoxKey).sorted().joinToString(";")
        "${spec.key}|${spec.blockPos}|$boxesKey"
    }.sorted()

private fun AABB.normalizedBoxKey(): String = listOf(
    minX.normalizedKeyPart(),
    minY.normalizedKeyPart(),
    minZ.normalizedKeyPart(),
    maxX.normalizedKeyPart(),
    maxY.normalizedKeyPart(),
    maxZ.normalizedKeyPart(),
).joinToString("|")

private fun LineSpec.normalizedLineKey(): String {
    val start = doubleArrayOf(startX, startY, startZ)
    val end = doubleArrayOf(endX, endY, endZ)
    val ordered = if (comesBefore(start, end)) start + end else end + start
    return ordered.joinToString("|") { it.normalizedKeyPart() }
}

private fun comesBefore(first: DoubleArray, second: DoubleArray): Boolean {
    for (index in first.indices) {
        if (!first[index].closeTo(second[index])) {
            return first[index] < second[index]
        }
    }
    return true
}

private fun Double.normalizedKeyPart(): String = (this / TEST_EPSILON).roundToLong().toString()

private fun Double.closeTo(other: Double): Boolean = kotlin.math.abs(this - other) <= TEST_EPSILON
