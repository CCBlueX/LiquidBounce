/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "LiquidWalk", description = "Allows you to walk on water.", category = ModuleCategory.MOVEMENT, keyBind = Keyboard.KEY_J)
class LiquidWalk : Module()
{
	val modeValue = ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC3.1.0", "AAC3.3.5", "AAC3.3.11", "Spartan146", "Dolphin"), "NCP")
	private val noJumpValue = BoolValue("NoJump", false)
	private val aacFlyValue = FloatValue("AAC3.3.5-Motion", 0.5f, 0.1f, 1f)

	private var nextTick = false

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.sneaking) return

		val isInWater = thePlayer.isInWater

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ

		when (modeValue.get().toLowerCase())
		{
			"ncp", "vanilla" -> if (collideBlock(theWorld, thePlayer.entityBoundingBox, classProvider::isBlockLiquid) && thePlayer.isInsideOfMaterial(classProvider.getMaterialEnum(MaterialType.AIR)) && !thePlayer.sneaking) thePlayer.motionY = 0.08

			"aac3.1.0" ->
			{
				val blockPos = thePlayer.position.down()

				if (!thePlayer.onGround && getBlock(theWorld, blockPos) == classProvider.getBlockEnum(BlockType.WATER) || isInWater)
				{
					if (!thePlayer.sprinting)
					{
						thePlayer.motionX *= 0.99999
						thePlayer.motionY *= 0.0
						thePlayer.motionZ *= 0.99999
						if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((posY - (posY - 1).toInt()).toInt() * 0.125)
					}
					else
					{
						thePlayer.motionX *= 0.99999
						thePlayer.motionY *= 0.0
						thePlayer.motionZ *= 0.99999
						if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((posY - (posY - 1).toInt()).toInt() * 0.125)
					}
					if (thePlayer.fallDistance >= 4) thePlayer.motionY = -0.004 else if (isInWater) thePlayer.motionY = 0.09
				}

				if (thePlayer.hurtTime != 0) thePlayer.onGround = false
			}

			"spartan146" -> if (isInWater)
			{
				if (thePlayer.isCollidedHorizontally)
				{
					thePlayer.motionY += 0.15
					return
				}

				val block = getBlock(theWorld, WBlockPos(posX, posY + 1, posZ))
				val blockUp = getBlock(theWorld, WBlockPos(posX, posY + 1.1, posZ))

				if (classProvider.isBlockLiquid(blockUp)) thePlayer.motionY = 0.1 else if (classProvider.isBlockLiquid(block)) thePlayer.motionY = 0.0

				thePlayer.onGround = true
				thePlayer.motionX *= 1.085
				thePlayer.motionZ *= 1.085
			}

			"aac3.3.11" -> if (isInWater)
			{
				thePlayer.motionX *= 1.17
				thePlayer.motionZ *= 1.17
				if (thePlayer.isCollidedHorizontally) thePlayer.motionY = 0.24 else if (theWorld.getBlockState(WBlockPos(posX, posY + 1.0, posZ)).block != classProvider.getBlockEnum(BlockType.AIR)) thePlayer.motionY += 0.04
			}

			"dolphin" -> if (isInWater) thePlayer.motionY += 0.03999999910593033 // Same as normal swimming
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		if (modeValue.get().equals("AAC3.3.5", ignoreCase = true) && thePlayer.isInWater)
		{
			val aacFlyMotion = aacFlyValue.get().toDouble()

			event.y = aacFlyMotion
			thePlayer.motionY = aacFlyMotion
		}
	}

	@EventTarget
	fun onBlockBB(event: BlockBBEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (classProvider.isBlockLiquid(event.block) && !collideBlock(theWorld, thePlayer.entityBoundingBox, classProvider::isBlockLiquid) && !thePlayer.sneaking) when (modeValue.get().toLowerCase())
		{
			"ncp", "vanilla" ->
			{
				val x = event.x
				val y = event.y
				val z = event.z

				event.boundingBox = classProvider.createAxisAlignedBB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
			}
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (!modeValue.get().equals("NCP", ignoreCase = true)) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (classProvider.isCPacketPlayer(event.packet))
		{
			val packetPlayer = event.packet.asCPacketPlayer()

			if (collideBlock(theWorld, classProvider.createAxisAlignedBB(thePlayer.entityBoundingBox.maxX, thePlayer.entityBoundingBox.maxY, thePlayer.entityBoundingBox.maxZ, thePlayer.entityBoundingBox.minX, thePlayer.entityBoundingBox.minY - 0.01, thePlayer.entityBoundingBox.minZ), classProvider::isBlockLiquid))
			{
				nextTick = !nextTick
				if (nextTick) packetPlayer.y -= 0.001
			}
		}
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val block = getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ))

		if (noJumpValue.get() && classProvider.isBlockLiquid(block)) event.cancelEvent()
	}

	override val tag: String
		get() = modeValue.get()
}
