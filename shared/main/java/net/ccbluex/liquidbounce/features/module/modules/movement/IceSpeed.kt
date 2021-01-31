/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getMaterial
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "IceSpeed", description = "Allows you to walk faster on ice.", category = ModuleCategory.MOVEMENT)
class IceSpeed : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("NCP", "AAC3.2.0", "Spartan146"), "NCP")
	override fun onEnable()
	{
		if (modeValue.get().equals("NCP", ignoreCase = true))
		{
			classProvider.getBlockEnum(BlockType.ICE).slipperiness = 0.39f
			classProvider.getBlockEnum(BlockType.ICE_PACKED).slipperiness = 0.39f
		}
		super.onEnable()
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val mode = modeValue.get()
		if (mode.equals("NCP", ignoreCase = true))
		{
			classProvider.getBlockEnum(BlockType.ICE).slipperiness = 0.39f
			classProvider.getBlockEnum(BlockType.ICE_PACKED).slipperiness = 0.39f
		}
		else
		{
			classProvider.getBlockEnum(BlockType.ICE).slipperiness = 0.98f
			classProvider.getBlockEnum(BlockType.ICE_PACKED).slipperiness = 0.98f
		}

		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.sneaking && thePlayer.sprinting && thePlayer.movementInput.moveForward > 0.0)
		{
			when (mode.toLowerCase())
			{
				"aac3.2.0" -> getMaterial(thePlayer.position.down()).let {
					if (it == classProvider.getBlockEnum(BlockType.ICE) || it == classProvider.getBlockEnum(BlockType.ICE_PACKED))
					{
						thePlayer.motionX *= 1.342
						thePlayer.motionZ *= 1.342

						classProvider.getBlockEnum(BlockType.ICE).slipperiness = 0.6f
						classProvider.getBlockEnum(BlockType.ICE_PACKED).slipperiness = 0.6f
					}
				}

				"spartan146" -> getMaterial(thePlayer.position.down()).let {
					if (it == classProvider.getBlockEnum(BlockType.ICE) || it == classProvider.getBlockEnum(BlockType.ICE_PACKED))
					{
						val blockAbove: IBlock? = BlockUtils.getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 2.0, thePlayer.posZ))

						if (classProvider.isBlockAir(blockAbove))
						{
							thePlayer.motionX *= 1.18
							thePlayer.motionZ *= 1.18
						}
						else
						{
							thePlayer.motionX *= 1.342
							thePlayer.motionZ *= 1.342
						}

						classProvider.getBlockEnum(BlockType.ICE).slipperiness = 0.6f
						classProvider.getBlockEnum(BlockType.ICE_PACKED).slipperiness = 0.6f
					}
				}
			}
		}
	}

	override fun onDisable()
	{
		classProvider.getBlockEnum(BlockType.ICE).slipperiness = 0.98f
		classProvider.getBlockEnum(BlockType.ICE_PACKED).slipperiness = 0.98f
		super.onDisable()
	}
}
