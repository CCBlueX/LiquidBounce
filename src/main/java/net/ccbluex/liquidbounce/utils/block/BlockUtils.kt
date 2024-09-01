/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.block.*
import net.minecraft.block.BlockState
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

typealias Collidable = (Block?) -> Boolean

object BlockUtils : MinecraftInstance() {
    /**
     * Get block from [blockPos]
     */
    fun getBlock(blockPos: BlockPos) = getState(blockPos)?.block

    /**
     * Get material from [blockPos]
     */
    fun getMaterial(blockPos: BlockPos) = getState(blockPos)?.block?.material

    /**
     * Check [blockPos] is replaceable
     */
    fun isReplaceable(blockPos: BlockPos) = getMaterial(blockPos)?.isReplaceable ?: false

    /**
     * Get state from [blockPos]
     */
    fun getState(blockPos: BlockPos) = mc.world?.getBlockState(blockPos)

    /**
     * Check if [blockPos] is clickable
     */
    fun canBeClicked(blockPos: BlockPos): Boolean {
        val state = getState(blockPos) ?: return false
        val block = state.block ?: return false

        return block.canCollide(state, false) && blockPos in mc.world.worldBorder && !block.material.isReplaceable
                && !block.hasBlockEntity() && isFullBlock(blockPos, state, true)
                && mc.world.entities.find { it is FallingBlockEntity && it.pos == blockPos } == null
                && block !is BlockWithEntity && block !is CraftingTableBlock
    }

    /**
     * Get block name by [id]
     */
    fun getBlockName(id: Int): String = Block.getById(id).translatedName

    /**
     * Check if block is full block
     */
    fun isFullBlock(blockPos: BlockPos, blockState: BlockState? = null, supportSlabs: Boolean = false): Boolean {
        val state = blockState ?: getState(blockPos) ?: return false

        val box = state.block.getCollisionBox(mc.world, blockPos, state) ?: return false

        // The slab will only return true if it's placed at a level that can be placed like any normal full block
        return box.maxX - box.minX == 1.0 && (box.maxY - box.minY == 1.0 || supportSlabs && box.maxY % 1.0 == 0.0) && box.maxZ - box.minZ == 1.0
    }

    fun isFullBlock(block: Block): Boolean {
        when (block) {
            // Soul Sand is considered as full block?!
            is SoulSandBlock -> return false

            // Glass isn't considered as full block?!
            is GlassBlock, is StainedGlassBlock -> return true
        }

        // Many translucent or non-full blocks have blockBounds set to 1.0
        return block.isFullBlock && block.isNormalBlock &&
                block.maxX == 1.0 && block.maxY == 1.0 && block.maxZ == 1.0
    }

    /**
     * Get distance to center of [blockPos]
     */
    fun getCenterDistance(blockPos: BlockPos) =
        mc.player.distanceTo(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

    /**
     * Search a limited amount [maxBlocksLimit] of specific blocks [targetBlocks] around the player in a specific [radius].
     * If [targetBlocks] is null it searches every block
     **/
    fun searchBlocks(radius: Int, targetBlocks: Set<Block>?, maxBlocksLimit: Int = 256): Map<BlockPos, Block> {
        val blocks = mutableMapOf<BlockPos, Block>()

        val player = mc.player ?: return blocks

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    if (blocks.size >= maxBlocksLimit) {
                        return blocks
                    }

                    val blockPos =
                        BlockPos(player.x.toInt() + x, player.y.toInt() + y, player.z.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    if (targetBlocks == null || targetBlocks.contains(block)) {
                        blocks[blockPos] = block
                    }
                }
            }
        }

        return blocks
    }

    /**
     * Check if [box] has collidable blocks using custom [collide] check
     */
    fun collideBlock(box: Box, collide: Collidable): Boolean {
        val player = mc.player

        for (x in player.boundingBox.minX.toInt() until player.boundingBox.maxX.toInt() + 1) {
            for (z in player.boundingBox.minZ.toInt() until player.boundingBox.maxZ.toInt() + 1) {
                val block = getBlock(BlockPos(x.toDouble(), box.minY, z.toDouble()))

                if (!collide(block))
                    return false
            }
        }

        return true
    }

    /**
     * Check if [Box] has collidable blocks using custom [collide] check
     */
    fun collideBlockIntersects(Box: Box, collide: Collidable): Boolean {
        val player = mc.player
        val world = mc.world

        for (x in player.boundingBox.minX.toInt() until player.boundingBox.maxX.toInt() + 1) {
            for (z in player.boundingBox.minZ.toInt() until player.boundingBox.maxZ.toInt() + 1) {
                val blockPos = BlockPos(x.toDouble(), Box.minY, z.toDouble())
                val block = getBlock(blockPos)

                if (collide(block)) {
                    val boundingBox = getState(blockPos)?.let { block?.getCollisionBox(world, blockPos, it) }
                        ?: continue

                    if (player.boundingBox.intersects(boundingBox))
                        return true
                }
            }
        }
        return false
    }

}