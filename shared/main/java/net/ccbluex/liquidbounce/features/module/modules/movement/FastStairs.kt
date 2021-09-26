/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.multiply
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "FastStairs", description = "Allows you to climb up stairs faster.", category = ModuleCategory.MOVEMENT)
class FastStairs : Module()
{

	private val modeValue = ListValue("Mode", arrayOf("Step", "NCP", "AAC3.1.0", "AAC3.3.6", "AAC3.3.13"), "NCP")
	private val longJumpValue = BoolValue("LongJump", false) // AAC LongJump

	private var canJump = false

	private var walkingDown = false

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (!thePlayer.isMoving || LiquidBounce.moduleManager[Speed::class.java].state) return

		if (thePlayer.fallDistance > 0 && !walkingDown) walkingDown = true
		else if (thePlayer.posY > thePlayer.prevChasingPosY) walkingDown = false

		val mode = modeValue.get().toLowerCase()

		if (!thePlayer.onGround) return

		val blockPos = WBlockPos(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ)

		val provider = classProvider

		if (provider.isBlockStairs(theWorld.getBlock(blockPos)) && !walkingDown)
		{
			thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 0.5, thePlayer.posZ)

			thePlayer.multiply(when (mode)
			{
				"ncp" -> 1.4
				"aac3.1.0" -> 1.5
				"aac3.3.13" -> 1.2
				else -> 1.0
			})
		}

		if (provider.isBlockStairs(theWorld.getBlock(blockPos.down())))
		{
			if (walkingDown)
			{
				when (mode)
				{
					"ncp" -> thePlayer.motionY = -1.0 // NCP ReverseStep
					"aac3.3.13" -> thePlayer.motionY -= 0.014
				}

				return
			}

			thePlayer.multiply(when (mode)
			{
				"ncp" -> 1.3
				"aac3.1.0" -> 1.3
				"aac3.3.6" -> 1.48
				"aac3.3.13" -> 1.52
				else -> 1.3
			})
			canJump = true
		}
		else if (mode.startsWith("aac", ignoreCase = true) && canJump)
		{
			if (longJumpValue.get())
			{
				thePlayer.jump()
				thePlayer.multiply(1.35)
			}

			canJump = false
		}
	}

	override val tag: String
		get() = modeValue.get()
}
