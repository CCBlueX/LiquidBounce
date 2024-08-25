/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Vec3d

/**
 * Get block by position
 */
fun BlockPos.getBlock() = BlockUtils.getBlock(this)

/**
 * Get vector of block position
 */
fun BlockPos.getVec() = Vec3d(x + 0.5, y + 0.5, z + 0.5)

fun BlockPos.toVec() = Vec3d(this)

fun BlockPos.isReplaceable() = BlockUtils.isReplaceable(this)

fun BlockPos.canBeClicked() = BlockUtils.canBeClicked(this)