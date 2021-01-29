/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "BlockWalk", description = "Allows you to walk on non-fullblock blocks.", category = ModuleCategory.MOVEMENT)
class BlockWalk : Module()
{
	private val cobwebValue = BoolValue("Cobweb", true)
	private val snowValue = BoolValue("Snow", true)

	@EventTarget
	fun onBlockBB(event: BlockBBEvent)
	{
		if (cobwebValue.get() && event.block == classProvider.getBlockEnum(BlockType.WEB) || snowValue.get() && event.block == classProvider.getBlockEnum(BlockType.SNOW_LAYER))
		{
			val x = event.x
			val y = event.y
			val z = event.z

			event.boundingBox = classProvider.createAxisAlignedBB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
		}
	}
}
