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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.fastutil.Pool.Companion.use
import net.ccbluex.liquidbounce.features.command.commands.module.CommandXRay
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS
import net.minecraft.world.level.block.Blocks.ANVIL
import net.minecraft.world.level.block.Blocks.BARREL
import net.minecraft.world.level.block.Blocks.BEACON
import net.minecraft.world.level.block.Blocks.BLACK_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.BLAST_FURNACE
import net.minecraft.world.level.block.Blocks.BLUE_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.BOOKSHELF
import net.minecraft.world.level.block.Blocks.BREWING_STAND
import net.minecraft.world.level.block.Blocks.BROWN_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE
import net.minecraft.world.level.block.Blocks.CAULDRON
import net.minecraft.world.level.block.Blocks.CHAIN_COMMAND_BLOCK
import net.minecraft.world.level.block.Blocks.CHEST
import net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL
import net.minecraft.world.level.block.Blocks.CLAY
import net.minecraft.world.level.block.Blocks.COAL_BLOCK
import net.minecraft.world.level.block.Blocks.COAL_ORE
import net.minecraft.world.level.block.Blocks.COMMAND_BLOCK
import net.minecraft.world.level.block.Blocks.COMPOSTER
import net.minecraft.world.level.block.Blocks.COPPER_BLOCK
import net.minecraft.world.level.block.Blocks.COPPER_ORE
import net.minecraft.world.level.block.Blocks.CRAFTING_TABLE
import net.minecraft.world.level.block.Blocks.CYAN_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL
import net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE
import net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE
import net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK
import net.minecraft.world.level.block.Blocks.DIAMOND_ORE
import net.minecraft.world.level.block.Blocks.DISPENSER
import net.minecraft.world.level.block.Blocks.DRAGON_EGG
import net.minecraft.world.level.block.Blocks.DROPPER
import net.minecraft.world.level.block.Blocks.EMERALD_BLOCK
import net.minecraft.world.level.block.Blocks.EMERALD_ORE
import net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE
import net.minecraft.world.level.block.Blocks.ENDER_CHEST
import net.minecraft.world.level.block.Blocks.END_PORTAL
import net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME
import net.minecraft.world.level.block.Blocks.FIRE
import net.minecraft.world.level.block.Blocks.FLETCHING_TABLE
import net.minecraft.world.level.block.Blocks.FLOWER_POT
import net.minecraft.world.level.block.Blocks.FURNACE
import net.minecraft.world.level.block.Blocks.GOLD_BLOCK
import net.minecraft.world.level.block.Blocks.GOLD_ORE
import net.minecraft.world.level.block.Blocks.GRAY_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.GREEN_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.GRINDSTONE
import net.minecraft.world.level.block.Blocks.HOPPER
import net.minecraft.world.level.block.Blocks.IRON_BLOCK
import net.minecraft.world.level.block.Blocks.IRON_ORE
import net.minecraft.world.level.block.Blocks.JUKEBOX
import net.minecraft.world.level.block.Blocks.LAPIS_BLOCK
import net.minecraft.world.level.block.Blocks.LAPIS_ORE
import net.minecraft.world.level.block.Blocks.LAVA
import net.minecraft.world.level.block.Blocks.LAVA_CAULDRON
import net.minecraft.world.level.block.Blocks.LECTERN
import net.minecraft.world.level.block.Blocks.LIGHT_BLUE_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.LIGHT_GRAY_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.LIME_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.LODESTONE
import net.minecraft.world.level.block.Blocks.LOOM
import net.minecraft.world.level.block.Blocks.MAGENTA_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK
import net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE
import net.minecraft.world.level.block.Blocks.NETHER_PORTAL
import net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE
import net.minecraft.world.level.block.Blocks.ORANGE_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.PINK_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.PURPLE_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK
import net.minecraft.world.level.block.Blocks.RAW_COPPER_BLOCK
import net.minecraft.world.level.block.Blocks.RAW_GOLD_BLOCK
import net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK
import net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK
import net.minecraft.world.level.block.Blocks.REDSTONE_ORE
import net.minecraft.world.level.block.Blocks.RED_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.REPEATING_COMMAND_BLOCK
import net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR
import net.minecraft.world.level.block.Blocks.SHULKER_BOX
import net.minecraft.world.level.block.Blocks.SMITHING_TABLE
import net.minecraft.world.level.block.Blocks.SMOKER
import net.minecraft.world.level.block.Blocks.SPAWNER
import net.minecraft.world.level.block.Blocks.STONECUTTER
import net.minecraft.world.level.block.Blocks.TNT
import net.minecraft.world.level.block.Blocks.TRAPPED_CHEST
import net.minecraft.world.level.block.Blocks.WATER
import net.minecraft.world.level.block.Blocks.WATER_CAULDRON
import net.minecraft.world.level.block.Blocks.WHITE_SHULKER_BOX
import net.minecraft.world.level.block.Blocks.YELLOW_SHULKER_BOX
import net.minecraft.world.level.block.state.BlockState

/**
 * XRay module
 *
 * Allows you to see ores through walls.
 *
 * Command: [CommandXRay]
 */
object ModuleXRay : ClientModule("XRay", ModuleCategories.RENDER) {

    // Lighting of blocks through walls
    val fullBright by boolean("FullBright", true)

    // Only render blocks with non-solid blocks around
    private val exposedOnly by boolean("ExposedOnly", false)
        .onChanged(::valueChangedReload)

    private val defaultBlocks = arrayOf(
        // Overworld ores
        COAL_ORE,
        COPPER_ORE,
        DIAMOND_ORE,
        EMERALD_ORE,
        GOLD_ORE,
        IRON_ORE,
        LAPIS_ORE,
        REDSTONE_ORE,

        // Overworld ores (deepslate variants)
        DEEPSLATE_COAL_ORE,
        DEEPSLATE_COPPER_ORE,
        DEEPSLATE_DIAMOND_ORE,
        DEEPSLATE_EMERALD_ORE,
        DEEPSLATE_GOLD_ORE,
        DEEPSLATE_IRON_ORE,
        DEEPSLATE_LAPIS_ORE,
        DEEPSLATE_REDSTONE_ORE,

        // Overworld mineral blocks
        COAL_BLOCK,
        COPPER_BLOCK,
        DIAMOND_BLOCK,
        EMERALD_BLOCK,
        GOLD_BLOCK,
        IRON_BLOCK,
        LAPIS_BLOCK,
        REDSTONE_BLOCK,

        // Overworld raw mineral blocks
        RAW_COPPER_BLOCK,
        RAW_GOLD_BLOCK,
        RAW_IRON_BLOCK,

        // Nether ores
        ANCIENT_DEBRIS,
        NETHER_GOLD_ORE,
        NETHER_QUARTZ_ORE,

        // Nether material blocks
        NETHERITE_BLOCK,
        QUARTZ_BLOCK,

        // Storage blocks
        CHEST,
        DISPENSER,
        DROPPER,
        ENDER_CHEST,
        HOPPER,
        TRAPPED_CHEST,

        // Storage blocks (shulker box variants)
        BLACK_SHULKER_BOX,
        BLUE_SHULKER_BOX,
        BROWN_SHULKER_BOX,
        CYAN_SHULKER_BOX,
        GRAY_SHULKER_BOX,
        GREEN_SHULKER_BOX,
        LIGHT_BLUE_SHULKER_BOX,
        LIGHT_GRAY_SHULKER_BOX,
        LIME_SHULKER_BOX,
        MAGENTA_SHULKER_BOX,
        ORANGE_SHULKER_BOX,
        PINK_SHULKER_BOX,
        PURPLE_SHULKER_BOX,
        RED_SHULKER_BOX,
        SHULKER_BOX,
        WHITE_SHULKER_BOX,
        YELLOW_SHULKER_BOX,

        // Utility blocks
        BEACON,
        CRAFTING_TABLE,
        ENCHANTING_TABLE,
        FURNACE,
        FLOWER_POT,
        JUKEBOX,
        LODESTONE,
        RESPAWN_ANCHOR,

        // Utility blocks (anvil variants)
        ANVIL,
        CHIPPED_ANVIL,
        DAMAGED_ANVIL,

        // Utility blocks (job variants)
        BARREL,
        BLAST_FURNACE,
        BREWING_STAND,
        CARTOGRAPHY_TABLE,
        COMPOSTER,
        FLETCHING_TABLE,
        GRINDSTONE,
        LECTERN,
        LOOM,
        SMITHING_TABLE,
        SMOKER,
        STONECUTTER,

        // Utility blocks (job variants (cauldron variants))
        CAULDRON,
        LAVA_CAULDRON,
        WATER_CAULDRON,

        // Liquids
        LAVA,
        WATER,

        // Portals
        END_PORTAL,
        END_PORTAL_FRAME,
        NETHER_PORTAL,

        // Command block variants
        CHAIN_COMMAND_BLOCK,
        COMMAND_BLOCK,
        REPEATING_COMMAND_BLOCK,

        // Remaining blocks
        BOOKSHELF,
        CLAY,
        DRAGON_EGG,
        FIRE,
        SPAWNER,
        TNT
    )

    // Set of blocks that will not be excluded
    val blocks: MutableSet<Block> by blocks(
        "Blocks",
        blockSortedSetOf(blocks = defaultBlocks)
    ).onChanged(::valueChangedReload)

    /**
     * Checks if the block should be rendered or not.
     * This can be used to exclude blocks that should not be rendered.
     * Also features an option to only render blocks that are exposed to air.
     */
    fun shouldRender(blockState: BlockState, blockPos: BlockPos) = when {
        blockState.block !in blocks -> false

        exposedOnly -> Pools.MutableBlockPos.use { pos ->
            Direction.entries.any {
                pos.set(blockPos).move(it.unitVec3i).getState()?.isRedstoneConductor(world, pos) == false
            }
        }

        else -> true
    }

    fun shouldRender(state: BlockState, otherState: BlockState, side: Direction) = when {
        state.block !in blocks -> false

        exposedOnly -> !state.skipRendering(otherState, side)

        else -> true
    }

    /**
     * Resets the block list to the default values
     */
    fun applyDefaults() {
        blocks.clear()
        blocks.addAll(defaultBlocks)
    }

    override fun onEnabled() {
        mc.levelRenderer.allChanged()
    }

    override fun onDisabled() {
        mc.levelRenderer.allChanged()
    }

    @Suppress("UNUSED_PARAMETER")
    fun valueChangedReload(it: Any) {
        if (!running) return

        mc.execute {
            // Reload world renderer on block list change
            mc.levelRenderer.allChanged()
        }
    }

}
