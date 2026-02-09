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

import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.BambooStalkBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.CactusBlock
import net.minecraft.world.level.block.CocoaBlock
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.KelpPlantBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.PumpkinBlock
import net.minecraft.world.level.block.SoulSandBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.SugarCaneBlock
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.state.BlockState

private inline fun <reified T : Block> isAboveLast(pos: BlockPos): Boolean {
    return pos.below().getBlock() is T && pos.below(2).getBlock() !is T
}

/**
 * @see BonemealableBlock
 */
internal fun BlockPos.canUseBoneMeal(state: BlockState): Boolean {
    return when (val block = state.block) {
        is CropBlock, is StemBlock, is CocoaBlock, is SweetBerryBushBlock ->
            block.isValidBonemealTarget(world, this, state)
        else -> false
    }
}

enum class HarvestAction {
    /**
     * Break the block to harvest it. e.g. Melon, Pumpkin, Cactus
     */
    BREAK,
    /**
     * Use the item in hand to harvest the block. e.g. Sweet Berry Bush
     */
    USE,
}

/**
 * Get the harvest action for the block. The block itself might be not ready for harvest!
 * Call [BlockPos.readyForHarvest] before performing the harvest action.
 */
val Block.harvestAction: HarvestAction?
    get() = when (this) {
        is PumpkinBlock, is CropBlock, is NetherWartBlock, is CocoaBlock,
        is SugarCaneBlock, is CactusBlock, is KelpPlantBlock, is BambooStalkBlock, Blocks.MELON -> HarvestAction.BREAK
        is SweetBerryBushBlock -> HarvestAction.USE
        else -> null
    }

/**
 * Check if [this@shouldBeDestroyed] with [state] is ready for harvest
 */
fun BlockPos.readyForHarvest(state: BlockState): Boolean {
    return when (val block = state.block) {
        is PumpkinBlock -> true
        Blocks.MELON -> true
        is CropBlock -> block.isMaxAge(state)
        is NetherWartBlock -> state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE
        is CocoaBlock -> state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE
        is SweetBerryBushBlock -> state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE
        is SugarCaneBlock -> isAboveLast<SugarCaneBlock>(this)
        is CactusBlock -> isAboveLast<CactusBlock>(this)
        is KelpPlantBlock -> isAboveLast<KelpPlantBlock>(this)
        is BambooStalkBlock -> isAboveLast<BambooStalkBlock>(this)
        else -> false
    }
}

inline val BlockState.supportsCrops: Boolean get() = block is FarmBlock

inline val BlockState.supportsCocoa: Boolean get() = `is`(BlockTags.JUNGLE_LOGS)

inline val BlockState.supportsNetherWart: Boolean get() = block is SoulSandBlock
