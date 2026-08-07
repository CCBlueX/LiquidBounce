package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import kotlin.jvm.optionals.getOrNull

object POIProvider {
    private val POI_CATEGORIES = listOf(
        getContainedBlocks(BlockTags.ICE),
        getContainedBlocks(BlockTags.PRESSURE_PLATES),
        getContainedBlocks(BlockTags.FENCES) + getContainedBlocks(BlockTags.WALLS),
        getContainedBlocks(BlockTags.STAIRS),
        listOf(Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH),
        listOf(Blocks.COBWEB),
        listOf(Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.ANVIL, Blocks.ENCHANTING_TABLE),
        getContainedBlocks(BlockTags.BEDS),
        listOf(Blocks.WATER),
        listOf(Blocks.LAVA, Blocks.FIRE),
        listOf(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.SHULKER_BOX, Blocks.BARREL),
        getContainedBlocks(BlockTags.DOORS) + getContainedBlocks(BlockTags.FENCE_GATES),
        getContainedBlocks(BlockTags.CLIMBABLE),
    )


    private fun getContainedBlocks(tag: TagKey<Block>): List<Block> {
        val contents = BuiltInRegistries.BLOCK.get(tag).getOrNull() ?: return emptyList()

        return contents.map { it.value() }
    }

    private val POI_MAP = buildMap {
        POI_CATEGORIES.forEachIndexed { index, blocks ->
            blocks.forEach { block ->
                put(block, index)
            }
        }
    }
    val playerCategoryNumber = POI_MAP.values.max() + 1

    fun getPOIType(block: Block): Int = POI_MAP.getOrDefault(block, 0)
}
