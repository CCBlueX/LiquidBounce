package net.ccbluex.liquidbounce.invitro

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isBlockUnfavourable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.integrationtest.util.tenaccAssert
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.tenacc.api.TACCTest
import net.ccbluex.tenacc.api.TACCTestClass
import net.ccbluex.tenacc.api.common.TACCSequenceAdapter
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack

@TACCTestClass("TestInventoryBlockManagement")
class TestInventoryBlockManagement {
    private val unfavourableBlocks = arrayOf(
        Blocks.ICE,
        Blocks.CRAFTING_TABLE,
        Blocks.STONE_SLAB,
        Blocks.CHEST,
        Blocks.BEEHIVE,
        Blocks.MAGMA_BLOCK,
        Blocks.BLUE_ICE,
        Blocks.SLIME_BLOCK,
        Blocks.SOUL_SAND,
        Blocks.HONEY_BLOCK,
        Blocks.SPAWNER,
    )

    @TACCTest(name = "testValidAndUnfavourableBlocks", scenary = "generic/one_block_platform.nbt")
    fun testValidAndUnfavourableBlocks(adapter: TACCSequenceAdapter) {
        adapter.startSequence {
            client {
                runFavourableTest()
            }

            sync()
        }
    }

    @TACCTest(name = "testBlockPriority", scenary = "generic/one_block_platform.nbt")
    fun testBlockPriority(adapter: TACCSequenceAdapter) {
        adapter.startSequence {
            client {
                runBlockPriorityTest()
            }

            sync()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun runBlockPriorityTest() {
        val categories = arrayOf(
            arrayOf(Blocks.SANDSTONE, Blocks.STONE, Blocks.OAK_WOOD),
            arrayOf(Blocks.OBSIDIAN, Blocks.OAK_LEAVES),
            unfavourableBlocks,
        )

        for (i0 in categories.indices) {
            val betterCategory = categories[i0]

            for (i1 in i0 + 1 until categories.size) {
                val worseCategory = categories[i1]

                for (betterBlock in betterCategory) {
                    for (worseBlock in worseCategory) {
                        assertFavourLeft(createStack(betterBlock), createStack(worseBlock))
                    }
                }
            }
        }

        val sandstoneFewer = ItemStack(Blocks.SANDSTONE.asItem(), 16)
        val sandstoneMore = ItemStack(Blocks.SANDSTONE.asItem(), 64)

        tenaccAssert(
            ModuleScaffold.BLOCK_COMPARATOR_FOR_INVENTORY.compare(sandstoneMore, sandstoneFewer) > 0,
            "Inventory comparator should favour more items"
        )
        tenaccAssert(
            ModuleScaffold.BLOCK_COMPARATOR_FOR_HOTBAR.compare(sandstoneMore, sandstoneFewer) < 0,
            "Hotbar comparator should favour less items"
        )
    }

    private fun assertFavourLeft(lhs: ItemStack, rhs: ItemStack) {
        val message = "${lhs.name.convertToString()} should be better than ${rhs.name.convertToString()}."

        tenaccAssert(
            ModuleScaffold.BLOCK_COMPARATOR_FOR_INVENTORY.compare(lhs, rhs) > 0,
            "$message (inventory comparator)"
        )
        tenaccAssert(
            ModuleScaffold.BLOCK_COMPARATOR_FOR_HOTBAR.compare(lhs, rhs) > 0,
            "$message (hotbar comparator)"
        )
    }

    private fun runFavourableTest() {
        val validBlocks = arrayOf(
            Blocks.STONE,
            Blocks.OAK_WOOD,
            Blocks.SANDSTONE,
            Blocks.END_STONE,
            Blocks.CRAFTING_TABLE,
            Blocks.OAK_LEAVES
        )
        val invalidBlocks = arrayOf(
            Blocks.CHEST,
            Blocks.TNT,
            Blocks.ENCHANTING_TABLE,
            Blocks.DRAGON_HEAD,
            Blocks.DRAGON_EGG,
            Blocks.GRAVEL,
            Blocks.SAND,
        )
        val favourableBlocks = arrayOf(
            Blocks.STONE,
            Blocks.OAK_WOOD,
            Blocks.SANDSTONE,
            Blocks.END_STONE,
        )
        val unfavourableBlocks = unfavourableBlocks

        validBlocks.forEach {
            tenaccAssert(isValidBlock(createStack(it)), "${it.name.convertToString()} is a valid block")
        }
        invalidBlocks.forEach {
            tenaccAssert(!isValidBlock(createStack(it)), "${it.name.convertToString()} is an invalid block")
        }
        favourableBlocks.forEach {
            tenaccAssert(
                !isBlockUnfavourable(createStack(it)),
                "${it.name.convertToString()} is a favourable block"
            )
        }
        unfavourableBlocks.forEach {
            tenaccAssert(
                isBlockUnfavourable(createStack(it)),
                "${it.name.convertToString()} is an unfavourable block"
            )
        }
    }

    private fun createStack(block: Block) = ItemStack(block.asItem(), 64)

}
