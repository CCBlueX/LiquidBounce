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

package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.minecraft.block.BambooBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.CactusBlock
import net.minecraft.block.CocoaBlock
import net.minecraft.block.CropBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.Fertilizable
import net.minecraft.block.KelpPlantBlock
import net.minecraft.block.NetherWartBlock
import net.minecraft.block.PumpkinBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.block.StemBlock
import net.minecraft.block.SugarCaneBlock
import net.minecraft.block.SweetBerryBushBlock
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos

private const val NETHER_WART_MAX_AGE = 3
private const val COCOA_MAX_AGE = 2
//    private const val SWEET_BERRY_BUSH_MAX_AGE = 3 TODO: right click it

private inline fun <reified T : Block> isAboveLast(pos: BlockPos): Boolean {
    return pos.down().getBlock() is T && pos.down(2).getBlock() !is T
}

/**
 * @see Fertilizable
 */
internal fun BlockPos.canUseBoneMeal(state: BlockState): Boolean {
    return when (val block = state.block) {
        is CropBlock, is StemBlock, is CocoaBlock, is SweetBerryBushBlock ->
            block.isFertilizable(world, this, state)
        else -> false
    }
}

/**
 * Check if [this@shouldBeDestroyed] with [state] is ready for harvest
 */
internal fun BlockPos.readyForHarvest(state: BlockState): Boolean {
    return when (val block = state.block) {
        is PumpkinBlock -> true
        Blocks.MELON -> true
        is CropBlock -> block.isMature(state)
        is NetherWartBlock -> state.get(NetherWartBlock.AGE) >= NETHER_WART_MAX_AGE
        is CocoaBlock -> state.get(CocoaBlock.AGE) >= COCOA_MAX_AGE
        is SugarCaneBlock -> isAboveLast<SugarCaneBlock>(this)
        is CactusBlock -> isAboveLast<CactusBlock>(this)
        is KelpPlantBlock -> isAboveLast<KelpPlantBlock>(this)
        is BambooBlock -> isAboveLast<BambooBlock>(this)
        else -> false
    }
}

internal val itemsForFarmland = arrayOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO)
internal val itemsForSoulSand = arrayOf(Items.NETHER_WART)

internal fun getAvailableSlotForBlock(blockState: BlockState) =
    when (blockState.block) {
        is FarmlandBlock -> Slots.OffhandWithHotbar.findClosestSlot(items = itemsForFarmland)
        is SoulSandBlock -> Slots.OffhandWithHotbar.findClosestSlot(items = itemsForSoulSand)
        else -> null
    }
