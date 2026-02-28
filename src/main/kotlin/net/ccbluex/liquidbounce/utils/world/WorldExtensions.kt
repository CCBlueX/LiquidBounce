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

package net.ccbluex.liquidbounce.utils.world

import net.ccbluex.fastutil.asObjectList
import net.minecraft.core.BlockPos
import net.minecraft.world.attribute.BedRule
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection

/**
 * @return if water and ice evaporates in this world (e.g. nether)
 */
val Level.waterEvaporates: Boolean
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)

val Level.bedRule: BedRule
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE)

val Level.respawnAnchorWorks: Boolean
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)

/**
 * Returns the loaded section slice from section 0 through [LevelChunk.highestFilledSectionIndex].
 */
val LevelChunk.filledSections: List<LevelChunkSection>
    get() = this.sections.asObjectList(offset = 0, length = this.highestFilledSectionIndex + 1)

/**
 * Iterates all blocks in a specific section index and exposes world-space block positions.
 *
 * The [mutable] instance is reused across callbacks for allocation-free chunk scanning.
 *
 * @see LevelChunk.getBlockState
 */
inline fun LevelChunk.forEachSectionBlock(
    sectionIndex: Int,
    mutable: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    action: (BlockPos, BlockState) -> Unit,
) {
    val section = this.getSection(sectionIndex)
    val startX = this.pos.minBlockX
    val startY = this.sectionBottonY(sectionIndex)
    val startZ = this.pos.minBlockZ
    section.forEachBlock { localX, localY, localZ, state ->
        action(mutable.set(startX or localX, startY or localY, startZ or localZ), state)
    }
}

/**
 * Iterates all 4096 block states in a section and provides local section coordinates (0..15).
 *
 * @see LevelChunk.getBlockState
 */
inline fun LevelChunkSection.forEachBlock(action: (localX: Int, localY: Int, localZ: Int, BlockState) -> Unit) {
    for (localY in 0..15) {
        for (localZ in 0..15) {
            for (localX in 0..15) {
                val blockState = this.getBlockState(localX, localY, localZ)
                action(localX, localY, localZ, blockState)
            }
        }
    }
}

/**
 * Converts a section index to the section base world Y (multiple of 16).
 *
 * `index == (y >> 4) - (bottomY >> 4)`
 */
fun LevelChunk.sectionBottonY(index: Int): Int = (index + (this.minY shr 4)) shl 4
