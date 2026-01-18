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

package net.ccbluex.liquidbounce.render

import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB

enum class BoxVertexIterator {
    FACE {
        override fun forEachVertex(box: AABB, consumer: BoxVertexConsumer) {
            box.forEachFaceVertex(consumer::invoke)
        }

        override fun sideMask(side: Direction): Int = when (side) {
            Direction.DOWN -> 0x00_000F
            Direction.UP -> 0x00_00F0
            Direction.NORTH -> 0x00_0F00
            Direction.EAST -> 0x00_F000
            Direction.SOUTH -> 0x0F_0000
            Direction.WEST -> 0xF0_0000
        }
    },
    OUTLINE {
        override fun forEachVertex(box: AABB, consumer: BoxVertexConsumer) {
            box.forEachOutlineVertex(consumer::invoke)
        }

        override fun sideMask(side: Direction): Int = when (side) {
            Direction.DOWN -> 0b0000_0000_0000_0000_1111_1111
            Direction.UP -> 0b1111_1111_0000_0000_0000_0000
            Direction.NORTH -> 0b0000_0011_0000_1111_0000_0011
            Direction.EAST -> 0b0000_1100_0011_1100_0000_1100
            Direction.SOUTH -> 0b0011_0000_1111_0000_0011_0000
            Direction.WEST -> 0b1100_0000_1100_0011_1100_0000
        }
    };

    /**
     * For Java and JS usage.
     */
    abstract fun forEachVertex(box: AABB, consumer: BoxVertexConsumer)

    /**
     * For [drawBox].
     */
    abstract fun sideMask(side: Direction): Int

    fun sideMask(sides: Iterable<Direction>): Int {
        var bits = 0
        for (side in sides) {
            bits = bits or sideMask(side)
        }
        return bits
    }

    fun interface BoxVertexConsumer {
        operator fun invoke(index: Int, x: Double, y: Double, z: Double)
    }
}

inline fun AABB.forEachFaceVertex(fn: (index: Int, x: Double, y: Double, z: Double) -> Unit) {
    var i = 0
    // down
    fn(i++, minX, minY, minZ)
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, minY, maxZ)
    fn(i++, minX, minY, maxZ)

    // up
    fn(i++, minX, maxY, minZ)
    fn(i++, minX, maxY, maxZ)
    fn(i++, maxX, maxY, maxZ)
    fn(i++, maxX, maxY, minZ)

    // north
    fn(i++, minX, minY, minZ)
    fn(i++, minX, maxY, minZ)
    fn(i++, maxX, maxY, minZ)
    fn(i++, maxX, minY, minZ)

    // east
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, maxY, minZ)
    fn(i++, maxX, maxY, maxZ)
    fn(i++, maxX, minY, maxZ)

    // south
    fn(i++, minX, minY, maxZ)
    fn(i++, maxX, minY, maxZ)
    fn(i++, maxX, maxY, maxZ)
    fn(i++, minX, maxY, maxZ)

    // west
    fn(i++, minX, minY, minZ)
    fn(i++, minX, minY, maxZ)
    fn(i++, minX, maxY, maxZ)
    fn(i++, minX, maxY, minZ)

    // i == 24
}

inline fun AABB.forEachOutlineVertex(fn: (index: Int, x: Double, y: Double, z: Double) -> Unit) {
    var i = 0
    // down north
    fn(i++, minX, minY, minZ)
    fn(i++, maxX, minY, minZ)

    // down east
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, minY, maxZ)

    // down south
    fn(i++, maxX, minY, maxZ)
    fn(i++, minX, minY, maxZ)

    // down west
    fn(i++, minX, minY, maxZ)
    fn(i++, minX, minY, minZ)

    // north west
    fn(i++, minX, minY, minZ)
    fn(i++, minX, maxY, minZ)

    // north east
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, maxY, minZ)

    // south east
    fn(i++, maxX, minY, maxZ)
    fn(i++, maxX, maxY, maxZ)

    // south west
    fn(i++, minX, minY, maxZ)
    fn(i++, minX, maxY, maxZ)

    // up north
    fn(i++, minX, maxY, minZ)
    fn(i++, maxX, maxY, minZ)

    // up east
    fn(i++, maxX, maxY, minZ)
    fn(i++, maxX, maxY, maxZ)

    // up south
    fn(i++, maxX, maxY, maxZ)
    fn(i++, minX, maxY, maxZ)

    // up west
    fn(i++, minX, maxY, maxZ)
    fn(i++, minX, maxY, minZ)

    // i == 24
}
