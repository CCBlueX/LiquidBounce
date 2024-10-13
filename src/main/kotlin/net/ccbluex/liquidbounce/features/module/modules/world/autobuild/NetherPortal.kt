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
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class NetherPortal(val origin: BlockPos, val down: Boolean, val direction: Direction, rotated: Direction)
    : QuickImports {

    val blocks = arrayOf(
        origin.up().up().up().up(), origin.offset(rotated).up().up().up().up(),

        origin.offset(rotated.opposite).up().up().up(), origin.offset(rotated).offset(rotated).up().up().up(),
        origin.offset(rotated.opposite).up().up(), origin.offset(rotated).offset(rotated).up().up(),
        origin.offset(rotated.opposite).up(), origin.offset(rotated).offset(rotated).up(),

        origin, origin.offset(rotated)
    )
    private val requiredEmpty = arrayOf(
        origin.up().up().up(), origin.offset(rotated).up().up().up(),
        origin.up().up(), origin.offset(rotated).up().up(),
        origin.up(), origin.offset(rotated).up()
    )
    private val edgeBlocks = arrayOf(
        origin.offset(rotated.opposite).up().up().up().up(), origin.offset(rotated).offset(rotated).up().up().up().up(),
        origin.offset(rotated.opposite), origin.offset(rotated).offset(rotated)
    )
    val ignitePos: BlockPos = origin.up()
    var score = 0

    fun calculateScore() {
        // there can't be blocks inside the portal
        if (requiredEmpty.any { !world.isAir(it) }) {
            score = -1
            return
        }

        blocks.forEach {
            val blockState = world.getBlockState(it)
            if (blockState.block == Blocks.OBSIDIAN) {
                score += 3
            } else if (!blockState.isReplaceable) {
                // a block is not obsidian and not replaceable, making the portal invalid
                score = -1
                return
            }
        }

        // might not need support blocks
        edgeBlocks.forEach {
           if (!world.isAir(it)) {
                score += 4
           }
        }

        // entering doesn't require jumping
        if (down) {
            score += 1
        }

        if (player.movementDirection == direction) {
            score += 10
        }
    }

    fun isValid(): Boolean = score != -1

}
