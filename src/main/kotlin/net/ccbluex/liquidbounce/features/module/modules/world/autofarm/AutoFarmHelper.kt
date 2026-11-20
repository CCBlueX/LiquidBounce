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

package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.BambooStalkBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealSource
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.CactusBlock
import net.minecraft.world.level.block.CocoaBlock
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.KelpPlantBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.PumpkinBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.SugarCaneBlock
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * @see BonemealableBlock
 */
internal fun BlockPos.canUseBoneMeal(state: BlockState): Boolean {
    return when (val block = state.block) {
        is CropBlock, is StemBlock, is CocoaBlock, is SweetBerryBushBlock ->
            block.isValidBonemealTarget(world, this, state, BonemealSource.INTERACTION)
        else -> false
    }
}

/**
 * Whether the block grows something to harvest once ripe, see [net.ccbluex.agent.skills.farm.Crops.isRipe].
 */
private val Block.isHarvestable: Boolean
    get() = when (this) {
        is PumpkinBlock, is CropBlock, is NetherWartBlock, is CocoaBlock, is SweetBerryBushBlock,
        is SugarCaneBlock, is CactusBlock, is KelpPlantBlock, is BambooStalkBlock, Blocks.MELON -> true
        else -> false
    }

inline val BlockState.supportsCrops: Boolean get() = `is`(BlockTags.SUPPORTS_CROPS)

inline val BlockState.supportsCocoa: Boolean get() = `is`(BlockTags.SUPPORTS_COCOA)

inline val BlockState.supportsNetherWart: Boolean get() = `is`(BlockTags.SUPPORTS_NETHER_WART)

/**
 * Whether the position holding this state can ever be tracked by [AutoFarmBlockTracker].
 *
 * Air is deliberately not a candidate: a plantable position is reported through the block it
 * relies on, which this predicate matches instead.
 */
fun BlockState.isAutoFarmCandidate(): Boolean =
    block.isHarvestable ||
        block is StemBlock ||
        supportsCrops ||
        supportsNetherWart ||
        supportsCocoa
