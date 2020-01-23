package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.util.BlockPos

/**
 * Get block by position
 */
fun BlockPos.getBlock() = BlockUtils.getBlock(this)