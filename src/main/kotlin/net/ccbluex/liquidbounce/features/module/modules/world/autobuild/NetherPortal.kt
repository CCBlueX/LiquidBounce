/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.autobuild

import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class NetherPortal(val origin: BlockPos, val down: Boolean, val direction: Direction, rotated: Direction)
    : QuickImports {

    val frameBlocks = arrayOf(
        origin.up(4), origin.offset(rotated).up(4),

        origin.offset(rotated.opposite).up(3), origin.offset(rotated).offset(rotated).up(3),
        origin.offset(rotated.opposite).up(2), origin.offset(rotated).offset(rotated).up(2),
        origin.offset(rotated.opposite).up(), origin.offset(rotated).offset(rotated).up(),

        origin, origin.offset(rotated)
    )
    val enclosedBlocks = arrayOf(
        origin.up(3), origin.offset(rotated).up(3),
        origin.up(2), origin.offset(rotated).up(2),
        origin.up(), origin.offset(rotated).up()
    )
    private val edgeBlocks = arrayOf(
        origin.offset(rotated.opposite).up(4), origin.offset(rotated).offset(rotated).up(4),
        origin.offset(rotated.opposite), origin.offset(rotated).offset(rotated)
    )
    val ignitePos: BlockPos = origin.up()
    var score = 0

    /**
     * Scores the potential portal about how favourable it would be, to find the best place position.
     */
    fun calculateScore() {
        // there can't be blocks inside the portal
        if (enclosedBlocks.any { !world.isAir(it) }) {
            score = -1
            return
        }

        frameBlocks.forEach {
            val blockState = world.getBlockState(it)
            if (blockState.block == Blocks.OBSIDIAN) {
                score += 3
            } else if (!blockState.isReplaceable || it.isBlockedByEntities()) {
                // a block is not obsidian and not replaceable, making the portal invalid
                score = -1
                return
            }
        }

        // might not need support blocks
        edgeBlocks.forEach {
           if (!world.isAir(it)) {
                score += 4
           } else if (it.isBlockedByEntities()) {
               score -= 1
               score = score.coerceAtLeast(0)
           }
        }

        // entering doesn't require jumping
        if (down) {
            score += 1
        }

        // in the best case, we already look directly at the portal
        if (player.movementDirection == direction) {
            score += 10
        }
    }

    /**
     * Returns a list with all the positions that should be obsidian but aren't.
     */
    fun confirmPlacements(): List<BlockPos> {
        return frameBlocks.filter {
            val blockState = world.getBlockState(it)
            blockState.block != Blocks.OBSIDIAN && blockState.isReplaceable
        }
    }

    /**
     * Whether the score is `-1`, meaning we can't build this portal without additional actions such as breaking.
     */
    fun isValid(): Boolean = score != -1

}
