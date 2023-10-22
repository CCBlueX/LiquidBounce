/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Blocks.*

/**
 * XRay module
 *
 * Allows you to see ores through walls.
 */

object ModuleXRay : Module("XRay", Category.RENDER) {

    // Lighting of blocks through walls
    val fullBright by boolean("FullBright", true)

    private val deafultBlocks = mutableSetOf(
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
        deafultBlocks
    )

    fun resetBlocks() {
        blocks.clear()
        blocks.addAll(deafultBlocks)
    }

    override fun enable() {
        mc.worldRenderer.reload()
    }

    override fun disable() {
        mc.worldRenderer.reload()
    }
}
