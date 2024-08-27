/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.block.LadderBlock
import net.minecraft.block.BlockVine
import net.minecraft.util.math.BlockPos

object AirLadder : Module("AirLadder", Category.MOVEMENT, hideModule = false) {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return

        val currBlock = getBlock(BlockPos(player))
        val block = getBlock(BlockPos(player).up())
        if ((block is LadderBlock && player.isCollidedHorizontally) || (block is BlockVine || currBlock is BlockVine)) {
            player.velocityY = 0.15
            player.velocityX = 0.0
            player.velocityZ = 0.0
        }
    }
}