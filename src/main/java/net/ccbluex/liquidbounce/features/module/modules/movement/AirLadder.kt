/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.util.BlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ
import net.ccbluex.liquidbounce.value.FloatValue

@ModuleInfo(name = "AirLadder", description = "Allows you to climb up ladders/vines without touching them.", category = ModuleCategory.MOVEMENT)
class AirLadder : Module()
{
    private val motionValue = FloatValue("Motion", 0.118F, 0.118F, 0.6F, description = "Climbing up speed")

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (classProvider.isBlockLadder(theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ))) && thePlayer.isCollidedHorizontally || classProvider.isBlockVine(theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ))) || classProvider.isBlockVine(theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ))))
        {
            thePlayer.motionY = motionValue.get().toDouble()
            thePlayer.zeroXZ()
        }
    }
}
