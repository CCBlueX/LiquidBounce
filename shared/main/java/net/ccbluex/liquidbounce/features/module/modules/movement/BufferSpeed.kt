/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import java.util.*

@ModuleInfo(name = "BufferSpeed", description = "Allows you to walk faster on slabs, stairs, ice and more. (a.k.a. TerrainSpeed)", category = ModuleCategory.MOVEMENT)
class BufferSpeed : Module()
{
	private val speedLimitValue = BoolValue("SpeedLimit", true)
	private val maxSpeedValue = FloatValue("MaxSpeed", 2.0f, 1.0f, 5f)
	private val bufferValue = BoolValue("Buffer", true)

	// Stairs boost
	private val stairsValue = BoolValue("Stairs", true)
	private val stairsBoostValue = FloatValue("StairsBoost", 1.87f, 1f, 2f)
	private val stairsModeValue = ListValue("StairsMode", arrayOf("Old", "New"), "New")

	// Slabs boost
	private val slabsValue = BoolValue("Slabs", true)
	private val slabsBoostValue = FloatValue("SlabsBoost", 1.87f, 1f, 2f)
	private val slabsModeValue = ListValue("SlabsMode", arrayOf("Old", "New"), "New")

	// Ice boost (AAC3.3.6)
	private val iceValue = BoolValue("Ice", false)
	private val iceBoostValue = FloatValue("IceBoost", 1.342f, 1f, 2f)

	// Snow boost
	private val snowValue = BoolValue("Snow", true)
	private val snowBoostValue = FloatValue("SnowBoost", 1.87f, 1f, 2f)
	private val snowPortValue = BoolValue("SnowPort", true)

	// Wall boost
	private val wallValue = BoolValue("Wall", true)
	private val wallBoostValue = FloatValue("WallBoost", 1.87f, 1f, 2f)
	private val wallModeValue = ListValue("WallMode", arrayOf("AAC3.2.1", "AAC3.3.8"), "AAC3.3.8")

	// HeadBlock(BlockAbove) boost
	private val headBlockValue = BoolValue("HeadBlock", true)
	private val headBlockBoostValue = FloatValue("HeadBlockBoost", 1.87f, 1f, 2f)

	private val slimeValue = BoolValue("Slime", true)

	private val airStrafeValue = BoolValue("AirStrafe", false)
	private val noHurtValue = BoolValue("NoHurt", true)

	private var speed = 0.0F
	private var down = false
	private var forceDown = false
	private var fastHop = false
	private var hadFastHop = false
	private var legitHop = false

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (LiquidBounce.moduleManager[Speed::class.java].state || noHurtValue.get() && thePlayer.hurtTime > 0)
		{
			reset()
			return
		}

		val blockPos = WBlockPos(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ)

		if (forceDown || down && thePlayer.motionY == 0.0)
		{
			thePlayer.motionY = -1.0
			down = false
			forceDown = false
		}

		if (fastHop)
		{
			thePlayer.speedInAir = 0.0211f
			hadFastHop = true
		}
		else if (hadFastHop)
		{
			thePlayer.speedInAir = 0.02f
			hadFastHop = false
		}

		if (!MovementUtils.isMoving(thePlayer) || thePlayer.sneaking || thePlayer.isInWater || mc.gameSettings.keyBindJump.isKeyDown)
		{
			reset()
			return
		}

		if (thePlayer.onGround)
		{
			fastHop = false

			val provider = classProvider

			if (slimeValue.get() && (provider.isBlockSlime(getBlock(theWorld, blockPos.down())) || provider.isBlockSlime(getBlock(theWorld, blockPos))))
			{
				thePlayer.jump()

				thePlayer.motionX *= 1.132
				thePlayer.motionY = 0.08
				thePlayer.motionZ *= 1.132

				down = true
				return
			}

			if (slabsValue.get() && provider.isBlockSlab(getBlock(theWorld, blockPos)))
			{
				when (slabsModeValue.get().toLowerCase())
				{
					"old" ->
					{
						boost(thePlayer, slabsBoostValue.get())
						return
					}

					"new" ->
					{
						fastHop = true
						if (legitHop)
						{
							thePlayer.jump()
							thePlayer.onGround = false
							legitHop = false
							return
						}
						thePlayer.onGround = false

						MovementUtils.strafe(thePlayer, 0.375f)

						thePlayer.jump()
						thePlayer.motionY = 0.41
						return
					}
				}
			}

			if (stairsValue.get() && (provider.isBlockStairs(getBlock(theWorld, blockPos.down())) || provider.isBlockStairs(getBlock(theWorld, blockPos))))
			{
				when (stairsModeValue.get().toLowerCase())
				{
					"old" ->
					{
						boost(thePlayer, stairsBoostValue.get())
						return
					}

					"new" ->
					{
						fastHop = true

						if (legitHop)
						{
							thePlayer.jump()
							thePlayer.onGround = false
							legitHop = false
							return
						}

						thePlayer.onGround = false
						MovementUtils.strafe(thePlayer, 0.375f)
						thePlayer.jump()
						thePlayer.motionY = 0.41
						return
					}
				}
			}
			legitHop = true

			if (headBlockValue.get() && getBlock(theWorld, blockPos.up(2)) != provider.getBlockEnum(BlockType.AIR))
			{
				boost(thePlayer, headBlockBoostValue.get())
				return
			}

			if (iceValue.get() && (getBlock(theWorld, blockPos.down()) == provider.getBlockEnum(BlockType.ICE) || getBlock(theWorld, blockPos.down()) == provider.getBlockEnum(BlockType.ICE_PACKED)))
			{
				boost(thePlayer, iceBoostValue.get())
				return
			}

			if (snowValue.get() && getBlock(theWorld, blockPos) == provider.getBlockEnum(BlockType.SNOW_LAYER) && (snowPortValue.get() || thePlayer.posY - thePlayer.posY.toInt() >= 0.12500))
			{
				if (thePlayer.posY - thePlayer.posY.toInt() >= 0.12500) boost(thePlayer, snowBoostValue.get())
				else
				{
					thePlayer.jump()
					forceDown = true
				}
				return
			}

			if (wallValue.get())
			{
				when (wallModeValue.get().toLowerCase())
				{
					"aac3.2.1" -> if (thePlayer.isCollidedVertically && isNearBlock(theWorld, thePlayer) || !provider.isBlockAir(getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY + 2.0, thePlayer.posZ))))
					{
						boost(thePlayer, wallBoostValue.get())

						return
					}

					"aac3.3.8" -> if (isNearBlock(theWorld, thePlayer) && !thePlayer.movementInput.jump)
					{
						thePlayer.jump()

						thePlayer.motionY = 0.08
						thePlayer.motionX *= 0.99
						thePlayer.motionZ *= 0.99

						down = true

						return
					}
				}
			}

			val currentSpeed = MovementUtils.getSpeed(thePlayer)

			if (speed < currentSpeed) speed = currentSpeed

			if (bufferValue.get() && speed > 0.2f)
			{
				speed /= 1.0199999809265137F
				MovementUtils.strafe(thePlayer, speed)
			}
		}
		else
		{
			speed = 0.0F

			if (airStrafeValue.get()) MovementUtils.strafe(thePlayer)
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet
		if (classProvider.isSPacketPlayerPosLook(packet)) speed = 0.0F
	}

	override fun onEnable()
	{
		reset()
	}

	override fun onDisable()
	{
		reset()
	}

	private fun reset()
	{
		val thePlayer = mc.thePlayer ?: return
		legitHop = true
		speed = 0.0F

		if (hadFastHop)
		{
			thePlayer.speedInAir = 0.02f
			hadFastHop = false
		}
	}

	private fun boost(thePlayer: IEntity, boost: Float)
	{
		thePlayer.motionX *= boost
		thePlayer.motionZ *= boost
		speed = MovementUtils.getSpeed(thePlayer)

		val maxSpeed = maxSpeedValue.get()
		if (speedLimitValue.get() && speed > maxSpeed) speed = maxSpeed
	}

	private fun isNearBlock(theWorld: IWorld, thePlayer: IEntity): Boolean
	{
		val blocks = ArrayDeque<WBlockPos>(4)

		blocks.add(WBlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ - 0.7))
		blocks.add(WBlockPos(thePlayer.posX + 0.7, thePlayer.posY + 1, thePlayer.posZ))
		blocks.add(WBlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ + 0.7))
		blocks.add(WBlockPos(thePlayer.posX - 0.7, thePlayer.posY + 1, thePlayer.posZ))

		val provider = classProvider

		return blocks.map { blockPos ->
			val blockState = theWorld.getBlockState(blockPos)
			blockState to blockState.block.getCollisionBoundingBox(theWorld, blockPos, blockState)
		}.any { (blockState, collisionBoundingBox) ->
			val block = blockState.block
			block == provider.getBlockEnum(BlockType.BARRIER) || (collisionBoundingBox == null || collisionBoundingBox.maxX == collisionBoundingBox.minY + 1) && !block.isTranslucent(blockState) && block == provider.getBlockEnum(BlockType.WATER) && !provider.isBlockSlab(block)
		}
	}
}
