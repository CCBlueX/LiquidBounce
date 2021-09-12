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
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.VecRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.*
import kotlin.math.ceil
import kotlin.math.pow

@ModuleInfo(name = "NoFall", description = "Prevents you from taking fall damage.", category = ModuleCategory.PLAYER)
class NoFall : Module()
{
	@JvmField
	val modeValue = ListValue("Mode", arrayOf("SpoofGround", "NoGround", "Packet", "MLG", "AAC3.1.0", "AAC3.3.4", "AAC3.3.11", "AAC3.3.15", "Spartan194", "CubeCraft", "Hypixel", "ACP"), "SpoofGround")

	private val noSpoofTicks = object : IntegerValue("NoSpoofTicks", 0, 0, 5)
	{
		override fun showCondition() = modeValue.get().equals("SpoofGround", ignoreCase = true) || modeValue.get().equals("Packet", ignoreCase = true)
	}

	private val thresholdFallDistanceValue = FloatValue("ThresholdFallDistance", 1.5f, 0f, 2.9f)

	private val mlgGroup = object : ValueGroup("MLG")
	{
		override fun showCondition() = modeValue.get().equals("MLG", ignoreCase = true)
	}
	private val mlgMinFallDistance = FloatValue("MinHeight", 5f, 2f, 50f, "MinMLGHeight")
	private val mlgSilentRotationValue = BoolValue("SilentRotation", true, "SilentRotation")

	private val mlgKeepRotationGroup = ValueGroup("KeepRotation")
	private val mlgKeepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
	private val mlgKeepRotationTicksValue = IntegerValue("Ticks", 1, 1, 40, "KeepRotationLength")

	private val spartanTimer = TickTimer()
	private val mlgTimer = TickTimer()

	private var currentState = 0
	private var jumped = false
	private var noSpoof = 0

	private var currentMlgRotation: VecRotation? = null
	private var currentMlgItemIndex = 0
	private var currentMlgBlock: WBlockPos? = null

	init
	{
		mlgKeepRotationGroup.addAll(mlgKeepRotationEnabledValue, mlgKeepRotationTicksValue)
		mlgGroup.addAll(mlgMinFallDistance, mlgSilentRotationValue, mlgKeepRotationGroup)
	}

	@EventTarget(ignoreCondition = true)
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager

		val playerBB = thePlayer.entityBoundingBox
		val fallDistance = thePlayer.fallDistance
		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ

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

		val moduleManager = LiquidBounce.moduleManager
		val provider = classProvider

		val fly = moduleManager[Fly::class.java] as Fly
		if (!state || moduleManager[FreeCam::class.java].state || fly.state && fly.shouldDisableNoFall || thePlayer.spectator || thePlayer.capabilities.allowFlying || thePlayer.capabilities.disableDamage || collideBlock(theWorld, playerBB) { provider.isBlockLiquid(it.block) } || collideBlock(theWorld, provider.createAxisAlignedBB(playerBB.minX, playerBB.minY - 0.01, playerBB.minZ, playerBB.maxX, playerBB.maxY, playerBB.maxZ)) { provider.isBlockLiquid(it.block) })
		{
			noSpoof = 0
			return
		}

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
		if (classProvider.isCPacketPlayer(packet) && !(fly.state && fly.shouldDisableNoFall))
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
		if (fly.state && fly.shouldDisableNoFall || collideBlock(theWorld, playerBB) { classProvider.isBlockLiquid(it.block) } || collideBlock(theWorld, classProvider.createAxisAlignedBB(playerBB.minX, playerBB.minY - 0.01, playerBB.minZ, playerBB.maxX, playerBB.maxY, playerBB.maxZ)) { classProvider.isBlockLiquid(it.block) }) return

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

		val silentRotation = mlgSilentRotationValue.get()

		val provider = classProvider

		if (event.eventState == EventState.PRE)
		{
			currentMlgRotation = null

			mlgTimer.update()

			if (!mlgTimer.hasTimePassed(10)) return

			if (thePlayer.fallDistance > mlgMinFallDistance.get())
			{
				val fallingPlayer = FallingPlayer(theWorld, thePlayer, thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)

				val maxDist: Double = controller.blockReachDistance + 1.5

				val collision = fallingPlayer.findCollision(ceil(1.0 / thePlayer.motionY * -maxDist).toInt()) ?: return

				var reachCheck: Boolean = WVec3(thePlayer.posX, thePlayer.posY + thePlayer.eyeHeight, thePlayer.posZ).squareDistanceTo(WVec3(collision.pos).addVector(0.5, 0.5, 0.5)) <= (controller.blockReachDistance + 0.75).pow(2)

				if (thePlayer.motionY < collision.pos.y + 1 - thePlayer.posY) reachCheck = true

				if (!reachCheck) return

				var mlgItemSlot = -1

				val inventory = thePlayer.inventory

				run {
					(0..8).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, stack) -> stack.item == provider.getItemEnum(ItemType.WATER_BUCKET) || provider.isItemBlock(stack.item) && (stack.item?.asItemBlock())?.block == provider.getBlockEnum(BlockType.WEB) }.forEach {
						mlgItemSlot = it.first
						if (thePlayer.inventory.currentItem == mlgItemSlot) return@run
					}
				}

				if (mlgItemSlot == -1) return

				currentMlgItemIndex = mlgItemSlot
				currentMlgBlock = collision.pos

				if (!InventoryUtils.tryHoldSlot(thePlayer, mlgItemSlot, lock = true)) return

				val currentMlgRotation = RotationUtils.faceBlock(theWorld, thePlayer, collision.pos)

				this.currentMlgRotation = currentMlgRotation

				if (currentMlgRotation != null) if (silentRotation) RotationUtils.setTargetRotation(currentMlgRotation.rotation, if (mlgKeepRotationEnabledValue.get()) mlgKeepRotationTicksValue.get() else 0) else currentMlgRotation.rotation.applyRotationToPlayer(thePlayer)
			}
		}
		else if (currentMlgRotation != null)
		{
			val stack = thePlayer.inventory.getStackInSlot(currentMlgItemIndex)
			if (stack != null) if (provider.isItemBucket(stack.item)) controller.sendUseItem(thePlayer, theWorld, stack) else if (controller.sendUseItem(thePlayer, theWorld, stack)) mlgTimer.reset()

			InventoryUtils.resetSlot(thePlayer)
		}
	}

	@EventTarget(ignoreCondition = true)
	fun onJump(@Suppress("UNUSED_PARAMETER") event: JumpEvent?)
	{
		jumped = true
	}

	override val tag: String
		get() = "${modeValue.get()}${if (modeValue.get().equals("SpoofGround", ignoreCase = true) || modeValue.get().equals("Packet", ignoreCase = true)) " ${noSpoofTicks.get()}" else ""}"
}
