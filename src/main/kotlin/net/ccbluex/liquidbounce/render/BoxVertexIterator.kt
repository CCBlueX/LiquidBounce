/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

enum class BoxVertexIterator {
    FACE {
        override fun forEachVertex(box: Box, consumer: Consumer) {
            box.forEachFaceVertex(consumer::invoke)
        }

        override fun sideMask(side: Direction): Int = when (side) {
            Direction.DOWN -> 0x00_000F.inv()
            Direction.UP -> 0x00_00F0.inv()
            Direction.NORTH -> 0x00_0F00.inv()
            Direction.EAST -> 0x00_F000.inv()
            Direction.SOUTH -> 0x0F_0000.inv()
            Direction.WEST -> 0xF0_0000.inv()
        }
    },
    OUTLINE {
        override fun forEachVertex(box: Box, consumer: Consumer) {
            box.forEachOutlineVertex(consumer::invoke)
        }

        override fun sideMask(side: Direction): Int = when (side) {
            Direction.DOWN -> 0b0000_0000_0000_0000_1111_1111.inv()
            Direction.UP -> 0b1111_1111_0000_0000_0000_0000.inv()
            Direction.NORTH -> 0b0000_0011_0000_1111_0000_0011.inv()
            Direction.EAST -> 0b0000_1100_0011_1100_0000_1100.inv()
            Direction.SOUTH -> 0b0011_0000_1111_0000_0011_0000.inv()
            Direction.WEST -> 0b1100_0000_1100_0011_1100_0000.inv()
        }
    };

    /**
     * For Java and JS usage.
     */
    abstract fun forEachVertex(box: Box, consumer: Consumer)

    /**
     * For [drawBox].
     */
    abstract fun sideMask(side: Direction): Int

    fun interface Consumer {
        operator fun invoke(index: Int, x: Double, y: Double, z: Double)
    }
}

inline fun Box.forEachFaceVertex(fn: (index: Int, x: Double, y: Double, z: Double) -> Unit) {
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

inline fun Box.forEachOutlineVertex(fn: (index: Int, x: Double, y: Double, z: Double) -> Unit) {
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
