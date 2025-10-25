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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Blocks
import net.minecraft.block.FallingBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object ScaffoldBlockItemSelection : Configurable("BlockItemSelection") {

    /**
     * A list of blocks which may not be placed (apart from the usual checks), so inv cleaner and scaffold
     * won't count them as blocks
     */
    private val disallowedBlocksToPlace by blocks(
        name = "Disallowed",
        default = ReferenceOpenHashSet.of(
            Blocks.TNT,
            Blocks.COBWEB,
            Blocks.NETHER_PORTAL,
        )
    )

    /**
     * @see [ScaffoldBlockItemSelection.isBlockUnfavourable]
     */
    private val unfavorableBlocksToPlace by blocks(
        name = "Unfavorable",
        default = ReferenceOpenHashSet.of(
            Blocks.CRAFTING_TABLE,
            Blocks.JIGSAW,
            Blocks.SMITHING_TABLE,
            Blocks.FLETCHING_TABLE,
            Blocks.ENCHANTING_TABLE,
            Blocks.CAULDRON,
            Blocks.MAGMA_BLOCK,
        )
    )

    fun isValidBlock(stack: ItemStack?): Boolean {
        if (stack == null) {
            return false
        }

        val block = stack.getBlock() ?: return false
        val defaultState = block.defaultState

        return when {
            !defaultState.isSolidSurface(world, BlockPos.ORIGIN, player, Direction.UP) -> {
                false
            }
            // We don't want to suicide...
            block is FallingBlock -> false
            else -> !disallowedBlocksToPlace.contains(block)
        }
    }

    /**
     * Special handling for unfavourable blocks (like crafting tables, slabs, etc.):
     * - [ModuleScaffold]: Unfavourable blocks are only used when there is no other option left
     * - [ModuleInventoryCleaner]: Unfavourable blocks are not used as blocks by inv-cleaner.
     */
    fun isBlockUnfavourable(stack: ItemStack): Boolean {
        val block = stack.getBlock() ?: return true
        return when {
            // We dislike slippery blocks...
            block.slipperiness > 0.6F -> true
            // We dislike soul sand and slime...
            block.velocityMultiplier < 1.0F -> true
            // We hate honey...
            block.jumpVelocityMultiplier < 1.0F -> true
            // We don't want to place bee hives, chests, spawners, etc.
            block is BlockWithEntity -> true
            // We don't like slabs etc.
            !block.defaultState.isFullCube(ModuleScaffold.mc.world!!, BlockPos.ORIGIN) -> true
            // Is there a hard coded answer?
            else -> block in unfavorableBlocksToPlace
        }
    }

}
