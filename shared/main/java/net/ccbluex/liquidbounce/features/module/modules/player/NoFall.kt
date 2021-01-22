/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.VecRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.*
import kotlin.math.ceil
import kotlin.math.sqrt

@ModuleInfo(name = "NoFall", description = "Prevents you from taking fall damage.", category = ModuleCategory.PLAYER)
class NoFall : Module()
{
	@JvmField
	val modeValue = ListValue("Mode", arrayOf("SpoofGround", "NoGround", "Packet", "MLG", "AAC3.1.0", "AAC3.3.4", "AAC3.3.11", "AAC3.3.15", "Spartan", "CubeCraft", "Hypixel", "AntiCheatPlus"), "SpoofGround")

	private val noSpoofTicks = IntegerValue("NoSpoofTicks", 0, 0, 5)
	private val minFallDistance = FloatValue("MinMLGHeight", 5f, 2f, 50f)

	private val silentRotationValue = BoolValue("SilentRotation", true)
	private val keepRotationValue = BoolValue("KeepRotation", false)
	private val keepRotationLengthValue = IntegerValue("KeepRotationLength", 1, 1, 40)

	private val spartanTimer = TickTimer()
	private val mlgTimer = TickTimer()

	private var currentState = 0
	private var jumped = false
	private var noSpoof = 0

	private var currentMlgRotation: VecRotation? = null
	private var currentMlgItemIndex = 0
	private var currentMlgBlock: WBlockPos? = null

	@EventTarget(ignoreCondition = true)
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround)
		{
			noSpoof = 0
			jumped = false
		}

		if (thePlayer.motionY > 0)
		{
			noSpoof = 0
			jumped = true
		}

		if (!state || LiquidBounce.moduleManager.getModule(FreeCam::class.java).state /*|| Fly.waitForDamage*/) return

		if (collideBlock(thePlayer.entityBoundingBox, classProvider::isBlockLiquid) || collideBlock(
				classProvider.createAxisAlignedBB(
					thePlayer.entityBoundingBox.maxX, thePlayer.entityBoundingBox.maxY, thePlayer.entityBoundingBox.maxZ, thePlayer.entityBoundingBox.minX, thePlayer.entityBoundingBox.minY - 0.01, thePlayer.entityBoundingBox.minZ
				), classProvider::isBlockLiquid
			)
		)
		{
			noSpoof = 0
			return
		}

		when (modeValue.get().toLowerCase())
		{
			"packet" -> if (thePlayer.fallDistance > 2f)
			{
				val nospoofticks: Int = noSpoofTicks.get()
				if (noSpoof >= nospoofticks)
				{
					mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(true))
					noSpoof = 0
				}
				noSpoof++
			}

			"cubecraft" -> if (thePlayer.fallDistance > 2f)
			{
				thePlayer.onGround = false
				thePlayer.sendQueue.addToSendQueue(classProvider.createCPacketPlayer(true))
			}

			"aac3.1.0" ->
			{
				if (thePlayer.fallDistance > 2f)
				{
					mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(true))
					currentState = 2
				} else if (currentState == 2 && thePlayer.fallDistance < 2)
				{
					thePlayer.motionY = 0.1
					currentState = 3
					return
				}
				when (currentState)
				{
					3 ->
					{
						thePlayer.motionY = 0.1
						currentState = 4
					}

					4 ->
					{
						thePlayer.motionY = 0.1
						currentState = 5
					}

					5 ->
					{
						thePlayer.motionY = 0.1
						currentState = 1
					}
				}
			}

			"aac3.3.4" -> if (!jumped && thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb) thePlayer.motionY = -6.0

			"aac3.3.11" -> if (thePlayer.fallDistance > 2)
			{
				thePlayer.motionZ = 0.0
				thePlayer.motionX = thePlayer.motionZ
				mc.netHandler.addToSendQueue(
					classProvider.createCPacketPlayerPosition(
						thePlayer.posX, thePlayer.posY - 10E-4, thePlayer.posZ, thePlayer.onGround
					)
				)
				mc.netHandler.addToSendQueue(classProvider.createCPacketPlayer(true))
			}

			"aac3.3.15" -> if (thePlayer.fallDistance > 2)
			{
				if (!mc.isIntegratedServerRunning) mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(thePlayer.posX, Double.NaN, thePlayer.posZ, false))
				thePlayer.fallDistance = (-9999).toFloat()
			}

			"spartan" ->
			{
				spartanTimer.update()
				if (thePlayer.fallDistance > 1.5 && spartanTimer.hasTimePassed(10))
				{
					mc.netHandler.addToSendQueue(
						classProvider.createCPacketPlayerPosition(
							thePlayer.posX, thePlayer.posY + 10, thePlayer.posZ, true
						)
					)
					mc.netHandler.addToSendQueue(
						classProvider.createCPacketPlayerPosition(
							thePlayer.posX, thePlayer.posY - 10, thePlayer.posZ, true
						)
					)
					spartanTimer.reset()
				}
			}
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val packet = event.packet
		val mode = modeValue.get()
		if (classProvider.isCPacketPlayer(packet)/* && !Fly.waitForDamage*/)
		{
			val playerPacket = packet.asCPacketPlayer()

			// SpoofGround
			if (mode.equals("SpoofGround", ignoreCase = true))
			{
				val nospoofticks: Int = noSpoofTicks.get()
				if (noSpoof >= nospoofticks)
				{
					playerPacket.onGround = true
					noSpoof = 0
				}
				noSpoof++
			}

			// NoGround
			if (mode.equals("NoGround", ignoreCase = true)) playerPacket.onGround = false

			// Hypixel
			if (mode.equals("Hypixel", ignoreCase = true) && mc.thePlayer != null && mc.thePlayer!!.fallDistance > 1.5) playerPacket.onGround = mc.thePlayer!!.ticksExisted % 2 == 0

			// AntiCheatPlus
			if (thePlayer.fallDistance > 2 && mc.thePlayer != null && mode.equals("AntiCheatPlus", ignoreCase = true))
			{
				playerPacket.onGround = true

				thePlayer.motionZ = 0.0
				thePlayer.motionX = thePlayer.motionZ
			}
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (collideBlock(thePlayer.entityBoundingBox, classProvider::isBlockLiquid) || collideBlock(
				classProvider.createAxisAlignedBB(
					thePlayer.entityBoundingBox.maxX, thePlayer.entityBoundingBox.maxY, thePlayer.entityBoundingBox.maxZ, thePlayer.entityBoundingBox.minX, thePlayer.entityBoundingBox.minY - 0.01, thePlayer.entityBoundingBox.minZ
				), classProvider::isBlockLiquid
			)
		) return

		if (modeValue.get().equals("laac", ignoreCase = true))
		{
			if (!jumped && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb && thePlayer.motionY < 0.0)
			{
				event.x = 0.0
				event.z = 0.0
			}
		}
	}

	@EventTarget
	private fun onMotionUpdate(event: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		if (!modeValue.get().equals("MLG", ignoreCase = true)) return

		val silentRotation = silentRotationValue.get()

		if (event.eventState == EventState.PRE)
		{
			currentMlgRotation = null

			mlgTimer.update()

			if (!mlgTimer.hasTimePassed(10)) return

			if (thePlayer.fallDistance > minFallDistance.get())
			{
				val fallingPlayer = FallingPlayer(
					thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward
				)

				val maxDist: Double = mc.playerController.blockReachDistance + 1.5

				val collision = fallingPlayer.findCollision(ceil(1.0 / thePlayer.motionY * -maxDist).toInt()) ?: return

				var ok: Boolean = WVec3(thePlayer.posX, thePlayer.posY + thePlayer.eyeHeight, thePlayer.posZ).distanceTo(WVec3(collision.pos).addVector(0.5, 0.5, 0.5)) < mc.playerController.blockReachDistance + sqrt(0.75)

				if (thePlayer.motionY < collision.pos.y + 1 - thePlayer.posY)
				{
					ok = true
				}

				if (!ok) return

				var index = -1

				for (i in 36..44)
				{
					val itemStack = thePlayer.inventoryContainer.getSlot(i).stack

					if (itemStack != null && (itemStack.item == classProvider.getItemEnum(ItemType.WATER_BUCKET) || classProvider.isItemBlock(itemStack.item) && (itemStack.item?.asItemBlock())?.block == classProvider.getBlockEnum(BlockType.WEB)))
					{
						index = i - 36

						if (thePlayer.inventory.currentItem == index) break
					}
				}
				if (index == -1) return

				currentMlgItemIndex = index
				currentMlgBlock = collision.pos

				if (thePlayer.inventory.currentItem != index)
				{
					thePlayer.sendQueue.addToSendQueue(classProvider.createCPacketHeldItemChange(index))
				}

				currentMlgRotation = RotationUtils.faceBlock(collision.pos)

				if (silentRotation) RotationUtils.setTargetRotation(currentMlgRotation!!.rotation, if (keepRotationValue.get()) keepRotationLengthValue.get() else 0)
				else currentMlgRotation!!.rotation.applyRotationToPlayer(thePlayer)
			}
		} else if (currentMlgRotation != null)
		{
			val stack = thePlayer.inventory.getStackInSlot(currentMlgItemIndex + 36)

			if (classProvider.isItemBucket(stack!!.item))
			{
				mc.playerController.sendUseItem(thePlayer, mc.theWorld!!, stack)
			} else
			{

				//				val dirVec: WVec3i = classProvider.getEnumFacing(EnumFacingType.UP).directionVec

				if (mc.playerController.sendUseItem(thePlayer, mc.theWorld!!, stack)) mlgTimer.reset()
			}
			if (thePlayer.inventory.currentItem != currentMlgItemIndex) thePlayer.sendQueue.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
		}
	}

	@EventTarget(ignoreCondition = true)
	fun onJump(@Suppress("UNUSED_PARAMETER") event: JumpEvent?)
	{
		jumped = true
	}

	override val tag: String
		get() = modeValue.get()
}
