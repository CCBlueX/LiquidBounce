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
package net.ccbluex.liquidbounce.features.module.modules.world.autobuild

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.block.getBlockingEntities
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.level.block.Blocks

class NetherPortal(val origin: BlockPos, val down: Boolean, val direction: Direction, rotated: Direction)
    : MinecraftShortcuts {

    val frameBlocks = arrayOf(
        origin.above(4), origin.relative(rotated).above(4),

        origin.relative(rotated.opposite).above(3), origin.relative(rotated).relative(rotated).above(3),
        origin.relative(rotated.opposite).above(2), origin.relative(rotated).relative(rotated).above(2),
        origin.relative(rotated.opposite).above(), origin.relative(rotated).relative(rotated).above(),

        origin, origin.relative(rotated)
    )
    val enclosedBlocks = arrayOf(
        origin.above(3), origin.relative(rotated).above(3),
        origin.above(2), origin.relative(rotated).above(2),
        origin.above(), origin.relative(rotated).above()
    )
    private val edgeBlocks = arrayOf(
        origin.relative(rotated.opposite).above(4), origin.relative(rotated).relative(rotated).above(4),
        origin.relative(rotated.opposite), origin.relative(rotated).relative(rotated)
    )
    val ignitePos: BlockPos = origin.above()
    var score = 0

    /**
     * Scores the potential portal about how favourable it would be, to find the best place position.
     */
    fun calculateScore() {
        // there can't be blocks inside the portal
        if (enclosedBlocks.any { !world.isEmptyBlock(it) }) {
            score = -1
            return
        }

        val canDestroyCrystals = ModuleAutoBuild.placer.crystalDestroyer.enabled
        frameBlocks.forEach {
            val blockState = world.getBlockState(it)

            when {
                blockState.block == Blocks.OBSIDIAN -> score += 3

                !blockState.canBeReplaced() || !canDestroyCrystals && it.isBlockedByEntities() -> {
                    // a block that is not obsidian and not replaceable, making the portal invalid
                    score = -1
                    return
                }

                canDestroyCrystals -> {
                    val blockingEntities = it.getBlockingEntities()
                    if (blockingEntities.any { entity -> entity !is EndCrystal }) {
                        score = -1
                        return
                    } else if (blockingEntities.isNotEmpty()) {
                        score -= 10 - 2 * blockingEntities.size
                    }
                }
            }
        }

        // might not need support blocks
        edgeBlocks.forEach {
           if (!world.isEmptyBlock(it)) {
                score += 4
           } else if (it.isBlockedByEntities()) {
               score -= 1
           }
        }

        // entering doesn't require jumping
        if (down) {
            score += 1
        }

        // in the best case, we already look directly at the portal
        if (player.motionDirection == direction) {
            score += 10
        }

        score = score.coerceAtLeast(0)
    }

    /**
     * Returns a list with all the positions that should be obsidian but aren't.
     */
    fun confirmPlacements(): List<BlockPos> {
        return frameBlocks.filter {
            val blockState = world.getBlockState(it)
            blockState.block != Blocks.OBSIDIAN && blockState.canBeReplaced()
        }
    }

    /**
     * Whether the score is `-1`, meaning we can't build this portal without additional actions such as breaking.
     */
    fun isValid(): Boolean = score != -1

}
