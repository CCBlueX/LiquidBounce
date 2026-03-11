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
package net.ccbluex.liquidbounce.utils.block.bed

import it.unimi.dsi.fastutil.ints.IntIntMutablePair
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3

/**
 * Represents a bed state.
 */
@JvmRecord
data class BedState(
    val block: BedBlock,
    val trackedBlockPos: BlockPos,
    val pos: Vec3,
    val surroundingBlocks: List<SurroundingBlock>,
    val compactSurroundingBlocks: List<SurroundingBlock> = run {
        val map = Reference2ObjectOpenHashMap<Block, IntIntMutablePair>()

        surroundingBlocks.forEach { surrounding ->
            val pair = map.computeIfAbsent(surrounding.block) { IntIntMutablePair(0, 0) }
            pair.left(pair.leftInt() + surrounding.count)
            pair.right(minOf(pair.rightInt(), surrounding.layer))
        }

        map.map { SurroundingBlock(block = it.key, count = it.value.leftInt(), layer = it.value.rightInt()) }
    },
)
