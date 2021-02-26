/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils

@ModuleInfo(name = "AirLadder", description = "Allows you to climb up ladders/vines without touching them.", category = ModuleCategory.MOVEMENT)
class AirLadder : Module()
{
	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		if (provider.isBlockLadder(BlockUtils.getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ))) && thePlayer.isCollidedHorizontally || provider.isBlockVine(BlockUtils.getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ))) || provider.isBlockVine(BlockUtils.getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ))))
		{
			thePlayer.motionY = 0.15
			MovementUtils.zeroXZ(thePlayer)
		}
	}
}
