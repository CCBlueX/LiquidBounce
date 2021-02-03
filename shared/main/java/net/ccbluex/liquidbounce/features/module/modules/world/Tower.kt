/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
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
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.truncate
import kotlin.random.Random

@ModuleInfo(name = "Tower", description = "Automatically builds a tower beneath you.", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_O)
class Tower : Module()
{
	/**
	 * OPTIONS
	 */
	private val modeValue = ListValue(
		"Mode", arrayOf(
			"Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4"
		), "Motion"
	)

	// AutoBlock
	private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
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

	// Render
	private val counterDisplayValue = BoolValue("Counter", true)

	/**
	 * MODULE
	 */

	// Target block
	private var placeInfo: PlaceInfo? = null

	// Rotation lock
	var lockRotation: Rotation? = null

	// Mode stuff
	private val timer = TickTimer()
	private val onJumpTimer = MSTimer()
	private var jumpGround = 0.0

	// AutoBlock
	private var slot = 0

	//private var oldslot = 0
	override fun onEnable()
	{
		slot = (mc.thePlayer ?: return).inventory.currentItem //oldslot = thePlayer.inventory.currentItem
	}

	override fun onDisable()
	{
		val thePlayer = mc.thePlayer ?: return

		mc.timer.timerSpeed = 1.0F
		lockRotation = null
		active = false

		// Restore to original slot
		if (slot != thePlayer.inventory.currentItem) mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{

		// Lock Rotation
		if (rotationsValue.get() && keepRotationValue.get() && lockRotationValue.get() && lockRotation != null) RotationUtils.setTargetRotation(lockRotation)

		active = false

		// OnJump
		if (onJumpValue.get() && !mc.gameSettings.keyBindJump.isKeyDown)
		{

			// Skip if jump key isn't pressed
			if (onJumpDelayValue.get() > 0) onJumpTimer.reset()
			return
		}
		else if (onJumpValue.get() && onJumpDelayValue.get() > 0 && (!onJumpTimer.hasTimePassed(
				onJumpDelayValue.get().toLong()
			) || disableOnJumpWhileMoving.get()) && (isMoving || !onJumpNoDelayIfNotMovingValue.get())) // Skip if onjump delay aren't over yet.
			return

		active = true

		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		mc.timer.timerSpeed = timerValue.get()
		val eventState = event.eventState

		// Place
		if (placeModeValue.get().equals(eventState.stateName, ignoreCase = true)) place(theWorld, thePlayer)

		// Update
		if (eventState == EventState.PRE)
		{
			placeInfo = null
			timer.update()

			val update = if (!autoBlockValue.get().equals("Off", ignoreCase = true)) InventoryUtils.findAutoBlockBlock(autoBlockFullCubeOnlyValue.get(), 0.0) != -1 || thePlayer.heldItem != null && classProvider.isItemBlock(thePlayer.heldItem!!.item)
			else thePlayer.heldItem != null && classProvider.isItemBlock(thePlayer.heldItem!!.item)

			if (update)
			{
				if (!stopWhenBlockAbove.get() || classProvider.isBlockAir(getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ)))) move()

				val blockPos = WBlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)
				if (classProvider.isBlockAir(theWorld.getBlockState(blockPos).block) && search(theWorld, blockPos) && rotationsValue.get())
				{
					val vecRotation = RotationUtils.faceBlock(blockPos)
					if (vecRotation != null)
					{
						RotationUtils.setTargetRotation(vecRotation.rotation, keepRotationTicks)
						RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))

						placeInfo!!.vec3 = vecRotation.vec
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

		when (modeValue.get().toLowerCase())
		{
			"jump" -> if (onGround && timer.hasTimePassed(jumpDelayValue.get()))
			{
				fakeJump()
				thePlayer.motionY = jumpMotionValue.get().toDouble()
				timer.reset()
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

			"packet" -> if (onGround && timer.hasTimePassed(2))
			{
				val netHandler = mc.netHandler

				fakeJump()

				netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(posX, posY + 0.42, posZ, false))
				netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(posX, posY + 0.753, posZ, false))
				thePlayer.setPosition(posX, posY + 1.0, posZ)

				timer.reset()
			}

			"teleport" ->
			{
				if (teleportNoMotionValue.get()) thePlayer.motionY = 0.0

				if ((onGround || !teleportGroundValue.get()) && timer.hasTimePassed(teleportDelayValue.get()))
				{
					fakeJump()
					thePlayer.setPositionAndUpdate(posX, posY + teleportHeightValue.get(), posZ)
					timer.reset()
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

				mc.timer.timerSpeed = 1f

				if (thePlayer.motionY < 0)
				{
					// Fast down
					thePlayer.motionY -= 0.00000945
					mc.timer.timerSpeed = 1.6f
				}
			}

			"aac3.6.4" -> if (thePlayer.ticksExisted % 4 == 1)
			{
				thePlayer.motionY = 0.4195464
				thePlayer.setPosition(posX - 0.035, posY, posZ)
			}
			else if (thePlayer.ticksExisted % 4 == 0)
			{
				thePlayer.motionY = -0.5
				thePlayer.setPosition(posX + 0.035, posY, posZ)
			}
		}
	}

	/**
	 * Place target block
	 */
	private fun place(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		if (placeInfo == null) return

		val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
		val scaffold = LiquidBounce.moduleManager[Scaffold::class.java] as Scaffold

		if (scaffold.killauraBypassValue.get().equals("SuspendKillaura", true)) killAura.suspend(suspendKillauraDuration.get().toLong())

		val netHandler = mc.netHandler

		// AutoBlock
		var itemStack = thePlayer.heldItem

		if (itemStack == null || !classProvider.isItemBlock(itemStack.item) || classProvider.isBlockBush(itemStack.item?.asItemBlock()?.block))
		{
			if (autoBlockValue.get().equals("Off", true)) return

			val blockSlot = InventoryUtils.findAutoBlockBlock(autoBlockFullCubeOnlyValue.get(), 0.0)
			if (blockSlot == -1) return

			when (autoBlockValue.get())
			{
				"Pick" ->
				{
					thePlayer.inventory.currentItem = blockSlot - 36
					mc.playerController.updateController()
				}

				"Spoof" -> if (blockSlot - 36 != slot) netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36))

				"Switch" -> if (blockSlot - 36 != slot) netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(blockSlot - 36))
			}
			itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
		}

		// Place block
		if (mc.playerController.onPlayerRightClick(
				thePlayer, theWorld, itemStack!!, placeInfo!!.blockPos, placeInfo!!.enumFacing, placeInfo!!.vec3
			)) if (swingValue.get()) thePlayer.swingItem()
		else netHandler.addToSendQueue(classProvider.createCPacketAnimation())

		// Switch back to original slot after place on AutoBlock-Switch mode
		if (autoBlockValue.get().equals("Switch", true) && slot != thePlayer.inventory.currentItem) netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

		placeInfo = null
	}

	/**
	 * Search for placeable block
	 *
	 * @param blockPosition pos
	 * @return
	 */
	private fun search(theWorld: IWorldClient, blockPosition: WBlockPos): Boolean
	{
		val thePlayer = mc.thePlayer ?: return false
		if (!isReplaceable(blockPosition)) return false

		val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
		var placeRotation: PlaceRotation? = null
		for (facingType in EnumFacingType.values())
		{
			val side = classProvider.getEnumFacing(facingType)
			val neighbor = blockPosition.offset(side)

			if (!canBeClicked(neighbor)) continue

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
						if (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || theWorld.rayTraceBlocks(
								eyesPos, hitVec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false
							) != null)
						{
							zSearch += 0.1
							continue
						}

						// face block
						val diffX = hitVec.xCoord - eyesPos.xCoord
						val diffY = hitVec.yCoord - eyesPos.yCoord
						val diffZ = hitVec.zCoord - eyesPos.zCoord
						val diffXZ = hypot(diffX, diffZ)

						val rotation = Rotation(
							WMathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f), WMathHelper.wrapAngleTo180_float((-Math.toDegrees(atan2(diffY, diffXZ))).toFloat())
						)
						val rotationVector = RotationUtils.getVectorForRotation(rotation)
						val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
						val rayTrace = theWorld.rayTraceBlocks(
							eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true
						)
						if (!(rayTrace!!.typeOfHit == IMovingObjectPosition.WMovingObjectType.BLOCK && rayTrace.blockPos == neighbor))
						{
							zSearch += 0.1
							continue
						}

						if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.rotation)) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)

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
			val scaffold = LiquidBounce.moduleManager[Scaffold::class.java] as Scaffold
			RotationUtils.setTargetRotation(placeRotation.rotation, keepRotationTicks)
			RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))
			scaffold.lockRotation = null // Prevent to lockRotation confliction
			lockRotation = placeRotation.rotation
		}
		placeInfo = placeRotation.placeInfo
		return true
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet
		if (classProvider.isCPacketHeldItemChange(packet)) slot = packet.asCPacketHeldItemChange().slotId
	}

	/**
	 * Tower visuals
	 */
	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		if (counterDisplayValue.get())
		{
			val thePlayer = mc.thePlayer ?: return

			GL11.glPushMatrix()
			val blockOverlay = LiquidBounce.moduleManager[BlockOverlay::class.java] as BlockOverlay
			if (blockOverlay.state && blockOverlay.infoValue.get() && blockOverlay.currentBlock != null) GL11.glTranslatef(0f, 15f, 0f)

			val info = "Blocks: \u00A7${if (getBlocksAmount(thePlayer) <= 10) "c" else "7"}${getBlocksAmount(thePlayer)}"
			val scaledResolution = classProvider.createScaledResolution(mc)

			RenderUtils.drawBorderedRect(
				scaledResolution.scaledWidth / 2 - 2.0f, scaledResolution.scaledHeight / 2 + 5.0f, scaledResolution.scaledWidth / 2 + Fonts.font40.getStringWidth(info) + 2.0f, scaledResolution.scaledHeight / 2 + 16.0f, 3f, Color.BLACK.rgb, Color.BLACK.rgb
			)

			classProvider.getGlStateManager().resetColor()

			Fonts.font40.drawString(
				info, scaledResolution.scaledWidth / 2.0f, scaledResolution.scaledHeight / 2 + 7.0f, Color.WHITE.rgb
			)
			GL11.glPopMatrix()
		}
	}

	@EventTarget
	fun onJump(event: JumpEvent)
	{
		if (onJumpValue.get() && (onJumpDelayValue.get() > 0 && onJumpTimer.hasTimePassed(onJumpDelayValue.get().toLong()) && !disableOnJumpWhileMoving.get() || !isMoving && onJumpNoDelayIfNotMovingValue.get())) event.cancelEvent()
	}

	/**
	 * @return hotbar blocks amount
	 */
	private fun getBlocksAmount(thePlayer: IEntityPlayerSP): Int
	{
		var amount = 0

		for (i in 36..44)
		{
			val itemStack = thePlayer.inventoryContainer.getSlot(i).stack
			if (itemStack != null && classProvider.isItemBlock(itemStack.item))
			{
				val block = itemStack.item!!.asItemBlock().block
				if (thePlayer.heldItem == itemStack || InventoryUtils.canAutoBlock(block)) amount += itemStack.stackSize
			}
		}

		return amount
	}

	override val tag: String
		get() = modeValue.get()

	var active = false

	private val keepRotationTicks: Int
		get() = if (keepRotationValue.get()) if (maxKeepRotationTicksValue.get() == minKeepRotationTicksValue.get()) maxKeepRotationTicksValue.get()
		else minKeepRotationTicksValue.get() + Random.nextInt(maxKeepRotationTicksValue.get() - minKeepRotationTicksValue.get())
		else 0
}
