/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "FastClimb", description = "Allows you to climb up ladders and vines faster.", category = ModuleCategory.MOVEMENT)
class FastClimb : Module()
{
	val modeValue = ListValue("Mode", arrayOf("Vanilla", "Clip", "AAC3.0.0", "AAC3.0.5", "SAAC3.1.2", "AAC3.1.2"), "Vanilla")
	private val speedValue = FloatValue("Speed", 0.2872F, 0.01F, 5F)
	private val clipSearchHeight = IntegerValue("Clip-SearchHeight", 8, 8, 255)

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val mode = modeValue.get().toLowerCase()

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		val speed = speedValue.get().toDouble()

		val horizontalFacing = thePlayer.horizontalFacing

		val provider = classProvider

		when (mode)
		{
			"vanilla" -> if (thePlayer.isCollidedHorizontally && thePlayer.isOnLadder)
			{
				event.y = speed
				thePlayer.motionY = 0.0
			}

			"aac3.0.0" -> if (thePlayer.isCollidedHorizontally)
			{
				var x = 0.0
				var z = 0.0

				when
				{
					horizontalFacing.isNorth() -> z = -0.99
					horizontalFacing.isEast() -> x = +0.99
					horizontalFacing.isSouth() -> z = +0.99
					horizontalFacing.isWest() -> x = -0.99

					else ->
					{
					}
				}

				val block = getBlock(theWorld, WBlockPos(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z))

				if (provider.isBlockLadder(block) || provider.isBlockVine(block))
				{
					event.y = 0.5
					thePlayer.motionY = 0.0
				}
			}

			"aac3.0.5" -> if (gameSettings.keyBindForward.isKeyDown && collideBlockIntersects(theWorld, thePlayer.entityBoundingBox) {
					provider.isBlockLadder(it) || provider.isBlockVine(it)
				})
			{
				event.zeroXZ()
				event.y = 0.5
				MovementUtils.zeroXYZ(thePlayer)
			}

			"saac3.1.2" -> if (thePlayer.isCollidedHorizontally && thePlayer.isOnLadder)
			{
				event.y = 0.1649
				thePlayer.motionY = 0.0
			}

			"aac3.1.2" -> if (thePlayer.isCollidedHorizontally && thePlayer.isOnLadder)
			{
				event.y = 0.1699
				thePlayer.motionY = 0.0
			}

			"clip" -> if (thePlayer.isOnLadder && gameSettings.keyBindForward.isKeyDown)
			{
				for (i in thePlayer.posY.toInt()..thePlayer.posY.toInt() + clipSearchHeight.get())
				{
					val block = getBlock(theWorld, WBlockPos(thePlayer.posX, i.toDouble(), thePlayer.posZ))

					if (!provider.isBlockLadder(block))
					{
						var x = 0.0
						var z = 0.0

						when
						{
							horizontalFacing.isNorth() -> z = -1.0
							horizontalFacing.isEast() -> x = +1.0
							horizontalFacing.isSouth() -> z = +1.0
							horizontalFacing.isWest() -> x = -1.0

							else ->
							{
							}
						}

						thePlayer.setPosition(thePlayer.posX + x, i.toDouble(), thePlayer.posZ + z)
						break
					}

					thePlayer.setPosition(thePlayer.posX, i.toDouble(), thePlayer.posZ)
				}
			}
		}
	}

	@EventTarget
	fun onBlockBB(event: BlockBBEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val eventBlock = event.block

		val provider = classProvider

		if ((provider.isBlockLadder(eventBlock) || provider.isBlockVine(eventBlock)) && modeValue.get().equals("AAC3.0.5", ignoreCase = true) && thePlayer.isOnLadder) event.boundingBox = null
	}

	override val tag: String
		get() = modeValue.get()
}
