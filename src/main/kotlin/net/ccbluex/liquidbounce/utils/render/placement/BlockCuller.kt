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

package net.ccbluex.liquidbounce.utils.render.placement

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.DOWN
import net.minecraft.core.Direction.EAST
import net.minecraft.core.Direction.NORTH
import net.minecraft.core.Direction.SOUTH
import net.minecraft.core.Direction.UP
import net.minecraft.core.Direction.WEST

private const val FACE_DOWN = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3)
private const val FACE_UP = (1 shl 4) or (1 shl 5) or (1 shl 6) or (1 shl 7)
private const val FACE_NORTH = (1 shl 8) or (1 shl 9) or (1 shl 10) or (1 shl 11)
private const val FACE_EAST = (1 shl 12) or (1 shl 13) or (1 shl 14) or (1 shl 15)
private const val FACE_SOUTH = (1 shl 16) or (1 shl 17) or (1 shl 18) or (1 shl 19)
private const val FACE_WEST = (1 shl 20) or (1 shl 21) or (1 shl 22) or (1 shl 23)

// TODO check whether the Boxes actually touch
@JvmInline
internal value class BlockCuller(val owner: Owner) {

    interface Owner {
        operator fun contains(pos: Long): Boolean
    }

    private fun contains(pos: Long, direction: Direction) =
        BlockPos.offset(pos, direction) in owner

    /**
     * The face vertices of the block at [pos] that have no neighbor in front of them.
     */
    fun getCullData(pos: Long): Int {
        var faces = 0

        faces = cullSide(faces, contains(pos, EAST), FACE_EAST)
        faces = cullSide(faces, contains(pos, WEST), FACE_WEST)
        faces = cullSide(faces, contains(pos, UP), FACE_UP)
        faces = cullSide(faces, contains(pos, DOWN), FACE_DOWN)
        faces = cullSide(faces, contains(pos, SOUTH), FACE_SOUTH)
        faces = cullSide(faces, contains(pos, NORTH), FACE_NORTH)

        return faces
    }

    /**
     * Applies a mask to the current data if either [directionPresent] is `false`.
     *
     * This will result in the face only being visible if it's on the outside of multiple blocks.
     *
     * @return The updated [currentData]
     */
    private fun cullSide(currentData: Int, directionPresent: Boolean, mask: Int): Int {
        return if (!directionPresent) {
            currentData or mask
        } else {
            currentData
        }
    }
}
