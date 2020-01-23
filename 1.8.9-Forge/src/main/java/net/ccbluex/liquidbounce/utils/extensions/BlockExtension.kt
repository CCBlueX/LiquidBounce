/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.util.BlockPos

/**
 * Get block by position
 */
fun BlockPos.getBlock() = BlockUtils.getBlock(this)