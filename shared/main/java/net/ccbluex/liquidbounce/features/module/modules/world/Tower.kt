/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoUse
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.truncate
import kotlin.random.Random

@ModuleInfo(name = "Tower", description = "Automatically builds a tower beneath you.", category = ModuleCategory.WORLD, defaultKeyBinds = [Keyboard.KEY_O])
class Tower : Module()
{
	/**
	 * OPTIONS
	 */
	private val modeValue = ListValue("Mode", arrayOf("Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4", "AAC4.4-Constant", "AAC4-Jump"), "Motion")

	// AutoBlock
	private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
	private val autoBlockSwitchKeepTimeValue = IntegerValue("AutoBlockSwitchKeepTime", -1, -1, 10)
	private val autoBlockFullCubeOnlyValue = BoolValue("AutoBlockFullCubeOnly", false)

	private val swingValue = BoolValue("Swing", true)
	private val stopWhenBlockAbove = BoolValue("StopWhenBlockAbove", false)

	// Rotation
	private val rotationsValue = BoolValue("Rotations", true)
	private val keepRotationValue = BoolValue("KeepRotation", false)
	private val minKeepRotationTicksValue: IntegerValue = object : IntegerValue("MinKeepRotationTicks", 20, 0, 50)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxKeepRotationTicksValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val maxKeepRotationTicksValue: IntegerValue = object : IntegerValue("MaxKeepRotationTicks", 30, 0, 50)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minKeepRotationTicksValue.get()
			if (i > newValue) this.set(i)
		}
	}
	private val lockRotationValue = BoolValue("LockRotation", false)

	// Reset Turn Speed
	private val maxResetTurnSpeed: FloatValue = object : FloatValue("MaxRotationResetSpeed", 180f, 20f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minResetTurnSpeed.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minResetTurnSpeed: FloatValue = object : FloatValue("MinRotationResetSpeed", 180f, 20f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxResetTurnSpeed.get()
			if (v < newValue) this.set(v)
		}
	}

	// OnJump
	private val onJumpValue = BoolValue("OnJump", false)
	private val onJumpDelayValue = IntegerValue("OnJumpDelay", 500, 0, 1000)
	private val onJumpNoDelayIfNotMovingValue = BoolValue("OnJumpNoDelayIfNotMoving", true)
	private val disableOnJumpWhileMoving: BoolValue = object : BoolValue("DisableOnJumpWhileMoving", true)
	{
		override fun onChanged(oldValue: Boolean, newValue: Boolean)
		{
			if (newValue && !onJumpNoDelayIfNotMovingValue.get()) onJumpNoDelayIfNotMovingValue.set(true)
		}
	}
	private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

	private val timerValue = FloatValue("Timer", 1f, 0.01f, 10f)

	// Jump mode
	private val jumpMotionValue = FloatValue("JumpMotion", 0.42f, 0.3681289f, 0.79f)
	private val jumpDelayValue = IntegerValue("JumpDelay", 0, 0, 20)

	// ConstantMotion mode
	private val constantMotionValue = FloatValue("ConstantMotion", 0.42f, 0.1f, 1f)
	private val constantMotionJumpGroundValue = FloatValue("ConstantMotionJumpGround", 0.79f, 0.76f, 1f)

	// Teleport mode
	private val teleportHeightValue = FloatValue("TeleportHeight", 1.15f, 0.1f, 5f)
	private val teleportDelayValue = IntegerValue("TeleportDelay", 0, 0, 20)
	private val teleportGroundValue = BoolValue("TeleportGround", true)
	private val teleportNoMotionValue = BoolValue("TeleportNoMotion", false)

	// Killaura bypass (Other settings are same as scaffold's)
	private val suspendKillauraDuration = IntegerValue("SuspendKillauraDuration", 500, 250, 1000)

	private val stopConsumingBeforePlaceValue = BoolValue("StopConsumingBeforePlace", true)

	// Render
	val counterDisplayValue = BoolValue("Counter", true)

	private val noCustomTimer = arrayOf("aac3.3.9", "aac4.4-constant", "aac4-jump")

	/**
	 * MODULE
	 */

	// Target block
	private var placeInfo: PlaceInfo? = null

	// Rotation lock
	var lockRotation: Rotation? = null

	// Mode stuff
	private val delayTimer = TickTimer()
	private val onJumpTimer = MSTimer()
	private var jumpGround = 0.0

	override fun onDisable()
	{
		val thePlayer = mc.thePlayer ?: return

		mc.timer.timerSpeed = 1.0F
		lockRotation = null
		active = false

		// Restore to original slot
		if (InventoryUtils.serverHeldItemSlot != thePlayer.inventory.currentItem) InventoryUtils.reset()
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{

		// Lock Rotation
		if (rotationsValue.get() && keepRotationValue.get() && lockRotationValue.get() && lockRotation != null) RotationUtils.setTargetRotation(lockRotation)

		active = false

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val timer = mc.timer

		// OnJump
		if (onJumpValue.get() && !mc.gameSettings.keyBindJump.isKeyDown)
		{
			// Skip if jump key isn't pressed
			if (onJumpDelayValue.get() > 0) onJumpTimer.reset()

			return
		}
		else if (onJumpValue.get() && onJumpDelayValue.get() > 0 && (!onJumpTimer.hasTimePassed(onJumpDelayValue.get().toLong()) || disableOnJumpWhileMoving.get()) && (isMoving(thePlayer) || !onJumpNoDelayIfNotMovingValue.get())) // Skip if onjump delay aren't over yet.
			return

		active = true

		if (modeValue.get().toLowerCase() !in noCustomTimer) timer.timerSpeed = timerValue.get()

		val eventState = event.eventState

		// Place
		if (placeModeValue.get().equals(eventState.stateName, ignoreCase = true)) place(theWorld, thePlayer)

		// Update
		if (eventState == EventState.PRE)
		{
			placeInfo = null
			delayTimer.update()

			val provider = classProvider

			val heldItem = thePlayer.heldItem

			if (if (!autoBlockValue.get().equals("Off", ignoreCase = true)) InventoryUtils.findAutoBlockBlock(theWorld, thePlayer.inventoryContainer, autoBlockFullCubeOnlyValue.get()) != -1 || heldItem != null && provider.isItemBlock(heldItem.item) else heldItem != null && provider.isItemBlock(heldItem.item))
			{
				if (!stopWhenBlockAbove.get() || provider.isBlockAir(getBlock(theWorld, WBlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ)))) move()

				val blockPos = WBlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)
				if (provider.isBlockAir(theWorld.getBlockState(blockPos).block) && search(theWorld, thePlayer, blockPos) && rotationsValue.get())
				{
					val vecRotation = RotationUtils.faceBlock(theWorld, thePlayer, blockPos)
					if (vecRotation != null)
					{
						RotationUtils.setTargetRotation(vecRotation.rotation, keepRotationTicks)
						RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))

						placeInfo!!.vec3 = vecRotation.vec // Is this redundant?
					}
				}
			}
		}
	}

	/**
	 * Send jump packets, bypasses stat-based cheat detections like Hypixel watchdog.
	 */
	private fun fakeJump()
	{
		val thePlayer = mc.thePlayer ?: return

		thePlayer.isAirBorne = true
		thePlayer.triggerAchievement(classProvider.getStatEnum(StatType.JUMP_STAT))
	}

	/**
	 * Move player
	 */
	private fun move()
	{
		val thePlayer = mc.thePlayer ?: return

		val posX = thePlayer.posX
		val posY = thePlayer.posY
		val posZ = thePlayer.posZ
		val onGround = thePlayer.onGround
		val timer = mc.timer

		when (modeValue.get().toLowerCase())
		{
			"jump" -> if (onGround && delayTimer.hasTimePassed(jumpDelayValue.get()))
			{
				fakeJump()
				thePlayer.motionY = jumpMotionValue.get().toDouble()
				delayTimer.reset()
			}

			"motion" -> if (onGround)
			{
				fakeJump()
				thePlayer.motionY = 0.42
			}
			else if (thePlayer.motionY < 0.1) thePlayer.motionY = -0.3

			"motiontp" -> if (onGround)
			{
				fakeJump()
				thePlayer.motionY = 0.42
			}
			else if (thePlayer.motionY < 0.23) thePlayer.setPosition(posX, truncate(posY), posZ)

			"packet" -> if (onGround && delayTimer.hasTimePassed(2))
			{
				val netHandler = mc.netHandler

				fakeJump()

				val provider = classProvider

				netHandler.addToSendQueue(provider.createCPacketPlayerPosition(posX, posY + 0.42, posZ, false))
				netHandler.addToSendQueue(provider.createCPacketPlayerPosition(posX, posY + 0.753, posZ, false))
				thePlayer.setPosition(posX, posY + 1.0, posZ)

				delayTimer.reset()
			}

			"teleport" ->
			{
				if (teleportNoMotionValue.get()) thePlayer.motionY = 0.0

				if ((onGround || !teleportGroundValue.get()) && delayTimer.hasTimePassed(teleportDelayValue.get()))
				{
					fakeJump()
					thePlayer.setPositionAndUpdate(posX, posY + teleportHeightValue.get(), posZ)
					delayTimer.reset()
				}
			}

			"constantmotion" ->
			{
				val constantMotion = constantMotionValue.get().toDouble()

				if (onGround)
				{
					fakeJump()
					jumpGround = posY
					thePlayer.motionY = constantMotion
				}

				if (posY > jumpGround + constantMotionJumpGroundValue.get())
				{
					fakeJump()
					thePlayer.setPosition(posX, truncate(posY), posZ) // TODO: toInt() required?
					thePlayer.motionY = constantMotion
					jumpGround = posY
				}
			}

			"aac3.3.9" ->
			{
				if (onGround)
				{
					fakeJump()
					thePlayer.motionY = 0.4001
				}

				timer.timerSpeed = 1f

				if (thePlayer.motionY < 0.0)
				{
					// Fast down
					thePlayer.motionY -= 0.00000945
					timer.timerSpeed = 1.6f
				}
			}

			"aac3.6.4" -> when (thePlayer.ticksExisted % 4)
			{
				0 ->
				{
					thePlayer.motionY = -0.5
					thePlayer.setPosition(posX + 0.035, posY, posZ)
				}

				1 ->
				{
					thePlayer.motionY = 0.4195464
					thePlayer.setPosition(posX - 0.035, posY, posZ)
				}
			}

			"aac4.4-constant" ->
			{
				if (thePlayer.onGround)
				{
					fakeJump()
					jumpGround = thePlayer.posY
					thePlayer.motionY = 0.42
				}

				thePlayer.motionX = 0.0
				thePlayer.motionZ = -0.00000001
				thePlayer.jumpMovementFactor = 0.000F
				timer.timerSpeed = 0.60f

				if (thePlayer.posY > jumpGround + 0.99)
				{
					fakeJump()
					thePlayer.setPosition(thePlayer.posX, thePlayer.posY - 0.001335979112146, thePlayer.posZ)
					thePlayer.motionY = 0.42
					jumpGround = thePlayer.posY
					timer.timerSpeed = 0.75f
				}
			}

			"aac4-jump" ->
			{
				timer.timerSpeed = 0.97f

				if (thePlayer.onGround)
				{
					fakeJump()
					thePlayer.motionY = 0.387565
					timer.timerSpeed = 1.05f
				}
			}
		}
	}

	/**
	 * Place target block
	 */
	private fun place(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val placeInfo = placeInfo ?: return

		val moduleManager = LiquidBounce.moduleManager

		val killAura = moduleManager[KillAura::class.java] as KillAura
		val scaffold = moduleManager[Scaffold::class.java] as Scaffold

		if (scaffold.killauraBypassValue.get().equals("SuspendKillaura", true)) killAura.suspend(suspendKillauraDuration.get().toLong())

		val netHandler = mc.netHandler
		val controller = mc.playerController
		val inventory = thePlayer.inventory

		val provider = classProvider

		(LiquidBounce.moduleManager[AutoUse::class.java] as AutoUse).endEating(thePlayer, classProvider, netHandler)

		// AutoBlock
		val slot = InventoryUtils.serverHeldItemSlot ?: inventory.currentItem
		var itemStack = inventory.mainInventory[slot]

		val switchKeepTime = autoBlockSwitchKeepTimeValue.get()

		if (itemStack == null || !provider.isItemBlock(itemStack.item) || provider.isBlockBush(itemStack.item?.asItemBlock()?.block))
		{
			if (autoBlockValue.get().equals("Off", true)) return

			val blockSlot = InventoryUtils.findAutoBlockBlock(theWorld, thePlayer.inventoryContainer, autoBlockFullCubeOnlyValue.get())
			if (blockSlot == -1) return

			when (val autoBlockMode = autoBlockValue.get().toLowerCase())
			{
				"pick" ->
				{
					inventory.currentItem = blockSlot - 36
					controller.updateController()
				}

				"spoof", "switch" -> if (blockSlot - 36 != slot)
				{
					if (InventoryUtils.setHeldItemSlot(blockSlot - 36, if (autoBlockMode.equals("spoof", ignoreCase = true)) -1 else switchKeepTime, false)) return
				}
				else InventoryUtils.reset()
			}

			itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
		}

		// CPSCounter support
		CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

		if (thePlayer.isUsingItem && stopConsumingBeforePlaceValue.get()) mc.playerController.onStoppedUsingItem(thePlayer)

		// Place block
		if (controller.onPlayerRightClick(thePlayer, theWorld, itemStack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)) if (swingValue.get()) thePlayer.swingItem() else netHandler.addToSendQueue(provider.createCPacketAnimation())

		// Switch back to original slot after place on AutoBlock-Switch mode
		if (autoBlockValue.get().equals("Switch", true) && switchKeepTime < 0) InventoryUtils.reset()

		this.placeInfo = null
	}

	/**
	 * Search for placeable block
	 *
	 * @param blockPosition pos
	 * @return
	 */
	private fun search(theWorld: IWorld, thePlayer: IEntity, blockPosition: WBlockPos): Boolean
	{
		if (!isReplaceable(theWorld, blockPosition)) return false

		val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
		var placeRotation: PlaceRotation? = null

		val provider = classProvider

		EnumFacingType.values().map(provider::getEnumFacing).forEach { side ->
			val neighbor = blockPosition.offset(side)

			if (!canBeClicked(theWorld, neighbor)) return@forEach

			val dirVec = WVec3(side.directionVec)

			var xSearch = 0.1
			while (xSearch < 0.9)
			{
				var ySearch = 0.1
				while (ySearch < 0.9)
				{
					var zSearch = 0.1
					while (zSearch < 0.9)
					{
						val posVec = WVec3(blockPosition).addVector(xSearch, ySearch, zSearch)
						val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
						val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))
						if (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || theWorld.rayTraceBlocks(eyesPos, hitVec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) != null)
						{
							zSearch += 0.1
							continue
						}

						// face block
						val diffX = hitVec.xCoord - eyesPos.xCoord
						val diffY = hitVec.yCoord - eyesPos.yCoord
						val diffZ = hitVec.zCoord - eyesPos.zCoord
						val diffXZ = hypot(diffX, diffZ)

						val rotation = Rotation(WMathHelper.wrapAngleTo180_float(WMathHelper.toDegrees(atan2(diffZ, diffX).toFloat()) - 90f), WMathHelper.wrapAngleTo180_float((-WMathHelper.toDegrees(atan2(diffY, diffXZ).toFloat()))))
						val rotationVector = RotationUtils.getVectorForRotation(rotation)
						val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
						val rayTrace = theWorld.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)

						if (rayTrace != null && (rayTrace.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos != neighbor))
						{
							zSearch += 0.1
							continue
						}

						if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation!!.rotation)) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)

						zSearch += 0.1
					}
					ySearch += 0.1
				}
				xSearch += 0.1
			}
		}

		if (placeRotation == null) return false

		if (rotationsValue.get())
		{
			// Rotate
			RotationUtils.setTargetRotation(placeRotation!!.rotation, keepRotationTicks)
			RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))

			// Lock Rotation
			(LiquidBounce.moduleManager[Scaffold::class.java] as Scaffold).lockRotation = null // Prevent to lockRotation confliction
			lockRotation = placeRotation!!.rotation
		}

		placeInfo = placeRotation!!.placeInfo

		return true
	}

	/**
	 * Tower visuals
	 */
	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		if (counterDisplayValue.get())
		{
			val theWorld = mc.theWorld ?: return
			val thePlayer = mc.thePlayer ?: return

			GL11.glPushMatrix()
			val blockOverlay = LiquidBounce.moduleManager[BlockOverlay::class.java] as BlockOverlay
			if (blockOverlay.state && blockOverlay.infoValue.get() && blockOverlay.getCurrentBlock(theWorld) != null) GL11.glTranslatef(0f, 15f, 0f)

			val blocksAmount = getBlocksAmount(thePlayer)
			val info = "Blocks: \u00A7${if (blocksAmount <= 10) "c" else "7"}$blocksAmount"

			val provider = classProvider

			val scaledResolution = provider.createScaledResolution(mc)

			val middleScreenX = scaledResolution.scaledWidth shr 1
			val middleScreenY = scaledResolution.scaledHeight shr 1
			RenderUtils.drawBorderedRect(middleScreenX - 2.0f, middleScreenY + 5.0f, ((scaledResolution.scaledWidth shr 1) + Fonts.font40.getStringWidth(info)) + 2.0f, middleScreenY + 16.0f, 3f, -16777216, -16777216)

			provider.glStateManager.resetColor()

			Fonts.font40.drawString(info, middleScreenX.toFloat(), middleScreenY + 7.0f, 0xffffff)
			GL11.glPopMatrix()
		}
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (!onJumpValue.get()) return

		val onJumpDelay = onJumpDelayValue.get()

		if (onJumpDelay > 0 && onJumpTimer.hasTimePassed(onJumpDelay.toLong()) && !disableOnJumpWhileMoving.get() || !isMoving(thePlayer) && onJumpNoDelayIfNotMovingValue.get()) event.cancelEvent()
	}

	/**
	 * @return hotbar blocks amount
	 */
	private fun getBlocksAmount(thePlayer: IEntityPlayer): Int
	{
		val provider = classProvider

		val inventoryContainer = thePlayer.inventoryContainer

		return (36..44).mapNotNull { inventoryContainer.getSlot(it).stack }.filter { provider.isItemBlock(it.item) }.filter { thePlayer.heldItem == it || InventoryUtils.canAutoBlock(it.item?.asItemBlock()?.block) }.sumBy(IItemStack::stackSize)
	}

	override val tag: String
		get() = modeValue.get()

	var active = false

	private val keepRotationTicks: Int
		get() = if (keepRotationValue.get()) if (maxKeepRotationTicksValue.get() == minKeepRotationTicksValue.get()) maxKeepRotationTicksValue.get() else minKeepRotationTicksValue.get() + Random.nextInt(maxKeepRotationTicksValue.get() - minKeepRotationTicksValue.get()) else 0
}
