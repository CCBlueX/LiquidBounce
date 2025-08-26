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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.commands.module.CommandXRay
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * XRay module
 *
 * Allows you to see ores through walls.
 *
 * Command: [CommandXRay]
 */
object ModuleXRay : ClientModule("XRay", Category.RENDER) {

    // Lighting of blocks through walls
    val fullBright by boolean("FullBright", true)

    // Only render blocks with non-solid blocks around
    private val exposedOnly by boolean("ExposedOnly", false)
        .onChanged(::valueChangedReload)

    private val defaultBlocks = setOf(
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
    val blocks by blocks(
        "Blocks",
        defaultBlocks.toMutableSet()
    ).onChanged(::valueChangedReload)

    /**
     * Checks if the block should be rendered or not.
     * This can be used to exclude blocks that should not be rendered.
     * Also features an option to only render blocks that are exposed to air.
     */
    fun shouldRender(blockState: BlockState, blockPos: BlockPos) = when {
        blockState.block !in blocks -> false

        exposedOnly -> Direction.entries.any {
            blockPos.add(it.vector)?.let { pos -> pos.getState()?.isSolidBlock(world, pos) } == false
        }

        else -> true
    }

    fun shouldRender(state: BlockState, otherState: BlockState, side: Direction) = when {
        state.block !in blocks -> false

        exposedOnly -> !state.isSideInvisible(otherState, side)

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
        mc.worldRenderer.reload()
    }

    override fun onDisabled() {
        mc.worldRenderer.reload()
    }

    @Suppress("UNUSED_PARAMETER")
    fun valueChangedReload(it: Any) {
        RenderSystem.recordRenderCall {
            // Reload world renderer on block list change
            mc.worldRenderer.reload()
        }
    }

}
