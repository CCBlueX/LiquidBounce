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
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShapeExtensionsTest {

    @Test
    fun `forAllFaces returns empty for empty shape`() {
        assertSameFaces(emptyList(), Shapes.empty().collectFacesForTest())
    }

    @Test
    fun `forAllFaces returns six faces for full cube`() {
        assertSameFaces(
            expected = listOf(
                face(Direction.DOWN, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0),
                face(Direction.UP, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0),
                face(Direction.NORTH, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0),
                face(Direction.SOUTH, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0),
                face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0),
                face(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            ),
            actual = Shapes.block().collectFacesForTest(),
        )
    }

    @Test
    fun `forAllFaces returns correct slab faces without internal surfaces`() {
        val slab = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)

        assertSameFaces(
            expected = listOf(
                face(Direction.DOWN, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0),
                face(Direction.UP, 0.0, 0.5, 0.0, 1.0, 0.5, 1.0),
                face(Direction.NORTH, 0.0, 0.0, 0.0, 1.0, 0.5, 0.0),
                face(Direction.SOUTH, 0.0, 0.0, 1.0, 1.0, 0.5, 1.0),
                face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0),
                face(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 0.5, 1.0),
            ),
            actual = slab.collectFacesForTest(),
        )
    }

    @Test
    fun `forAllFaces merges coplanar stair faces and omits shared interface`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
            Shapes.box(0.0, 0.5, 0.0, 0.5, 1.0, 1.0),
        )

        val faces = shape.collectFacesForTest()

        assertSameFaces(
            expected = listOf(face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0)),
            actual = faces.filter { it.direction == Direction.WEST },
        )
        assertSameFaces(
            expected = listOf(
                face(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                face(Direction.EAST, 0.5, 0.5, 0.0, 0.5, 1.0, 1.0),
            ),
            actual = faces.filter { it.direction == Direction.EAST },
        )
        assertTrue(faces.none { it == face(Direction.UP, 0.0, 0.5, 0.0, 0.5, 0.5, 1.0) })
        assertTrue(faces.none { it == face(Direction.DOWN, 0.0, 0.5, 0.0, 0.5, 0.5, 1.0) })
    }

    @Test
    fun `forAllFaces keeps disconnected coplanar regions separate`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        assertSameFaces(
            expected = listOf(
                face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 0.4),
                face(Direction.WEST, 0.0, 0.0, 0.6, 0.0, 1.0, 1.0),
            ),
            actual = shape.collectFacesForTest().filter { it.direction == Direction.WEST },
        )
    }

    @Test
    fun `forAllFaces preserves concave notch faces`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5),
            Shapes.box(0.0, 0.0, 0.5, 0.5, 1.0, 1.0),
        )
        val faces = shape.collectFacesForTest()

        assertSameFaces(
            expected = listOf(
                face(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 1.0, 0.5),
                face(Direction.EAST, 0.5, 0.0, 0.5, 0.5, 1.0, 1.0),
            ),
            actual = faces.filter { it.direction == Direction.EAST },
        )
        assertSameFaces(
            expected = listOf(
                face(Direction.SOUTH, 0.5, 0.0, 0.5, 1.0, 1.0, 0.5),
                face(Direction.SOUTH, 0.0, 0.0, 1.0, 0.5, 1.0, 1.0),
            ),
            actual = faces.filter { it.direction == Direction.SOUTH },
        )
    }

    @Test
    fun `forAllFaces never emits zero area faces`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
            Shapes.box(0.0, 0.5, 0.0, 0.5, 1.0, 1.0),
            Shapes.box(0.5, 0.5, 0.0, 1.0, 0.75, 0.25),
        )

        assertHasNoZeroAreaFaces(shape.collectFacesForTest())
    }

    @Test
    fun `forAllFaces handles sub-block thin shape like fence post`() {
        val fencePost = Shapes.box(0.375, 0.0, 0.375, 0.625, 1.0, 0.625)

        assertSameFaces(
            expected = listOf(
                face(Direction.DOWN, 0.375, 0.0, 0.375, 0.625, 0.0, 0.625),
                face(Direction.UP, 0.375, 1.0, 0.375, 0.625, 1.0, 0.625),
                face(Direction.NORTH, 0.375, 0.0, 0.375, 0.625, 1.0, 0.375),
                face(Direction.SOUTH, 0.375, 0.0, 0.625, 0.625, 1.0, 0.625),
                face(Direction.WEST, 0.375, 0.0, 0.375, 0.375, 1.0, 0.625),
                face(Direction.EAST, 0.625, 0.0, 0.375, 0.625, 1.0, 0.625),
            ),
            actual = fencePost.collectFacesForTest(),
        )
    }

    @Test
    fun `forAllSideFaces returns only the hit connected component`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        assertSameFaces(
            expected = listOf(face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 0.4)),
            actual = shape.collectSideFacesForTest(Direction.WEST, Vec3(0.0, 0.5, 0.2)),
        )
    }

    @Test
    fun `forAllSideFaces returns empty when hit position does not touch a component`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        assertSameFaces(
            expected = emptyList(),
            actual = shape.collectSideFacesForTest(Direction.WEST, Vec3(0.0, 0.5, 0.5)),
        )
    }

    @Test
    fun `forAllSideFaces resolves correct component when hit is exactly on face boundary`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        assertSameFaces(
            expected = listOf(face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 0.4)),
            actual = shape.collectSideFacesForTest(Direction.WEST, Vec3(0.0, 0.5, 0.4)),
        )
    }

    @Test
    fun `forAllSideFaces returns empty when hit is on correct side but behind the face`() {
        val shape = Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0)

        assertSameFaces(
            expected = emptyList(),
            actual = shape.collectSideFacesForTest(Direction.WEST, Vec3(0.25, 0.5, 0.5)),
        )
    }

    @Test
    fun `forAllSideFaces returns one full face for each cube side`() {
        for (side in Direction.entries) {
            assertSameFaces(
                expected = listOf(expectedCubeFace(side)),
                actual = Shapes.block().collectSideFacesForTest(side, centerOfCubeFace(side)),
            )
        }
    }

    @Test
    fun `forAllSideOutlineEdges returns only component perimeter without internal lines`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        assertSameLines(
            expected = listOf(
                line(0.0, 0.0, 0.0, 0.0, 0.0, 0.4),
                line(0.0, 1.0, 0.0, 0.0, 1.0, 0.4),
                line(0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
                line(0.0, 0.0, 0.4, 0.0, 1.0, 0.4),
            ),
            actual = shape.collectSideOutlineEdgesForTest(Direction.WEST, Vec3(0.0, 0.5, 0.2)),
        )
    }

    @Test
    fun `forAllSideOutlineEdges returns empty when hit position does not touch a component`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 0.4),
            Shapes.box(0.0, 0.0, 0.6, 0.5, 1.0, 1.0),
        )

        assertSameLines(
            expected = emptyList(),
            actual = shape.collectSideOutlineEdgesForTest(Direction.WEST, Vec3(0.0, 0.5, 0.5)),
        )
    }

    @Test
    fun `forAllSideOutlineEdges returns the four perimeter edges for each cube face`() {
        for (side in Direction.entries) {
            assertSameLines(
                expected = expectedCubeFacePerimeter(side),
                actual = Shapes.block().collectSideOutlineEdgesForTest(side, centerOfCubeFace(side)),
            )
        }
    }

    @Test
    fun `forAllSideOutlineEdges returns L-shaped perimeter for notch face`() {
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 0.5),
            Shapes.box(0.0, 0.0, 0.5, 0.5, 1.0, 1.0),
        )

        assertSameLines(
            expected = listOf(
                line(0.0, 0.0, 0.0, 0.0, 0.0, 1.0),
                line(0.0, 0.5, 0.0, 0.0, 0.5, 0.5),
                line(0.0, 1.0, 0.5, 0.0, 1.0, 1.0),
                line(0.0, 0.0, 0.0, 0.0, 0.5, 0.0),
                line(0.0, 0.5, 0.5, 0.0, 1.0, 0.5),
                line(0.0, 0.0, 1.0, 0.0, 1.0, 1.0),
            ),
            actual = shape.collectSideOutlineEdgesForTest(Direction.WEST, Vec3(0.0, 0.25, 0.25)),
        )
    }

    private fun expectedCubeFace(side: Direction): FaceSpec = when (side) {
        Direction.DOWN -> face(Direction.DOWN, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0)
        Direction.UP -> face(Direction.UP, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0)
        Direction.NORTH -> face(Direction.NORTH, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0)
        Direction.SOUTH -> face(Direction.SOUTH, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0)
        Direction.WEST -> face(Direction.WEST, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0)
        Direction.EAST -> face(Direction.EAST, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    }

    private fun centerOfCubeFace(side: Direction): Vec3 = when (side) {
        Direction.DOWN -> Vec3(0.5, 0.0, 0.5)
        Direction.UP -> Vec3(0.5, 1.0, 0.5)
        Direction.NORTH -> Vec3(0.5, 0.5, 0.0)
        Direction.SOUTH -> Vec3(0.5, 0.5, 1.0)
        Direction.WEST -> Vec3(0.0, 0.5, 0.5)
        Direction.EAST -> Vec3(1.0, 0.5, 0.5)
    }

    private fun expectedCubeFacePerimeter(side: Direction): List<LineSpec> = when (side) {
        Direction.DOWN -> listOf(
            line(0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
            line(1.0, 0.0, 0.0, 1.0, 0.0, 1.0),
            line(1.0, 0.0, 1.0, 0.0, 0.0, 1.0),
            line(0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
        )

        Direction.UP -> listOf(
            line(0.0, 1.0, 0.0, 1.0, 1.0, 0.0),
            line(1.0, 1.0, 0.0, 1.0, 1.0, 1.0),
            line(1.0, 1.0, 1.0, 0.0, 1.0, 1.0),
            line(0.0, 1.0, 1.0, 0.0, 1.0, 0.0),
        )

        Direction.NORTH -> listOf(
            line(0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
            line(1.0, 0.0, 0.0, 1.0, 1.0, 0.0),
            line(1.0, 1.0, 0.0, 0.0, 1.0, 0.0),
            line(0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
        )

        Direction.SOUTH -> listOf(
            line(0.0, 0.0, 1.0, 1.0, 0.0, 1.0),
            line(1.0, 0.0, 1.0, 1.0, 1.0, 1.0),
            line(1.0, 1.0, 1.0, 0.0, 1.0, 1.0),
            line(0.0, 1.0, 1.0, 0.0, 0.0, 1.0),
        )

        Direction.WEST -> listOf(
            line(0.0, 0.0, 0.0, 0.0, 0.0, 1.0),
            line(0.0, 0.0, 1.0, 0.0, 1.0, 1.0),
            line(0.0, 1.0, 1.0, 0.0, 1.0, 0.0),
            line(0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
        )

        Direction.EAST -> listOf(
            line(1.0, 0.0, 0.0, 1.0, 0.0, 1.0),
            line(1.0, 0.0, 1.0, 1.0, 1.0, 1.0),
            line(1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
            line(1.0, 1.0, 0.0, 1.0, 0.0, 0.0),
        )
    }
}
