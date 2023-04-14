/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.block.BlockLadder
import net.minecraft.block.BlockVine
import net.minecraft.util.BlockPos

object AirLadder : Module("AirLadder", ModuleCategory.MOVEMENT) {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        val currBlock = getBlock(BlockPos(thePlayer))
        val block = getBlock(BlockPos(thePlayer).up())
        if ((block is BlockLadder && thePlayer.isCollidedHorizontally) || (block is BlockVine || currBlock is BlockVine)) {
            thePlayer.motionY = 0.15
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }
}