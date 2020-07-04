/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.utils.block.BlockUtils

/**
 * Get block by position
 */
fun WBlockPos.getBlock() = BlockUtils.getBlock(this)

/**
 * Get vector of block position
 */
fun WBlockPos.getVec() = WVec3(x + 0.5, y + 0.5, z + 0.5)