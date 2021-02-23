/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

@file:Suppress("BooleanLiteralArgument")

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketHeldItemChange
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.*
import kotlin.random.Random

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_I)
class Scaffold : Module()
{

	private val modeValue = ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand"), "Normal")

	// Delay
	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val minDelay = minDelayValue.get()
			if (minDelay > newValue) set(minDelay)
		}
	}

	private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val maxDelay = maxDelayValue.get()
			if (maxDelay < newValue) set(maxDelay)
		}
	}

	// Placeable delay
	private val placeableDelay = BoolValue("PlaceableDelay", true)

	// Autoblock
	private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
	private val autoBlockFullCubeOnlyValue = BoolValue("AutoBlockFullCubeOnly", false)
	private val maxSwitchDelayValue: IntegerValue = object : IntegerValue("MaxSwitchSlotDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minSwitchDelayValue.get()
			if (i > newValue) set(i)
		}
	}
	private val minSwitchDelayValue: IntegerValue = object : IntegerValue("MinSwitchSlotDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxSwitchDelayValue.get()
			if (i < newValue) set(i)
		}
	}

	// Basic stuff
	val sprintValue: BoolValue = BoolValue("Sprint", false)
	private val swingValue = BoolValue("Swing", true)
	private val downValue = BoolValue("Down", true)
	private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

	// Eagle
	private val eagleValue = ListValue("Eagle", arrayOf("Normal", "EdgeDistance", "Silent", "Off"), "Normal")
	private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10)
	private val edgeDistanceValue = FloatValue("EagleEdgeDistance", 0.2f, 0f, 0.5f)

	// Expand
	private val expandLengthValue = IntegerValue("ExpandLength", 1, 1, 6)

	// Tower support
	private val disableWhileTowering: BoolValue = BoolValue("DisableWhileTowering", true)

	// Rotation Options
	private val rotationStrafeValue = BoolValue("RotationStrafe", false)
	private val rotationModeValue = ListValue("RotationMode", arrayOf("Off", "Normal", "Static", "StaticPitch", "StaticYaw"), "Normal")
	private val silentRotationValue = BoolValue("SilentRotation", true)
	private val staticPitchValue = FloatValue("StaticPitchOffSet", 86f, 70f, 90f)
	private val staticYawValue = FloatValue("StaticYawOffSet", 0f, 0f, 90f)
	private val keepRotationValue = BoolValue("KeepRotation", true)

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

	// xz + y range
	private val xzRangeValue = FloatValue("XZRange", 0.8f, 0f, 1f)
	private val yRangeValue = FloatValue("YRange", 0.8f, 0f, 1f)
	private val minDiffValue = FloatValue("MinDiff", 0.0f, 0.0f, 0.2f)

	// Search
	private val searchValue = BoolValue("Search", true)

	// Search Accuracy
	private val searchAccuracyValue: IntegerValue = object : IntegerValue("SearchAccuracy", 8, 1, 16)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			if (maximum < newValue)
			{
				set(maximum)
			}
			else if (minimum > newValue)
			{
				set(minimum)
			}
		}
	}

	private val checkVisibleValue = BoolValue("CheckVisible", true)
	private val ySearchValue = BoolValue("YSearch", false)

	// Turn Speed
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) set(v)
			if (maximum < newValue)
			{
				set(maximum)
			}
			else if (minimum > newValue)
			{
				set(minimum)
			}
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) set(v)
			if (maximum < newValue)
			{
				set(maximum)
			}
			else if (minimum > newValue)
			{
				set(minimum)
			}
		}
	}

	// Zitter
	private val zitterValue = BoolValue("Zitter", false)
	private val zitterModeValue = ListValue("ZitterMode", arrayOf("Teleport", "Smooth"), "Teleport")
	private val zitterSpeed = FloatValue("ZitterSpeed", 0.13f, 0.1f, 2f)
	private val zitterStrength = FloatValue("ZitterStrength", 0.05f, 0f, 0.2f)

	// Game
	private val timerValue = FloatValue("Timer", 1f, 0.1f, 10f)
	private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)

	// Slow mode
	private val slowValue = object : BoolValue("Slow", false)
	{
		override fun onChanged(oldValue: Boolean, newValue: Boolean)
		{
			if (newValue) sprintValue.set(false)
		}
	}

	private val slowSpeed = FloatValue("SlowSpeed", 0.6f, 0.2f, 0.8f)

	// Safety
	private val sameYValue = BoolValue("SameY", false)
	private val safeWalkValue = BoolValue("SafeWalk", true)
	private val airSafeValue = BoolValue("AirSafe", false)

	// Killaura bypass
	val killauraBypassValue = ListValue("KillauraBypassMode", arrayOf("None", "SuspendKillaura", "WaitForKillauraEnd"), "SuspendKillaura")

	private val suspendKillauraDuration = IntegerValue("SuspendKillauraDuration", 300, 300, 1000)

	// Visuals
	val counterDisplayValue = BoolValue("Counter", true)
	private val markValue = BoolValue("Mark", false)
	private val searchDebug = BoolValue("SearchDebugChat", false)

	// MODULE

	// Target block
	private var targetPlace: PlaceInfo? = null
	private var lastSearchBound: SearchBounds? = null

	// Rotation lock
	var lockRotation: Rotation? = null
	private var limitedRotation: Rotation? = null

	// Launch position
	private var launchY = 0
	private var facesBlock = false

	// AutoBlock
	private var slot = 0 //private var oldslot = 0

	// Zitter Direction
	private var zitterDirection = false

	// Delay
	private val delayTimer = MSTimer()
	private val zitterTimer = MSTimer()
	private val switchTimer = MSTimer()
	private var delay = 0L
	private var switchDelay = 0L

	// Eagle
	private var placedBlocksWithoutEagle = 0
	private var eagleSneaking: Boolean = false

	// Downwards
	private var shouldGoDown: Boolean = false

	// Last Ground Block
	private var lastGroundBlockState: IIBlockState? = null
	private var lastGroundBlockPos: WBlockPos? = null
	private var lastGroundBlockBB: IAxisAlignedBB? = null

	private var lastSearchPosition: WBlockPos? = null

	// Falling Started On YPosition
	private var fallStartY = 0.0

	// ENABLING MODULE
	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		launchY = thePlayer.posY.toInt()
		fallStartY = 0.0
		slot = thePlayer.inventory.currentItem //oldslot = mc.thePlayer!!.inventory.currentItem
	}

	// UPDATE EVENTS

	@EventTarget
	private fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		mc.timer.timerSpeed = timerValue.get()

		shouldGoDown = downValue.get() && !sameYValue.get() && gameSettings.isKeyDown(gameSettings.keyBindSneak) && getBlocksAmount(thePlayer) > 1
		if (shouldGoDown) gameSettings.keyBindSneak.pressed = false

		// Slow
		if (slowValue.get())
		{
			thePlayer.motionX = thePlayer.motionX * slowSpeed.get()
			thePlayer.motionZ = thePlayer.motionZ * slowSpeed.get()
		}

		// Sprint
		// This can cause compatibility issue with other mods which tamper the sprinting state (example: BetterSprinting)
		//		if (sprintValue.get())
		//		{
		//			if (!mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSprint)) mc.gameSettings.keyBindSprint.pressed = false
		//			if (mc.gameSettings.isKeyDown(mc.gameSettings.keyBindSprint)) mc.gameSettings.keyBindSprint.pressed = true
		//			if (mc.gameSettings.keyBindSprint.isKeyDown) thePlayer.sprinting = true
		//			if (!mc.gameSettings.keyBindSprint.isKeyDown) thePlayer.sprinting = false
		//		}

		if (thePlayer.onGround)
		{
			if (modeValue.get().equals("Rewinside", ignoreCase = true))
			{
				MovementUtils.strafe(thePlayer, 0.2F)
				thePlayer.motionY = 0.0
			}

			// Smooth Zitter
			if (zitterValue.get() && zitterModeValue.get().equals("Smooth", true))
			{
				if (!gameSettings.isKeyDown(gameSettings.keyBindRight)) gameSettings.keyBindRight.pressed = false
				if (!gameSettings.isKeyDown(gameSettings.keyBindLeft)) gameSettings.keyBindLeft.pressed = false
				if (zitterTimer.hasTimePassed(100))
				{
					zitterDirection = !zitterDirection
					zitterTimer.reset()
				}
				if (zitterDirection)
				{
					gameSettings.keyBindRight.pressed = true
					gameSettings.keyBindLeft.pressed = false
				}
				else
				{
					gameSettings.keyBindRight.pressed = false
					gameSettings.keyBindLeft.pressed = true
				}
			}

			// Eagle
			if (!eagleValue.get().equals("Off", true) && !shouldGoDown)
			{
				var dif = 0.5
				if (eagleValue.get().equals("EdgeDistance", true) && !shouldGoDown)
				{
					repeat(4) {
						when (it)
						{
							0 ->
							{
								val blockPos = WBlockPos(thePlayer.posX - 1.0, thePlayer.posY - (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) 0.0 else 1.0), thePlayer.posZ)
								val placeInfo: PlaceInfo? = PlaceInfo.get(theWorld, blockPos)
								if (isReplaceable(blockPos) && placeInfo != null)
								{
									var calcDif: Double = thePlayer.posX - blockPos.x
									calcDif -= 0.5

									if (calcDif < 0)
									{
										calcDif *= -1.0
										calcDif -= 0.5
									}
									if (calcDif < dif)
									{
										dif = calcDif
									}
								}
							}

							1 ->
							{
								val blockPos = WBlockPos(thePlayer.posX + 1.0, thePlayer.posY - (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) 0.0 else 1.0), thePlayer.posZ)
								val placeInfo: PlaceInfo? = PlaceInfo.get(theWorld, blockPos)

								if (isReplaceable(blockPos) && placeInfo != null)
								{
									var calcDif: Double = thePlayer.posX - blockPos.x
									calcDif -= 0.5

									if (calcDif < 0)
									{
										calcDif *= -1.0
										calcDif -= 0.5
									}

									if (calcDif < dif) dif = calcDif
								}
							}

							2 ->
							{
								val blockPos = WBlockPos(thePlayer.posX, thePlayer.posY - (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) 0.0 else 1.0), thePlayer.posZ - 1.0)
								val placeInfo: PlaceInfo? = PlaceInfo.get(theWorld, blockPos)

								if (isReplaceable(blockPos) && placeInfo != null)
								{
									var calcDif: Double = thePlayer.posZ - blockPos.z
									calcDif -= 0.5

									if (calcDif < 0)
									{
										calcDif *= -1.0
										calcDif -= 0.5
									}

									if (calcDif < dif) dif = calcDif
								}
							}

							3 ->
							{
								val blockPos = WBlockPos(thePlayer.posX, thePlayer.posY - (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) 0.0 else 1.0), thePlayer.posZ + 1.0)
								val placeInfo: PlaceInfo? = PlaceInfo.get(theWorld, blockPos)

								if (isReplaceable(blockPos) && placeInfo != null)
								{
									var calcDif: Double = thePlayer.posZ - blockPos.z
									calcDif -= 0.5

									if (calcDif < 0)
									{
										calcDif *= -1
										calcDif -= 0.5
									}

									if (calcDif < dif) dif = calcDif
								}
							}
						}
					}
				}

				if (placedBlocksWithoutEagle >= blocksToEagleValue.get())
				{
					val shouldEagle: Boolean = theWorld.getBlockState(WBlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)).block == (classProvider.getBlockEnum(BlockType.AIR)) || (dif < edgeDistanceValue.get() && eagleValue.get().equals("EdgeDistance", true))
					if (eagleValue.get().equals("Silent", true) && !shouldGoDown)
					{
						if (eagleSneaking != shouldEagle) mc.netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, if (shouldEagle) ICPacketEntityAction.WAction.START_SNEAKING else ICPacketEntityAction.WAction.STOP_SNEAKING))
						eagleSneaking = shouldEagle
					}
					else
					{
						gameSettings.keyBindSneak.pressed = shouldEagle
						placedBlocksWithoutEagle = 0
					}
				}
				else placedBlocksWithoutEagle++
			}

			// Teleport Zitter
			if (zitterValue.get() && zitterModeValue.get().equals("teleport", true))
			{
				MovementUtils.strafe(thePlayer, zitterSpeed.get())

				val yaw = WMathHelper.toRadians(thePlayer.rotationYaw + if (zitterDirection) 90.0F else -90.0F)
				thePlayer.motionX = thePlayer.motionX - functions.sin(yaw) * zitterStrength.get()
				thePlayer.motionZ = thePlayer.motionZ + functions.cos(yaw) * zitterStrength.get()
				zitterDirection = !zitterDirection
			}
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet: IPacket = event.packet
		if (classProvider.isCPacketHeldItemChange(packet))
		{
			val packetHeldItemChange: ICPacketHeldItemChange = packet.asCPacketHeldItemChange()
			slot = packetHeldItemChange.slotId
		}
	}

	@EventTarget
	fun onStrafe(event: StrafeEvent)
	{
		if (!rotationStrafeValue.get()) return
		RotationUtils.serverRotation.applyStrafeToPlayer(event)
		event.cancelEvent()
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val eventState: EventState = event.eventState
		val tower = LiquidBounce.moduleManager[Tower::class.java] as Tower
		if (disableWhileTowering.get() && tower.active) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		// Lock Rotation
		if (!rotationModeValue.get().equals("Off", true) && keepRotationValue.get() && lockRotationValue.get() && lockRotation != null) setRotation(thePlayer, lockRotation!!)

		// Place block
		if ((facesBlock || rotationModeValue.get().equals("Off", true)) && placeModeValue.get().equals(eventState.stateName, true)) place(theWorld, thePlayer)

		// Update and search for a new block
		if (eventState == EventState.PRE) update(theWorld, thePlayer)

		// Reset placeable delay
		if (targetPlace == null && placeableDelay.get()) delayTimer.reset()
	}

	fun update(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val isNotHeldItemBlock: Boolean = thePlayer.heldItem == null || !classProvider.isItemBlock(thePlayer.heldItem!!.item)
		if (if (autoBlockValue.get().equals("Off", true)) isNotHeldItemBlock else InventoryUtils.findAutoBlockBlock(theWorld, thePlayer.inventoryContainer, autoBlockFullCubeOnlyValue.get(), 0.0) == -1 && isNotHeldItemBlock) return

		val groundSearchDepth = 0.2

		val pos = WBlockPos(thePlayer.posX, thePlayer.posY - groundSearchDepth, thePlayer.posZ)
		val bs: IIBlockState = theWorld.getBlockState(pos)
		if ( /* (this.lastGroundBlockState == null || !pos.equals(this.lastGroundBlockPos)) && */!isReplaceable(bs))
		{
			lastGroundBlockState = bs
			lastGroundBlockPos = pos
			lastGroundBlockBB = bs.block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, bs)
		}

		if (!thePlayer.onGround && thePlayer.motionY < 0)
		{
			if (fallStartY < thePlayer.posY) fallStartY = thePlayer.posY
		}
		else fallStartY = 0.0

		findBlock(theWorld, thePlayer, modeValue.get().equals("expand", true))
	}

	private fun setRotation(thePlayer: IEntityPlayerSP, rotation: Rotation, keepRotation: Int)
	{
		if (silentRotationValue.get())
		{
			RotationUtils.setTargetRotation(rotation, keepRotation)
			RotationUtils.setNextResetTurnSpeed(minResetTurnSpeed.get().coerceAtLeast(20F), maxResetTurnSpeed.get().coerceAtLeast(20F))
		}
		else
		{
			thePlayer.rotationYaw = rotation.yaw
			thePlayer.rotationPitch = rotation.pitch
		}
	}

	private fun setRotation(thePlayer: IEntityPlayerSP, rotation: Rotation)
	{
		setRotation(thePlayer, rotation, 0)
	}

	// Search for new target block
	private fun findBlock(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, expand: Boolean)
	{

		//		val blockPosition: WBlockPos = if (shouldGoDown) (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) WBlockPos(
		//			thePlayer.posX, thePlayer.posY - 0.6, thePlayer.posZ
		//		) else WBlockPos(
		//			thePlayer.posX, thePlayer.posY - 0.6, thePlayer.posZ
		//		).down()) else (if (sameYValue.get() && launchY <= thePlayer.posY) WBlockPos(
		//			thePlayer.posX, launchY - 1.0, thePlayer.posZ
		//		) else (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) WBlockPos(thePlayer) else WBlockPos(
		//			thePlayer.posX, thePlayer.posY, thePlayer.posZ
		//		).down()))

		val groundBlockState = lastGroundBlockState ?: return
		val groundBlockBB = lastGroundBlockBB ?: return
		val groundBlock: IBlock = groundBlockState.block

		// get the block that will be automatically placed
		var autoblock: IItemStack? = thePlayer.heldItem

		val provider = classProvider

		if (autoblock == null || !provider.isItemBlock(autoblock.item) || autoblock.stackSize <= 0 || !InventoryUtils.canAutoBlock(autoblock.item!!.asItemBlock().block))
		{
			if (autoBlockValue.get().equals("Off", true)) return

			val autoBlockSlot = InventoryUtils.findAutoBlockBlock(theWorld, thePlayer.inventoryContainer, autoBlockFullCubeOnlyValue.get(), 0.0)
			if (autoBlockSlot == -1) return

			autoblock = thePlayer.inventoryContainer.getSlot(autoBlockSlot).stack
		}

		val autoblockBlock = autoblock!!.item!!.asItemBlock().block

		// Configure place-position
		val searchPosition: WBlockPos
		var pos = WBlockPos(thePlayer)

		var sameY = false
		if (sameYValue.get() && launchY != -999)
		{
			pos = WBlockPos(thePlayer.posX, launchY - 1.0, thePlayer.posZ)
			sameY = true
		}

		val abCollisionBB = autoblockBlock.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, if (functions.isBlockEqualTo(groundBlock, autoblockBlock)) groundBlockState else autoblockBlock.defaultState!!)!!
		val gBB = provider.createAxisAlignedBB(groundBlock.getBlockBoundsMinX(), groundBlock.getBlockBoundsMinY(), groundBlock.getBlockBoundsMinZ(), groundBlock.getBlockBoundsMaxX(), groundBlock.getBlockBoundsMaxY(), groundBlock.getBlockBoundsMaxZ())

		// These delta variable has in range 0.0625 ~ 1.0
		val deltaX = gBB.maxX - gBB.minX
		val deltaY = gBB.maxY - gBB.minY
		val deltaZ = gBB.maxZ - gBB.minZ

		// Search Ranges
		val xzRange = xzRangeValue.get()
		val yRange = yRangeValue.get()

		val xSteps = calcStepSize(xzRange) * deltaX
		val ySteps = calcStepSize(yRange) * deltaY
		val zSteps = calcStepSize(xzRange) * deltaZ

		val sMinX = (0.5 - xzRange * 0.05) * deltaX + gBB.minX
		val sMaxX = (0.5 + xzRange * 0.5) * deltaX + gBB.minX
		val sMinY = (0.5 - yRange * 0.5) * deltaY + gBB.minY
		val sMaxY = (0.5 + yRange * 0.5) * deltaY + gBB.minY
		val sMinZ = (0.5 - xzRange * 0.5) * deltaZ + gBB.minZ
		val sMaxZ = (0.5 + xzRange * 0.5) * deltaZ + gBB.minZ

		val searchBounds = SearchBounds(sMinX, sMaxX, xSteps, sMinY, sMaxY, ySteps, sMinZ, sMaxZ, zSteps)

		lastSearchBound = searchBounds

		val state: String

		var clutching = false
		if (fallStartY - thePlayer.posY > 2) // Clutch while falling
		{
			val mx = thePlayer.motionX.coerceAtLeast(-2.5).coerceAtMost(2.5)
			val my = thePlayer.motionY.coerceAtLeast(-2.5).coerceAtMost(0.0)
			val mz = thePlayer.motionZ.coerceAtLeast(-2.5).coerceAtMost(2.5)

			searchPosition = WBlockPos(thePlayer.posX + mx, thePlayer.entityBoundingBox.minY + my, thePlayer.posZ + mz).down() // Predict position and clutch
			state = "Clutch based at motion $mx $my $mz"
			clutching = true
		}
		else if (!sameY && abCollisionBB.maxY - abCollisionBB.minY < 1.0 && groundBlockBB.maxY < 1.0 && abCollisionBB.maxY - abCollisionBB.minY < groundBlockBB.maxY - groundBlockBB.minY)
		{
			searchPosition = pos

			// Failsafe for slab: Limits maxY to 0.5 to place slab safely.
			if (searchBounds.maxY >= 0.5)
			{
				searchBounds.minY = 0.125 - yRange * 0.25
				searchBounds.maxY = 0.125 + yRange * 0.25
			}

			state = "Non-Fullblock-SlabCorrection"
		}
		else if (!sameY && abCollisionBB.maxY - abCollisionBB.minY < 1.0 && groundBlockBB.maxY < 1.0 && abCollisionBB.maxY - abCollisionBB.minY == groundBlockBB.maxY - groundBlockBB.minY)
		{
			searchPosition = pos
			state = "Non-Fullblock"
		}
		else if (shouldGoDown)
		{
			searchPosition = pos.add(0.0, -0.6, 0.0).down() // Default full-block only scaffold
			state = "Down"
		}
		else
		{
			searchPosition = pos.down() // Default full-block only scaffold
			state = "Default"
		}

		if (searchDebug.get())
		{
			ClientUtils.displayChatMessage(thePlayer, "$state - $searchBounds")
			ClientUtils.displayChatMessage(thePlayer, "AutoBlock: $abCollisionBB, Ground: $lastGroundBlockBB")
		}

		lastSearchPosition = searchPosition

		val checkVisible = checkVisibleValue.get() && !shouldGoDown

		if (!expand && (!isReplaceable(searchPosition) || search(theWorld, thePlayer, searchPosition, checkVisible, searchBounds))) return

		val ySearch = ySearchValue.get() || clutching
		if (expand)
		{
			val horizontalFacing = functions.getHorizontalFacing(MovementUtils.getDirectionDegrees(thePlayer))
			repeat(expandLengthValue.get()) { i ->
				if (search(theWorld, thePlayer, searchPosition.add(when (horizontalFacing)
					{
						provider.getEnumFacing(EnumFacingType.WEST)
						-> -i
						provider.getEnumFacing(EnumFacingType.EAST) -> i
						else -> 0
					}, 0, when (horizontalFacing)
					{
						provider.getEnumFacing(EnumFacingType.NORTH) -> -i
						provider.getEnumFacing(EnumFacingType.SOUTH)
						-> i
						else -> 0
					}), false, searchBounds)) return@findBlock
			}
		}
		else if (searchValue.get()) (-1..1).forEach { x ->
			(if (ySearch) -1..1 else 0..0).forEach { y ->
				if ((-1..1).any { z -> search(theWorld, thePlayer, searchPosition.add(x, y, z), !shouldGoDown, searchBounds) }) return@findBlock
			}
		}
	}

	private fun place(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
		val waitForKillauraEnd = killauraBypassValue.get().equals("WaitForKillauraEnd", true) && killAura.hasTarget        // targetPlace, Blacklist, killauraWait check
		if (targetPlace == null || InventoryUtils.AUTOBLOCK_BLACKLIST.contains(BlockUtils.getBlock(theWorld, (targetPlace ?: return).blockPos)) || waitForKillauraEnd)
		{
			if (placeableDelay.get()) delayTimer.reset()
			return
		}

		val targetPlace = targetPlace ?: return

		// Delay & SameY check
		if (!delayTimer.hasTimePassed(delay) || sameYValue.get() && launchY - 1 != targetPlace.vec3.yCoord.toInt()) return

		if (killauraBypassValue.get().equals("SuspendKillaura", true)) killAura.suspend(suspendKillauraDuration.get().toLong())

		// Check if the player is holding block
		var itemStack: IItemStack? = thePlayer.heldItem
		var switched = false

		val netHandler = mc.netHandler

		val provider = classProvider

		if (itemStack == null || !provider.isItemBlock(itemStack.item) || provider.isBlockBush(itemStack.item!!.asItemBlock().block) || thePlayer.heldItem!!.stackSize <= 0)
		{
			if (autoBlockValue.get().equals("Off", true)) return

			// Auto-Block
			val blockSlot = InventoryUtils.findAutoBlockBlock(theWorld, thePlayer.inventoryContainer, autoBlockFullCubeOnlyValue.get(), lastSearchPosition?.let { BlockUtils.getBlock(theWorld, it) }?.getBlockBoundsMaxY() ?: 0.0) // Default boundingBoxYLimit it 0.0

			// If there is no autoblock-able blocks in your inventory, we can't continue.
			if (blockSlot == -1) return

			switched = slot + 36 != blockSlot
			when (autoBlockValue.get())
			{
				"Pick" ->
				{
					thePlayer.inventory.currentItem = blockSlot - 36
					mc.playerController.updateController()
				}

				"Spoof" -> if (blockSlot - 36 != slot) netHandler.addToSendQueue(provider.createCPacketHeldItemChange(blockSlot - 36))

				"Switch" -> if (blockSlot - 36 != slot) netHandler.addToSendQueue(provider.createCPacketHeldItemChange(blockSlot - 36))
			}
			itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
		}

		// Switch Delay reset
		if (switched)
		{
			switchTimer.reset()
			switchDelay = TimeUtils.randomDelay(minSwitchDelayValue.get(), maxSwitchDelayValue.get())
			if (switchDelay > 0) return
		}

		// Switch Delay wait
		if (!switchTimer.hasTimePassed(switchDelay)) return

		// Place block
		if (mc.playerController.onPlayerRightClick(thePlayer, theWorld, itemStack, targetPlace.blockPos, targetPlace.enumFacing, targetPlace.vec3))
		{

			// Reset delay
			delayTimer.reset()
			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

			// Apply SpeedModifier
			if (thePlayer.onGround)
			{
				val modifier: Float = speedModifierValue.get()
				thePlayer.motionX = thePlayer.motionX * modifier
				thePlayer.motionZ = thePlayer.motionZ * modifier
			}

			if (swingValue.get()) thePlayer.swingItem()
			else netHandler.addToSendQueue(provider.createCPacketAnimation())
		}

		// Switch back to original slot after place on AutoBlock-Switch mode
		if (autoBlockValue.get().equals("Switch", true) && slot != thePlayer.inventory.currentItem) netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

		this.targetPlace = null
	}

	// DISABLING MODULE
	override fun onDisable()
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings
		val netHandler = mc.netHandler

		// Reset Seaking (Eagle)
		if (!gameSettings.isKeyDown(gameSettings.keyBindSneak))
		{
			gameSettings.keyBindSneak.pressed = false
			if (eagleSneaking) netHandler.addToSendQueue(classProvider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SNEAKING))
		}

		if (!gameSettings.isKeyDown(gameSettings.keyBindRight)) gameSettings.keyBindRight.pressed = false
		if (!gameSettings.isKeyDown(gameSettings.keyBindLeft)) gameSettings.keyBindLeft.pressed = false

		// Reset rotations
		lockRotation = null
		limitedRotation = null

		facesBlock = false
		mc.timer.timerSpeed = 1f
		shouldGoDown = false

		if (slot != thePlayer.inventory.currentItem) netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		if (!safeWalkValue.get() || shouldGoDown) return
		if (airSafeValue.get() || (mc.thePlayer ?: return).onGround) event.isSafeWalk = true
	}

	/**
	 * Scaffold visuals
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
			val scaledResolution = classProvider.createScaledResolution(mc)

			val middleScreenX = scaledResolution.scaledWidth shr 1
			val middleScreenY = scaledResolution.scaledHeight shr 1

			RenderUtils.drawBorderedRect(middleScreenX - 2.0f, middleScreenY + 5.0f, middleScreenX + Fonts.font40.getStringWidth(info) + 2.0f, middleScreenY + 16.0f, 3f, Color.BLACK.rgb, Color.BLACK.rgb)

			classProvider.glStateManager.resetColor()

			Fonts.font40.drawString(info, (scaledResolution.scaledWidth shr 1).toFloat(), middleScreenY + 7.0f, 0xffffff)
			GL11.glPopMatrix()
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		if (!markValue.get()) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		run searchLoop@{
			repeat(if (modeValue.get().equals("Expand", true)) expandLengthValue.get() + 1 else 2) {
				val horizontalFacing = functions.getHorizontalFacing(MovementUtils.getDirectionDegrees(thePlayer))
				val blockPos = WBlockPos(thePlayer.posX + when (horizontalFacing)
				{
					classProvider.getEnumFacing(EnumFacingType.WEST) -> -it.toDouble()
					classProvider.getEnumFacing(EnumFacingType.EAST)
					-> it.toDouble()
					else -> 0.0
				}, if (sameYValue.get() && launchY <= thePlayer.posY) launchY - 1.0 else thePlayer.posY - (if (thePlayer.posY > floor(thePlayer.posY).toInt()) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0, thePlayer.posZ + when (horizontalFacing)
				{
					classProvider.getEnumFacing(EnumFacingType.NORTH) -> -it.toDouble()
					classProvider.getEnumFacing(EnumFacingType.SOUTH)
					-> it.toDouble()
					else -> 0.0
				})

				val placeInfo: PlaceInfo? = PlaceInfo.get(theWorld, blockPos)
				if (isReplaceable(blockPos) && placeInfo != null)
				{
					RenderUtils.drawBlockBox(theWorld, thePlayer, blockPos, Color(68, 117, 255, 100), false)
					return@searchLoop
				}
			}
		}
	}

	/**
	 * Search for placeable block
	 *
	 * @param blockPosition pos
	 * @param checkVisible        visible
	 * @return
	 */

	private fun search(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, blockPosition: WBlockPos, checkVisible: Boolean, data: SearchBounds): Boolean
	{
		if (!isReplaceable(blockPosition)) return false

		// Static Modes
		val staticMode = rotationModeValue.get().equals("Static", ignoreCase = true)
		val staticYaw = staticMode || rotationModeValue.get().equals("StaticYaw", ignoreCase = true)
		val staticPitch = staticMode || rotationModeValue.get().equals("StaticPitch", ignoreCase = true)
		val staticPitchOffset = staticPitchValue.get()
		val staticYawOffset = staticYawValue.get()

		var xSearchFace = 0.0
		var ySearchFace = 0.0
		var zSearchFace = 0.0

		val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
		var placeRotation: PlaceRotation? = null

		val provider = classProvider

		val searchMinX = data.minX
		val searchMaxX = data.maxX
		val searchMinY = data.minY
		val searchMaxY = data.maxY
		val searchMinZ = data.minZ
		val searchMaxZ = data.maxZ

		EnumFacingType.values().map(provider::getEnumFacing).forEach { side ->
			val neighbor = blockPosition.offset(side)

			if (!canBeClicked(theWorld, neighbor)) return@forEach

			val dirVec = WVec3(side.directionVec)
			val dirX = dirVec.xCoord
			val dirY = dirVec.yCoord
			val dirZ = dirVec.zCoord

			var xSearch = searchMinX
			while (xSearch <= searchMaxX)
			{
				var ySearch = searchMinY
				while (ySearch <= searchMaxY)
				{
					var zSearch = searchMinZ
					while (zSearch <= searchMaxZ)
					{
						val posVec = WVec3(blockPosition).addVector(xSearch, ySearch, zSearch)
						val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
						val hitVec = posVec.add(WVec3(dirX * 0.5, dirY * 0.5, dirZ * 0.5))
						if (checkVisible && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
						{
							zSearch += data.zSteps
							continue
						}

						// Face block
						repeat(if (staticYaw) 2 else 1) { i ->
							val diffX: Double = if (staticYaw && i == 0) 0.0 else hitVec.xCoord - eyesPos.xCoord
							val diffY = hitVec.yCoord - eyesPos.yCoord
							val diffZ: Double = if (staticYaw && i == 1) 0.0 else hitVec.zCoord - eyesPos.zCoord
							val diffXZ = hypot(diffX, diffZ)

							if (!side.isUp() && minDiffValue.get() > 0)
							{
								val diff: Double = abs(if (side.isNorth() || side.isSouth()) diffZ else diffX)

								if (diff < minDiffValue.get() || diff > 0.3f) return@repeat
							}

							val pitch = if (staticPitch) staticPitchOffset else wrapAngleTo180_float((-WMathHelper.toDegrees(atan2(diffY, diffXZ).toFloat())))
							val rotation = Rotation(wrapAngleTo180_float(WMathHelper.toDegrees(atan2(diffZ, diffX).toFloat()) - 90f + if (staticYaw) staticYawOffset else 0f), pitch)
							val rotationVector = RotationUtils.getVectorForRotation(rotation)
							val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
							val rayTrace = theWorld.rayTraceBlocks(eyesPos, vector, false, false, true)

							if (rayTrace!!.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos!! != neighbor) return@repeat
							if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation!!.rotation)) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)

							xSearchFace = xSearch
							ySearchFace = ySearch
							zSearchFace = zSearch
						}

						zSearch += data.zSteps
					}
					ySearch += data.ySteps
				}
				xSearch += data.xSteps
			}
		}

		if (placeRotation == null) return false

		// Rotate
		if (!rotationModeValue.get().equals("Off", ignoreCase = true))
		{
			val tower = LiquidBounce.moduleManager[Tower::class.java] as Tower
			val keepRotationTicks = if (keepRotationValue.get()) if (maxKeepRotationTicksValue.get() == minKeepRotationTicksValue.get()) maxKeepRotationTicksValue.get()
			else minKeepRotationTicksValue.get() + Random.nextInt(maxKeepRotationTicksValue.get() - minKeepRotationTicksValue.get())
			else 0

			if (minTurnSpeedValue.get() < 180)
			{
				limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, placeRotation!!.rotation, (Random.nextFloat() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()), 0.0F) // TODO: Apply some settings here too
				setRotation(thePlayer, limitedRotation!!, keepRotationTicks)
				tower.lockRotation = null // Prevents conflict
				lockRotation = limitedRotation
				facesBlock = false

				run searchLoop@{
					EnumFacingType.values().map(provider::getEnumFacing).forEach { side ->
						val neighbor = blockPosition.offset(side)

						if (!canBeClicked(theWorld, neighbor)) return@forEach

						val dirVec = WVec3(side.directionVec)
						val posVec = WVec3(blockPosition).addVector(xSearchFace, ySearchFace, zSearchFace)
						val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
						val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))

						if (checkVisible && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld!!.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)) return@forEach

						val rotationVector = RotationUtils.getVectorForRotation(limitedRotation!!)
						val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
						val rayTrace = mc.theWorld!!.rayTraceBlocks(eyesPos, vector, false, false, true)

						if (!(rayTrace!!.typeOfHit == IMovingObjectPosition.WMovingObjectType.BLOCK && rayTrace.blockPos!! == neighbor)) return@forEach

						facesBlock = true

						return@searchLoop
					}
				}
			}
			else
			{
				setRotation(thePlayer, placeRotation!!.rotation, keepRotationTicks)
				tower.lockRotation = null // Prevents conflict
				lockRotation = placeRotation!!.rotation
				facesBlock = true
			}
		}

		targetPlace = placeRotation!!.placeInfo
		return true
	}

	private fun calcStepSize(range: Float): Double
	{
		var accuracy: Double = searchAccuracyValue.get().toDouble()
		accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
		return if (range / accuracy < 0.01) 0.01 else (range / accuracy)
	}

	// RETURN HOTBAR AMOUNT
	private fun getBlocksAmount(thePlayer: IEntityPlayerSP): Int
	{
		var amount = 0

		val inventoryContainer = thePlayer.inventoryContainer

		(36..44).map { inventoryContainer.getSlot(it).stack }.filter {
			val heldItem: IItemStack? = thePlayer.heldItem
			classProvider.isItemBlock(it?.item) && (heldItem != null && heldItem == it || InventoryUtils.canAutoBlock(((it?.item!!).asItemBlock()).block))
		}.forEach {
			amount += (it ?: return@forEach).stackSize
		}

		return amount
	}

	override val tag: String
		get() = modeValue.get()

	class SearchBounds(x: Double, x2: Double, xsteps: Double, y: Double, y2: Double, ysteps: Double, z: Double, z2: Double, zsteps: Double)
	{
		val minX: Double = x.coerceAtMost(x2)
		val maxX: Double = x.coerceAtLeast(x2)
		var minY: Double = y.coerceAtMost(y2)
		var maxY: Double = y.coerceAtLeast(y2)
		val minZ: Double = z.coerceAtMost(z2)
		val maxZ: Double = z.coerceAtLeast(z2)

		val xSteps = xsteps
		val ySteps = ysteps
		val zSteps = zsteps

		override fun toString(): String = String.format("SearchBounds[X: %.2f~%.2f (%.3f), Y: %.2f~%.2f (%.3f), Z: %.2f~%.2f (%.3f)]", minX, maxX, xSteps, minY, maxY, ySteps, minZ, maxZ, zSteps)
	}
}
