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
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
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
	val modeValue = ListValue("Mode", arrayOf("SpoofGround", "NoGround", "Packet", "MLG", "AAC3.1.0", "AAC3.3.4", "AAC3.3.11", "AAC3.3.15", "Spartan194", "CubeCraft", "Hypixel", "ACP"), "SpoofGround")

	private val noSpoofTicks = IntegerValue("NoSpoofTicks", 0, 0, 5)
	private val thresholdFallDistanceValue = FloatValue("ThresholdFallDistance", 1.5f, 0f, 2.9f)

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
		val theWorld = mc.theWorld ?: return
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

		val fly = LiquidBounce.moduleManager[Fly::class.java] as Fly
		if (!state || LiquidBounce.moduleManager[FreeCam::class.java].state || fly.state && fly.disableNoFall) return

		val entityBoundingBox = thePlayer.entityBoundingBox

		val provider = classProvider

		if (collideBlock(theWorld, thePlayer, entityBoundingBox, provider::isBlockLiquid) || collideBlock(theWorld, thePlayer, provider.createAxisAlignedBB(entityBoundingBox.maxX, entityBoundingBox.maxY, entityBoundingBox.maxZ, entityBoundingBox.minX, entityBoundingBox.minY - 0.01, entityBoundingBox.minZ), provider::isBlockLiquid))
		{
			noSpoof = 0
			return
		}

		val networkManager = mc.netHandler.networkManager
		val fallDistance = thePlayer.fallDistance
		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ

		val thresholdFallDistance = thresholdFallDistanceValue.get()

		when (modeValue.get().toLowerCase())
		{
			"packet" -> if (fallDistance > thresholdFallDistance)
			{
				val nospoofticks: Int = noSpoofTicks.get()
				if (noSpoof >= nospoofticks)
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayer(true))
					noSpoof = 0
				}
				noSpoof++
			}

			"cubecraft" -> if (fallDistance > thresholdFallDistance)
			{
				thePlayer.onGround = false
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayer(true))
			}

			"aac3.1.0" ->
			{
				if (fallDistance > thresholdFallDistance)
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayer(true))
					currentState = 2
				}
				else if (currentState == 2 && fallDistance < 2)
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

			"aac3.3.11" -> if (fallDistance > thresholdFallDistance)
			{
				thePlayer.motionZ = 0.0
				thePlayer.motionX = thePlayer.motionZ
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, posY - 10E-4, posZ, thePlayer.onGround))
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayer(true))
			}

			"aac3.3.15" -> if (fallDistance > thresholdFallDistance)
			{
				if (!mc.isIntegratedServerRunning) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, Double.NaN, posZ, false))
				thePlayer.fallDistance = -9999.0F
			}

			"spartan194" ->
			{
				spartanTimer.update()
				if (fallDistance > thresholdFallDistance && spartanTimer.hasTimePassed(10))
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, posY + 10, posZ, true))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(posX, posY - 10, posZ, true))
					spartanTimer.reset()
				}
			}
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val fallDistance = thePlayer.fallDistance

		val packet = event.packet
		val mode = modeValue.get()

		val fly = LiquidBounce.moduleManager[Fly::class.java] as Fly
		if (classProvider.isCPacketPlayer(packet) && !(fly.state && fly.disableNoFall))
		{
			val playerPacket = packet.asCPacketPlayer()

			if (fallDistance > thresholdFallDistanceValue.get())
			{
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

				// ACP
				else if (mode.equals("ACP", ignoreCase = true) /* && fallDistance > 2 */)
				{
					playerPacket.onGround = true

					thePlayer.motionZ = 0.0
					thePlayer.motionX = thePlayer.motionZ
				}

				// Hypixel
				if (mode.equals("Hypixel", ignoreCase = true) /* && fallDistance > 1.5 */) playerPacket.onGround = thePlayer.ticksExisted % 2 == 0
			}

			// NoGround
			if (mode.equals("NoGround", ignoreCase = true)) playerPacket.onGround = false
		}
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val playerBB = thePlayer.entityBoundingBox

		val fly = LiquidBounce.moduleManager[Fly::class.java] as Fly
		if (fly.state && fly.disableNoFall || collideBlock(theWorld, thePlayer, playerBB, classProvider::isBlockLiquid) || collideBlock(theWorld, thePlayer, classProvider.createAxisAlignedBB(playerBB.maxX, playerBB.maxY, playerBB.maxZ, playerBB.minX, playerBB.minY - 0.01, playerBB.minZ), classProvider::isBlockLiquid)) return

		if (modeValue.get().equals("AAC3.3.4", ignoreCase = true))
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
		if (!modeValue.get().equals("MLG", ignoreCase = true)) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val controller = mc.playerController

		val silentRotation = silentRotationValue.get()

		val provider = classProvider

		if (event.eventState == EventState.PRE)
		{
			currentMlgRotation = null

			mlgTimer.update()

			if (!mlgTimer.hasTimePassed(10)) return

			if (thePlayer.fallDistance > minFallDistance.get())
			{
				val fallingPlayer = FallingPlayer(theWorld, thePlayer, thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)

				val maxDist: Double = controller.blockReachDistance + 1.5

				val collision = fallingPlayer.findCollision(ceil(1.0 / thePlayer.motionY * -maxDist).toInt()) ?: return

				var ok: Boolean = WVec3(thePlayer.posX, thePlayer.posY + thePlayer.eyeHeight, thePlayer.posZ).distanceTo(WVec3(collision.pos).addVector(0.5, 0.5, 0.5)) < controller.blockReachDistance + sqrt(0.75)

				if (thePlayer.motionY < collision.pos.y + 1 - thePlayer.posY) ok = true

				if (!ok) return

				var mlgItemSlot = -1

				val inventory = thePlayer.inventory

				run {
					(0..8).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, itemStack) -> itemStack.item == provider.getItemEnum(ItemType.WATER_BUCKET) || provider.isItemBlock(itemStack.item) && (itemStack.item?.asItemBlock())?.block == provider.getBlockEnum(BlockType.WEB) }.forEach { (slot, _) ->
						mlgItemSlot = slot
						if (thePlayer.inventory.currentItem == mlgItemSlot) return@run
					}
				}

				if (mlgItemSlot == -1) return

				currentMlgItemIndex = mlgItemSlot
				currentMlgBlock = collision.pos

				if (thePlayer.inventory.currentItem != mlgItemSlot) thePlayer.sendQueue.addToSendQueue(provider.createCPacketHeldItemChange(mlgItemSlot))

				currentMlgRotation = RotationUtils.faceBlock(theWorld, thePlayer, collision.pos)

				if (silentRotation) RotationUtils.setTargetRotation(currentMlgRotation!!.rotation, if (keepRotationValue.get()) keepRotationLengthValue.get() else 0)
				else currentMlgRotation!!.rotation.applyRotationToPlayer(thePlayer)
			}
		}
		else if (currentMlgRotation != null)
		{
			val stack = thePlayer.inventory.getStackInSlot(currentMlgItemIndex + 36)

			if (provider.isItemBucket(stack!!.item)) controller.sendUseItem(thePlayer, theWorld, stack)
			else
			{

				//				val dirVec: WVec3i = classProvider.getEnumFacing(EnumFacingType.UP).directionVec

				if (controller.sendUseItem(thePlayer, theWorld, stack)) mlgTimer.reset()
			}
			if (thePlayer.inventory.currentItem != currentMlgItemIndex) thePlayer.sendQueue.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
		}
	}

	@EventTarget(ignoreCondition = true)
	fun onJump(@Suppress("UNUSED_PARAMETER") event: JumpEvent?)
	{
		jumped = true
	}

	override val tag: String
		get() = "${modeValue.get()}${if (modeValue.get().equals("SpoofGround", ignoreCase = true) || modeValue.get().equals("Packet", ignoreCase = true)) ", ${noSpoofTicks.get()}" else ""}"
}
