/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */


package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.wrapAngleTo180_float
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
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.*
import kotlin.random.Random

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD, defaultKeyBinds = [Keyboard.KEY_I])
class Scaffold : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand"), "Normal")

	private val delayGroup = ValueGroup("Delay")
	private val delayValue = IntegerRangeValue("Delay", 0, 0, 0, 1000, "MaxDelay" to "MinDelay")
	private val delaySwitchValue = IntegerRangeValue("SwitchSlotDelay", 0, 0, 0, 1000, "MaxSwitchSlotDelay" to "MinSwitchSlotDelay")
	private val delayPlaceableDelayValue = BoolValue("PlaceableDelay", true)

	private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

	private val autoBlockGroup = ValueGroup("AutoBlock")
	private val autoBlockModeValue = ListValue("Mode", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof", "AutoBlock")
	private val autoBlockSwitchKeepTimeValue = object : IntegerValue("SwitchKeepTime", -1, -1, 10, "AutoBlockSwitchKeepTime")
	{
		override fun showCondition() = !autoBlockModeValue.get().equals("None", ignoreCase = true)
	}
	private val autoBlockFullCubeOnlyValue = object : BoolValue("FullCubeOnly", false, "AutoBlockFullCubeOnly")
	{
		override fun showCondition() = !autoBlockModeValue.get().equals("None", ignoreCase = true)
	}

	private val expandLengthValue = object : IntegerValue("ExpandLength", 1, 1, 6)
	{
		override fun showCondition() = modeValue.get().equals("Expand", ignoreCase = true)
	}

	private val disableWhileTowering: BoolValue = BoolValue("DisableWhileTowering", true)

	private val rotationGroup = ValueGroup("Rotation")

	private val rotationModeValue = ListValue("Mode", arrayOf("Off", "Normal", "Static", "StaticPitch", "StaticYaw"), "Normal", "RotationMode")

	private val rotationSearchGroup = ValueGroup("Search")
	private val rotationSearchSearchValue = BoolValue("Search", true, "Search")
	private val rotationSearchSearchRangeValue = IntegerValue("SearchRange", 1, 1, 3)
	private val rotationSearchYSearchValue = BoolValue("YSearch", false, "YSearch")
	private val rotationSearchYSearchRangeValue = IntegerValue("YSearchRange", 1, 1, 3)
	private val rotationSearchXZRangeValue = FloatValue("XZRange", 0.8f, 0f, 1f, "XZRange")
	private val rotationSearchYRangeValue = FloatValue("YRange", 0.8f, 0f, 1f, "YRange")
	private val rotationSearchMinDiffValue = FloatValue("MinDiff", 0f, 0f, 0.2f, "MinDiff")
	private val rotationSearchStepsValue = IntegerValue("Steps", 8, 1, 16, "SearchAccuracy")
	private val rotationSearchCheckVisibleValue = BoolValue("CheckVisible", true, "CheckVisible")

	private val rotationSearchStaticYawValue = object : FloatValue("StaticYawOffSet", 0f, 0f, 90f, "StaticYawOffSet")
	{
		override fun showCondition(): Boolean
		{
			val rotationMode = rotationModeValue.get()
			return rotationMode.equals("Static", ignoreCase = true) || rotationMode.equals("StaticYaw", ignoreCase = true)
		}
	}
	private val rotationSearchStaticPitchValue = object : FloatValue("StaticPitchOffSet", 86f, 70f, 90f, "StaticPitchOffSet")
	{
		override fun showCondition(): Boolean
		{
			val rotationMode = rotationModeValue.get()
			return rotationMode.equals("Static", ignoreCase = true) || rotationMode.equals("StaticPitch", ignoreCase = true)
		}
	}

	private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 1f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
	private val rotationResetSpeedValue = FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")

	private val rotationSilentValue = BoolValue("SilentRotation", true, "SilentRotation")
	private val rotationStrafeValue = BoolValue("Strafe", false, "RotationStrafe")

	private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
	private val rotationKeepRotationEnabledValue = BoolValue("Enabled", true, "KeepRotation")
	private val rotationKeepRotationLockValue = BoolValue("Lock", false, "LockRotation")
	private val rotationKeepRotationTicksValue = object : IntegerRangeValue("Ticks", 20, 30, 0, 60, "MinKeepRotationTicks" to "MaxKeepRotationTicks")
	{
		override fun showCondition() = !rotationKeepRotationLockValue.get()
	}

	private val swingValue = BoolValue("Swing", true, "Swing")

	private val movementGroup = ValueGroup("Movement")

	val movementSprintValue: BoolValue = object : BoolValue("Sprint", false, "Sprint")
	{
		override fun onChanged(oldValue: Boolean, newValue: Boolean)
		{
			if (newValue) movementSlowEnabledValue.set(false)
		}
	}

	private val movementEagleGroup = ValueGroup("Eagle")
	private val movementEagleModeValue = ListValue("Mode", arrayOf("Normal", "EdgeDistance", "Silent", "SilentEdgeDistance", "Off"), "Normal", "Eagle")
	private val movementEagleBlocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10)
	private val movementEagleEdgeDistanceValue = object : FloatValue("EagleEdgeDistance", 0.2f, 0f, 0.5f)
	{
		override fun showCondition() = movementEagleModeValue.get().endsWith("EdgeDistance", ignoreCase = true)
	}

	private val movementZitterGroup = ValueGroup("Zitter")
	private val movementZitterEnabledValue = BoolValue("Enabled", false, "Zitter")
	private val movementZitterModeValue = ListValue("Mode", arrayOf("Teleport", "Smooth"), "Teleport", "ZitterMode")
	private val movementZitterIntensityValue = FloatValue("Intensity", 0.05f, 0f, 0.2f, "ZitterStrength")
	private val movementZitterSpeedValue = FloatValue("Speed", 0.13f, 0.05f, 0.4f, "ZitterSpeed")

	private val movementSlowGroup = ValueGroup("Slow")
	private val movementSlowEnabledValue = object : BoolValue("Enabled", false, "Slow")
	{
		override fun onChanged(oldValue: Boolean, newValue: Boolean)
		{
			if (newValue) movementSprintValue.set(false)
		}
	}
	private val movementSlowSpeedValue = FloatValue("Speed", 0.6f, 0.2f, 0.8f, "SlowSpeed")

	private val movementSafeWalkValue = BoolValue("SafeWalk", true, "SafeWalk")
	private val movementAirSafeValue = BoolValue("AirSafe", false, "AirSafe")

	private val timerValue = FloatValue("Timer", 1f, 0.1f, 10f)
	private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)

	private val sameYValue = BoolValue("SameY", false)
	private val downValue = BoolValue("Downward", true, "Down")

	private val killAuraBypassGroup = ValueGroup("KillAuraBypass")
	val killauraBypassModeValue = ListValue("Mode", arrayOf("None", "SuspendKillAura", "WaitForKillAuraEnd"), "SuspendKillAura", "KillAuraBypassMode")
	private val killAuraBypassKillAuraSuspendDurationValue = object : IntegerValue("Duration", 300, 100, 1000, "SuspendKillAuraDuration")
	{
		override fun showCondition() = killauraBypassModeValue.get().equals("SuspendKillAura", ignoreCase = true)
	}

	private val stopConsumingBeforePlaceValue = BoolValue("StopConsumingBeforePlace", true)

	private val visualGroup = ValueGroup("Visual")

	private val visualCounterGroup = ValueGroup("Counter")
	val visualCounterEnabledValue = BoolValue("Enabled", true, "Counter")
	private val visualCounterFontValue = FontValue("Font", Fonts.font40)

	private val visualMarkValue = BoolValue("Mark", false, "Mark")
	private val visualSearchDebugValue = BoolValue("SearchDebug", false, "SearchDebugChat")

	// MODULE

	// Target block
	private var targetPlace: PlaceInfo? = null
	private var lastSearchBound: SearchBounds? = null

	// Rotation lock
	var lockRotation: Rotation? = null
	private var limitedRotation: Rotation? = null

	// Launch position
	private var launchY = -999
	private var facesBlock = false

	// Zitter Direction
	private var zitterDirection = false

	// Delay
	private val delayTimer = MSTimer()
	private val zitterTimer = MSTimer()
	private val switchTimer = MSTimer()
	private val flagTimer = MSTimer()
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

	private var searchDebug: String? = null
	private var placeDebug: String? = null

	private val searchRanges = hashMapOf<Pair<Int, Int>, Pair<List<Pair<Int, Int>>, List<Int>>>()

	init
	{
		delayGroup.addAll(delayValue, delaySwitchValue, delayPlaceableDelayValue)
		autoBlockGroup.addAll(autoBlockModeValue, autoBlockSwitchKeepTimeValue, autoBlockFullCubeOnlyValue)
		rotationSearchGroup.addAll(rotationSearchSearchValue, rotationSearchSearchRangeValue, rotationSearchYSearchValue, rotationSearchYSearchRangeValue, rotationSearchXZRangeValue, rotationSearchYRangeValue, rotationSearchMinDiffValue, rotationSearchStepsValue, rotationSearchCheckVisibleValue)
		rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationLockValue, rotationKeepRotationTicksValue)
		rotationGroup.addAll(rotationModeValue, rotationSearchGroup, rotationSearchStaticYawValue, rotationSearchStaticPitchValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationSilentValue, rotationStrafeValue, rotationKeepRotationGroup)
		movementEagleGroup.addAll(movementEagleModeValue, movementEagleBlocksToEagleValue, movementEagleEdgeDistanceValue)
		movementZitterGroup.addAll(movementZitterEnabledValue, movementZitterModeValue, movementZitterIntensityValue, movementZitterSpeedValue)
		movementSlowGroup.addAll(movementSlowEnabledValue, movementSlowSpeedValue)
		movementGroup.addAll(movementSprintValue, movementEagleGroup, movementZitterGroup, movementSlowGroup, movementSafeWalkValue, movementAirSafeValue)
		killAuraBypassGroup.addAll(killauraBypassModeValue, killAuraBypassKillAuraSuspendDurationValue)
		visualCounterGroup.addAll(visualCounterEnabledValue, visualCounterFontValue)
		visualGroup.addAll(visualCounterGroup, visualMarkValue, visualSearchDebugValue)
	}

	// ENABLING MODULE
	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		launchY = thePlayer.posY.toInt()

		searchDebug = null
		placeDebug = null

		val rotationMode = rotationModeValue.get()
		if (modeValue.get().equals("Expand", ignoreCase = true) && (rotationMode.equals("Static", ignoreCase = true) || rotationMode.equals("StaticPitch", ignoreCase = true))) ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A7aScaffold\u00A78] \u00A7aUsing Expand scaffold with Static/StaticPitch rotation mode can decrease your expand length!")
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
		if (movementSlowEnabledValue.get())
		{
			thePlayer.motionX = thePlayer.motionX * movementSlowSpeedValue.get()
			thePlayer.motionZ = thePlayer.motionZ * movementSlowSpeedValue.get()
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
				thePlayer.strafe(0.2F)
				thePlayer.motionY = 0.0
			}

			// Smooth Zitter
			if (movementZitterEnabledValue.get() && movementZitterModeValue.get().equals("Smooth", true))
			{
				val keyBindRight = gameSettings.keyBindRight
				val keyBindLeft = gameSettings.keyBindLeft

				if (!gameSettings.isKeyDown(keyBindRight)) keyBindRight.pressed = false
				if (!gameSettings.isKeyDown(keyBindLeft)) keyBindLeft.pressed = false

				if (zitterTimer.hasTimePassed(100))
				{
					zitterDirection = !zitterDirection
					zitterTimer.reset()
				}

				if (zitterDirection)
				{
					keyBindRight.pressed = true
					keyBindLeft.pressed = false
				}
				else
				{
					keyBindRight.pressed = false
					keyBindLeft.pressed = true
				}
			}

			// Eagle
			if (!movementEagleModeValue.get().equals("Off", true) && !shouldGoDown)
			{
				var dif = 0.5

				// Caldulate edge distance
				if (movementEagleModeValue.get().endsWith("EdgeDistance", true) && !shouldGoDown)
				{
					repeat(4) {
						when (it)
						{
							0 ->
							{
								val blockPos = WBlockPos(thePlayer.posX - 1.0, thePlayer.posY - (if (thePlayer.posY == thePlayer.posY.toInt() + 0.5) 0.0 else 1.0), thePlayer.posZ)
								val placeInfo: PlaceInfo? = PlaceInfo[theWorld, blockPos]
								if (theWorld.isReplaceable(blockPos) && placeInfo != null)
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
								val placeInfo: PlaceInfo? = PlaceInfo[theWorld, blockPos]

								if (theWorld.isReplaceable(blockPos) && placeInfo != null)
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
								val placeInfo: PlaceInfo? = PlaceInfo[theWorld, blockPos]

								if (theWorld.isReplaceable(blockPos) && placeInfo != null)
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
								val placeInfo: PlaceInfo? = PlaceInfo[theWorld, blockPos]

								if (theWorld.isReplaceable(blockPos) && placeInfo != null)
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

				if (placedBlocksWithoutEagle >= movementEagleBlocksToEagleValue.get())
				{
					val provider = classProvider

					val shouldEagle: Boolean = theWorld.getBlockState(WBlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)).block == (provider.getBlockEnum(BlockType.AIR)) || (dif < movementEagleEdgeDistanceValue.get() && movementEagleModeValue.get().endsWith("EdgeDistance", true))
					if (movementEagleModeValue.get().startsWith("Silent", true) && !shouldGoDown)
					{
						if (eagleSneaking != shouldEagle) mc.netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, if (shouldEagle) ICPacketEntityAction.WAction.START_SNEAKING else ICPacketEntityAction.WAction.STOP_SNEAKING))
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
			if (movementZitterEnabledValue.get() && movementZitterModeValue.get().equals("Teleport", true))
			{
				thePlayer.strafe(movementZitterSpeedValue.get())

				val func = functions

				val yaw = WMathHelper.toRadians(thePlayer.rotationYaw + if (zitterDirection) 90f else -90f)
				thePlayer.motionX = thePlayer.motionX - func.sin(yaw) * movementZitterIntensityValue.get()
				thePlayer.motionZ = thePlayer.motionZ + func.cos(yaw) * movementZitterIntensityValue.get()
				zitterDirection = !zitterDirection
			}
		}
	}

	@EventTarget
	fun onStrafe(event: StrafeEvent)
	{
		if (!rotationStrafeValue.get()) return
		RotationUtils.serverRotation.applyStrafeToPlayer(event)
		event.cancelEvent()
	}

	@EventTarget(ignoreCondition = true)
	fun onMotion(event: MotionEvent)
	{
		val eventState: EventState = event.eventState

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (!thePlayer.onGround && thePlayer.motionY < 0)
		{
			if (fallStartY < thePlayer.posY) fallStartY = thePlayer.posY
		}
		else fallStartY = 0.0

		if (!state) return

		val tower = LiquidBounce.moduleManager[Tower::class.java] as Tower
		if (disableWhileTowering.get() && tower.active)
		{
			launchY = thePlayer.posY.toInt() // Compatibility between SameY and Tower
			return
		}

		val currentLockRotation = lockRotation

		// Lock Rotation
		if (!rotationModeValue.get().equals("Off", true) && rotationKeepRotationEnabledValue.get() && rotationKeepRotationLockValue.get() && currentLockRotation != null) setRotation(thePlayer, currentLockRotation)

		// Place block
		if ((facesBlock || rotationModeValue.get().equals("Off", true)) && placeModeValue.get().equals(eventState.stateName, true)) place(theWorld, thePlayer)

		// Update and search for a new block
		if (eventState == EventState.PRE) update(theWorld, thePlayer)

		// Reset placeable delay
		if (targetPlace == null && delayPlaceableDelayValue.get()) delayTimer.reset()
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isSPacketPlayerPosLook(packet))
		{
			val tpPacket = packet.asSPacketPlayerPosLook()

			flagTimer.reset()
			launchY = tpPacket.y.toInt()
		}
	}

	fun update(theWorld: IWorld, thePlayer: IEntityPlayerSP)
	{
		val provider = classProvider

		val heldItem = thePlayer.heldItem
		val heldItemIsNotBlock: Boolean = heldItem == null || !provider.isItemBlock(heldItem.item)

		if (if (autoBlockModeValue.get().equals("Off", true)) heldItemIsNotBlock else thePlayer.inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get()) == -1 && heldItemIsNotBlock) return

		val groundSearchDepth = 0.2

		val pos = WBlockPos(thePlayer.posX, thePlayer.posY - groundSearchDepth, thePlayer.posZ)
		val bs: IIBlockState = theWorld.getBlockState(pos)
		if ( /* (this.lastGroundBlockState == null || !pos.equals(this.lastGroundBlockPos)) && */!theWorld.isReplaceable(bs))
		{
			lastGroundBlockState = bs
			lastGroundBlockPos = pos
			lastGroundBlockBB = theWorld.getBlockCollisionBox(bs)
		}

		findBlock(theWorld, thePlayer, modeValue.get().equals("expand", true))
	}

	private fun setRotation(thePlayer: IEntityPlayer, rotation: Rotation, keepRotation: Int)
	{
		if (rotationSilentValue.get())
		{
			RotationUtils.setTargetRotation(rotation, keepRotation)
			RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))
		}
		else
		{
			thePlayer.rotationYaw = rotation.yaw
			thePlayer.rotationPitch = rotation.pitch
		}
	}

	private fun setRotation(thePlayer: IEntityPlayer, rotation: Rotation)
	{
		setRotation(thePlayer, rotation, 0)
	}

	// Search for new target block
	private fun findBlock(theWorld: IWorld, thePlayer: IEntityPlayerSP, expand: Boolean)
	{
		val failedResponce = { reason: String ->
			searchDebug = listOf("result".equalTo("FAILED", "\u00A74"), "reason" equalTo reason.withParentheses("\u00A74")).serialize()
		}

		val groundBlockState = lastGroundBlockState ?: run {
			failedResponce("lastGroundBlockState = null")
			return@findBlock
		}

		val groundBlockBB = lastGroundBlockBB ?: run {
			failedResponce("lastGroundBlockBB = null")
			return@findBlock
		}

		val groundBlock: IBlock = groundBlockState.block

		// get the block that will be automatically placed
		var autoBlock: IItemStack? = thePlayer.heldItem

		val provider = classProvider

		if (autoBlock == null || !provider.isItemBlock(autoBlock.item) || autoBlock.stackSize <= 0 || autoBlock.item?.let { !it.asItemBlock().block.canAutoBlock } != false)
		{
			if (autoBlockModeValue.get().equals("Off", true))
			{
				failedResponce("(AutoBlock) Insufficient block")
				return
			}

			val inventoryContainer = thePlayer.inventoryContainer

			val autoBlockSlot = inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get())
			if (autoBlockSlot == -1)
			{
				failedResponce("(Hand) Insufficient block")
				return
			}

			autoBlock = inventoryContainer.getSlot(autoBlockSlot).stack
		}

		val autoBlockBlock = (autoBlock?.item ?: run {
			failedResponce("Block?.item = null")
			return@findBlock
		}).asItemBlock().block

		val func = functions

		val abCollisionBB = theWorld.getBlockCollisionBox(if (func.isBlockEqualTo(groundBlock, autoBlockBlock)) groundBlockState
		else autoBlockBlock.defaultState ?: run {
			failedResponce("BlockState unavailable")
			return@findBlock
		}) ?: run {
			failedResponce("BlockCollisionBox unavailable")
			return@findBlock
		}

		val groundMinX = groundBlockBB.minX
		val groundMinY = groundBlockBB.minY
		val groundMaxY = groundBlockBB.maxY
		val groundMinZ = groundBlockBB.minZ

		// These delta variable has in range 0.0625 ~ 1.0
		val deltaX = groundBlockBB.maxX - groundMinX
		val deltaY = groundMaxY - groundMinY
		val deltaZ = groundBlockBB.maxZ - groundMinZ

		// Search Ranges
		val xzRange = rotationSearchXZRangeValue.get()
		val yRange = rotationSearchYRangeValue.get()

		val xSteps = calcStepSize(xzRange) * deltaX
		val ySteps = calcStepSize(yRange) * deltaY
		val zSteps = calcStepSize(xzRange) * deltaZ

		val sMinX = (0.5 - xzRange * 0.05) * deltaX + groundMinX
		val sMaxX = (0.5 + xzRange * 0.5) * deltaX + groundMinX
		val sMinY = (0.5 - yRange * 0.5) * deltaY + groundMinY
		val sMaxY = (0.5 + yRange * 0.5) * deltaY + groundMinY
		val sMinZ = (0.5 - xzRange * 0.5) * deltaZ + groundMinZ
		val sMaxZ = (0.5 + xzRange * 0.5) * deltaZ + groundMinZ

		val searchBounds = SearchBounds(sMinX, sMaxX, xSteps, sMinY, sMaxY, ySteps, sMinZ, sMaxZ, zSteps)

		lastSearchBound = searchBounds

		val state: String
		var clutching = false
		val flagged = !flagTimer.hasTimePassed(500L)
		val searchPosition: WBlockPos

		if (flagged) launchY = thePlayer.posY.toInt()

		if (fallStartY - thePlayer.posY > 2)
		{
			searchPosition = WBlockPos(thePlayer).down(2)
			state = "\u00A7cCLUTCH"
			clutching = true
			launchY = thePlayer.posY.toInt()
		}
		else
		{
			var pos = WBlockPos(thePlayer)

			var sameY = false
			if (sameYValue.get() && launchY != -999)
			{
				pos = WBlockPos(thePlayer.posX, launchY - 1.0, thePlayer.posZ).up()
				sameY = true
			}

			if (!sameY && abCollisionBB.maxY - abCollisionBB.minY < 1.0 && groundMaxY < 1.0 && abCollisionBB.maxY - abCollisionBB.minY < groundMaxY - groundMinY)
			{
				searchPosition = pos

				// Failsafe for slab: Limits maxY to 0.5 to place slab safely.
				if (searchBounds.maxY >= 0.5)
				{
					searchBounds.minY = 0.125 - yRange * 0.25
					searchBounds.maxY = 0.125 + yRange * 0.25
				}

				state = "\u00A76NON-FULLBLOCK-SLAB-FAILSAFE"
			}
			else if (!sameY && abCollisionBB.maxY - abCollisionBB.minY < 1.0 && groundMaxY < 1.0 && abCollisionBB.maxY - abCollisionBB.minY == groundMaxY - groundMinY)
			{
				searchPosition = pos
				state = "\u00A7eNON-FULLBLOCK"
			}
			else if (shouldGoDown)
			{
				searchPosition = pos.add(0.0, -0.6, 0.0).down() // Default full-block only scaffold
				state = "\u00A7bDOWN"
			}
			else
			{
				searchPosition = pos.down() // Default full-block only scaffold
				state = "\u00A7aDEFAULT"
			}
		}

		if (visualSearchDebugValue.get()) searchDebug = listOf("result".equalTo("SUCCESS", "\u00A7a"), "state" equalTo "$state${if (flagged) " \u00A78+ \u00A7cFLAGGED\u00A7r" else ""}", "searchRange".equalTo(searchBounds, "\u00A7b"), "autoBlockCollisionBox".equalTo("$abCollisionBB", "\u00A73"), "groundBlockCollisionBB".equalTo("$lastGroundBlockBB", "\u00A72")).serialize()

		lastSearchPosition = searchPosition

		val facings = EnumFacingType.values().map(provider::getEnumFacing)
		if (!expand && (!theWorld.isReplaceable(searchPosition) || search(theWorld, thePlayer, searchPosition, rotationSearchCheckVisibleValue.get() && !shouldGoDown, searchBounds, facings))) return

		val ySearch = rotationSearchYSearchValue.get() || clutching || flagged
		if (expand)
		{
			val hFacing = func.getHorizontalFacing(thePlayer.moveDirectionDegrees)

			repeat(expandLengthValue.get()) { i ->
				if (search(theWorld, thePlayer, searchPosition.add(when (hFacing)
					{
						provider.getEnumFacing(EnumFacingType.WEST) -> -i
						provider.getEnumFacing(EnumFacingType.EAST) -> i
						else -> 0
					}, 0, when (hFacing)
					{
						provider.getEnumFacing(EnumFacingType.NORTH) -> -i
						provider.getEnumFacing(EnumFacingType.SOUTH) -> i
						else -> 0
					}), false, searchBounds, facings)) return@findBlock
			}
		}
		else if (rotationSearchSearchValue.get())
		{
			val urgent = clutching || flagged
			val rangeValue = if (urgent) max(2, rotationSearchSearchRangeValue.get()) else rotationSearchSearchRangeValue.get()
			val yrangeValue = if (ySearch) if (urgent) max(2, rotationSearchYSearchRangeValue.get()) else rotationSearchYSearchRangeValue.get() else 0

			// Cache the ranges
			val listPair = searchRanges.computeIfAbsent(rangeValue to yrangeValue) { (range, yrange) ->
				// Closest first
				val xzList = mutableListOf<Pair<Int, Int>>()
				var i = 0
				while (i <= range)
				{
					(-i..i).forEach { x -> (-i..i).forEach { z -> if (x to z !in xzList) xzList.add(x to z) } }
					i++
				}

				val yList = mutableListOf<Int>()
				var j = 0
				while (j <= yrange)
				{
					(-j..j).forEach { y -> if (y !in yList) yList.add(y) }
					j++
				}

				xzList to yList
			}

			val yList = if (urgent) -yrangeValue..yrangeValue else listPair.second

			listPair.first.forEach { (x, z) ->
				if (yList.any { y -> search(theWorld, thePlayer, searchPosition.add(x, y, z), !shouldGoDown, searchBounds, facings) }) return@findBlock
			}
		}
	}

	private fun place(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura

		val placeableDelay = { if (delayPlaceableDelayValue.get()) delayTimer.reset() }

		if (targetPlace == null)
		{
			placeableDelay()
			return
		}

		val failedResponce = { reason: String ->
			placeDebug = listOf("result".equalTo("FAILED", "\u00A74"), "reason" equalTo reason.withParentheses("\u00A74")).serialize()
		}

		if (BLACKLISTED_BLOCKS.contains(theWorld.getBlock((targetPlace ?: return).blockPos)))
		{
			placeableDelay()
			failedResponce("Blacklisted block in targetPlace.blockPos")
			return
		}
		if (killauraBypassModeValue.get().equals("WaitForKillAuraEnd", true) && killAura.hasTarget)
		{
			placeableDelay()
			failedResponce("Waiting for KillAura end")
			return
		}

		val targetPlace = targetPlace ?: return

		val autoBlockMode = autoBlockModeValue.get().toLowerCase()
		val switchKeepTime = autoBlockSwitchKeepTimeValue.get()

		// Delay & SameY check
		if (!delayTimer.hasTimePassed(delay) || sameYValue.get() && launchY - 1 != targetPlace.vec3.yCoord.toInt() && fallStartY - thePlayer.posY <= 2) return

		if (killauraBypassModeValue.get().equals("SuspendKillAura", true)) killAura.suspend(killAuraBypassKillAuraSuspendDurationValue.get().toLong())

		val controller = mc.playerController
		val netHandler = mc.netHandler
		val inventory = thePlayer.inventory

		val provider = classProvider

		(LiquidBounce.moduleManager[AutoUse::class.java] as AutoUse).endEating(thePlayer, classProvider, netHandler)

		// Check if the player is holding block
		val slot = InventoryUtils.targetSlot ?: inventory.currentItem
		var itemStack = inventory.mainInventory[slot]
		var switched = false

		if (itemStack == null || !provider.isItemBlock(itemStack.item) || !itemStack.item?.asItemBlock()?.block.canAutoBlock || itemStack.stackSize <= 0)
		{
			if (autoBlockMode == "off")
			{
				failedResponce("(Hand) Insufficient block")
				return
			}

			// Auto-Block
			val blockSlot = thePlayer.inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get(), lastSearchPosition?.let(theWorld::getBlockState)?.let { state -> theWorld.getBlockCollisionBox(state)?.maxY } ?: 0.0) // Default boundingBoxYLimit it 0.0

			// If there is no autoblock-able blocks in your inventory, we can't continue.
			if (blockSlot == -1)
			{
				failedResponce("(AutoBlock) Insufficient block")
				return
			}

			switched = slot + 36 != blockSlot

			when (autoBlockMode)
			{
				"pick" ->
				{
					inventory.currentItem = blockSlot - 36
					controller.updateController()
				}

				"spoof", "switch" -> if (blockSlot - 36 != slot)
				{
					if (!InventoryUtils.tryHoldSlot(thePlayer, blockSlot - 36, if (autoBlockMode == "spoof") -1 else switchKeepTime, false)) return
				}
				else InventoryUtils.resetSlot(thePlayer)
			}

			itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
		}

		// Switch Delay reset
		if (switched)
		{
			switchTimer.reset()
			switchDelay = delaySwitchValue.getRandomDelay()
			if (switchDelay > 0)
			{
				failedResponce("SwitchDelay reset")
				return
			}
		}

		// Switch Delay wait
		if (!switchTimer.hasTimePassed(switchDelay))
		{
			failedResponce("SwitchDelay")
			return
		}

		// CPSCounter support
		CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

		if (thePlayer.isUsingItem && stopConsumingBeforePlaceValue.get()) mc.playerController.onStoppedUsingItem(thePlayer)

		// Place block
		val pos = targetPlace.blockPos
		if (controller.onPlayerRightClick(thePlayer, theWorld, itemStack, pos, targetPlace.enumFacing, targetPlace.vec3))
		{
			placeDebug = listOf("position".equalTo(pos, "\u00A7a"), "side".equalTo(targetPlace.enumFacing, "\u00A7b"), "hitVec".equalTo(targetPlace.vec3.addVector(-pos.x.toDouble(), -pos.y.toDouble(), -pos.z.toDouble()), "\u00A7e")).serialize()

			// Reset delay
			delayTimer.reset()
			delay = delayValue.getRandomDelay()

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
		else placeDebug = "result".equalTo("FAILED_TO_PLACE", "\u00A74")

		// Switch back to original slot after place on AutoBlock-Switch mode
		if (autoBlockMode == "switch" && switchKeepTime < 0) InventoryUtils.resetSlot(thePlayer)

		this.targetPlace = null
	}

	// DISABLING MODULE
	override fun onDisable()
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings
		val netHandler = mc.netHandler

		val provider = classProvider

		// Reset Seaking (Eagle)
		if (!gameSettings.isKeyDown(gameSettings.keyBindSneak))
		{
			gameSettings.keyBindSneak.pressed = false
			if (eagleSneaking) netHandler.addToSendQueue(provider.createCPacketEntityAction(thePlayer, ICPacketEntityAction.WAction.STOP_SNEAKING))
		}

		if (!gameSettings.isKeyDown(gameSettings.keyBindRight)) gameSettings.keyBindRight.pressed = false
		if (!gameSettings.isKeyDown(gameSettings.keyBindLeft)) gameSettings.keyBindLeft.pressed = false

		// Reset rotations
		lockRotation = null
		limitedRotation = null

		facesBlock = false
		mc.timer.timerSpeed = 1f
		shouldGoDown = false

		InventoryUtils.resetSlot(thePlayer)
	}

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		if (!movementSafeWalkValue.get() || shouldGoDown) return
		if (movementAirSafeValue.get() || (mc.thePlayer ?: return).onGround) event.isSafeWalk = true
	}

	/**
	 * Scaffold visuals
	 */
	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		// TODO: Move these 'rendering' implementation to RenderUtils

		val counter = visualCounterEnabledValue.get()
		val debug = visualSearchDebugValue.get()
		if (!counter && !debug) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val blockOverlay = LiquidBounce.moduleManager[BlockOverlay::class.java] as BlockOverlay

		val scaledResolution = classProvider.createScaledResolution(mc)

		val middleScreenX = scaledResolution.scaledWidth shr 1
		val middleScreenY = scaledResolution.scaledHeight shr 1

		if (counter)
		{
			GL11.glPushMatrix()

			val blocksAmount = getBlocksAmount(thePlayer)
			val info = "Blocks: \u00A7${if (blocksAmount <= 16) "c" else if (blocksAmount <= 64) "e" else "7"}$blocksAmount${if (downValue.get() && blocksAmount == 1) " (You need at least 2 blocks to go down)" else ""}"

			val yoffset = if (blockOverlay.state && blockOverlay.infoEnabledValue.get() && blockOverlay.getCurrentBlock(theWorld) != null) 15f else 0f

			val font = visualCounterFontValue.get()
			RenderUtils.drawBorderedRect(middleScreenX - 2f, middleScreenY + yoffset + 5f, middleScreenX + font.getStringWidth(info) + 2f, middleScreenY + yoffset + font.fontHeight + 7f, 3f, -16777216, -16777216)

			classProvider.glStateManager.resetColor()

			font.drawString(info, middleScreenX.toFloat(), middleScreenY + yoffset + 7f, 0xffffff)
			GL11.glPopMatrix()
		}

		if (debug)
		{
			val font = Fonts.minecraftFont

			val info = searchDebug
			val infoWidth = info?.let { font.getStringWidth(it) shr 2 }

			val info2 = placeDebug
			val info2Width = info2?.let { font.getStringWidth(it) shr 2 }

			GL11.glPushMatrix()

			infoWidth?.let { RenderUtils.drawBorderedRect(middleScreenX - it - 2f, middleScreenY - 60f, middleScreenX + it + 2f, middleScreenY + font.fontHeight * 0.5f - 60f, 3f, -16777216, -16777216) }
			info2Width?.let { RenderUtils.drawBorderedRect(middleScreenX - it - 2f, middleScreenY - 50f, middleScreenX + it + 2f, middleScreenY + font.fontHeight * 0.5f - 50f, 3f, -16777216, -16777216) }

			classProvider.glStateManager.resetColor()

			GL11.glScalef(0.5f, 0.5f, 0.5f)
			info?.let { font.drawCenteredString(it, middleScreenX.toFloat() * 2f, (middleScreenY - 60f) * 2f, 0xffffff) }
			info2?.let { font.drawCenteredString(it, middleScreenX.toFloat() * 2f, (middleScreenY - 50f) * 2f, 0xffffff) }
			GL11.glScalef(2f, 2f, 2f)

			GL11.glPopMatrix()
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		if (!visualMarkValue.get()) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider

		run searchLoop@{
			repeat(if (modeValue.get().equals("Expand", true)) expandLengthValue.get() + 1 else 2) {
				val horizontalFacing = functions.getHorizontalFacing(thePlayer.moveDirectionDegrees)
				val blockPos = WBlockPos(thePlayer.posX + when (horizontalFacing)
				{
					provider.getEnumFacing(EnumFacingType.WEST) -> -it.toDouble()
					provider.getEnumFacing(EnumFacingType.EAST) -> it.toDouble()
					else -> 0.0
				}, if (sameYValue.get() && launchY <= thePlayer.posY && fallStartY - thePlayer.posY <= 2) launchY - 1.0 else thePlayer.posY - (if (thePlayer.posY > floor(thePlayer.posY).toInt()) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0, thePlayer.posZ + when (horizontalFacing)
				{
					provider.getEnumFacing(EnumFacingType.NORTH) -> -it.toDouble()
					provider.getEnumFacing(EnumFacingType.SOUTH) -> it.toDouble()
					else -> 0.0
				})

				val placeInfo: PlaceInfo? = PlaceInfo[theWorld, blockPos]
				if (theWorld.isReplaceable(blockPos) && placeInfo != null)
				{
					RenderUtils.drawBlockBox(theWorld, thePlayer, blockPos, 1682208255, 0, false, event.partialTicks)
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

	private fun search(theWorld: IWorld, thePlayer: IEntityPlayer, blockPosition: WBlockPos, checkVisible: Boolean, data: SearchBounds, facings: List<IEnumFacing>): Boolean
	{
		if (!theWorld.isReplaceable(blockPosition)) return false

		// Static Modes
		val staticMode = rotationModeValue.get().equals("Static", ignoreCase = true)
		val staticYaw = staticMode || rotationModeValue.get().equals("StaticYaw", ignoreCase = true)
		val staticPitch = staticMode || rotationModeValue.get().equals("StaticPitch", ignoreCase = true)
		val staticPitchOffset = rotationSearchStaticPitchValue.get()
		val staticYawOffset = rotationSearchStaticYawValue.get()

		var xSearchFace = 0.0
		var ySearchFace = 0.0
		var zSearchFace = 0.0

		val eyesPos = WVec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
		var placeRotation: PlaceRotation? = null

		val searchMinX = data.minX
		val searchMaxX = data.maxX
		val searchMinY = data.minY
		val searchMaxY = data.maxY
		val searchMinZ = data.minZ
		val searchMaxZ = data.maxZ

		facings.forEach { side ->
			val neighbor = blockPosition.offset(side)

			if (!theWorld.canBeClicked(neighbor)) return@forEach

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

						// Visible check
						if (checkVisible && (eyesPos.squareDistanceTo(hitVec) > 18.0 // Distance Check - distance > 3√2 blocks
								|| distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) // Against Distance Check - distance to Block > distance to Block SIDE
								|| theWorld.rayTraceBlocks(eyesPos, hitVec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) != null)) // Raytrace Check - rayTrace hit between eye position and block side
						{
							// Skip
							zSearch += data.zSteps
							continue
						}

						val minDiff = rotationSearchMinDiffValue.get()

						// Face block
						repeat(if (staticYaw) 2 else 1) { i ->
							val diffX: Double = if (staticYaw && i == 0) 0.0 else hitVec.xCoord - eyesPos.xCoord
							val diffY = hitVec.yCoord - eyesPos.yCoord
							val diffZ: Double = if (staticYaw && i == 1) 0.0 else hitVec.zCoord - eyesPos.zCoord
							val diffXZ = hypot(diffX, diffZ)

							if (!side.isUp() && minDiff > 0)
							{
								val diff: Double = abs(if (side.isNorth() || side.isSouth()) diffZ else diffX)
								if (diff < minDiff || diff > 0.3f) return@repeat
							}

							// Calculate the rotation from vector
							val rotation = Rotation(wrapAngleTo180_float(WMathHelper.toDegrees(atan2(diffZ, diffX).toFloat()) - 90f + if (staticYaw) staticYawOffset else 0f), if (staticPitch) staticPitchOffset else wrapAngleTo180_float((-WMathHelper.toDegrees(atan2(diffY, diffXZ).toFloat()))))
							val rotationVector = RotationUtils.getVectorForRotation(rotation)
							val blockReachPos = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)

							val rayTrace = theWorld.rayTraceBlocks(eyesPos, blockReachPos, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)
							if (rayTrace != null && (rayTrace.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos != neighbor)) return@repeat // Raytrace Check - rayTrace hit between eye position and block reach position

							if (placeRotation?.let { RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(it.rotation) } != false) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation) // If the current rotation is better than the previous one

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

		placeRotation ?: return false

		// Rotate
		if (!rotationModeValue.get().equals("Off", ignoreCase = true))
		{
			val keepRotationTicks = if (rotationKeepRotationEnabledValue.get())
			{
				val max = rotationKeepRotationTicksValue.getMax()
				val min = rotationKeepRotationTicksValue.getMin()
				if (max == min) max else min + Random.nextInt(max - min)
			}
			else 0

			val min = rotationTurnSpeedValue.getMin()
			if (rotationTurnSpeedValue.getMin() < 180)
			{
				// Limit rotation speed

				val max = rotationTurnSpeedValue.getMax()
				val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, placeRotation!!.rotation, (Random.nextFloat() * (max - min) + min), 0f).also { limitedRotation = it }
				setRotation(thePlayer, limitedRotation, keepRotationTicks)

				lockRotation = limitedRotation
				facesBlock = false

				// Check player is faced the target block
				run searchLoop@{
					facings.forEach sideLoop@{ side ->
						val neighbor = blockPosition.offset(side)

						if (!theWorld.canBeClicked(neighbor)) return@sideLoop

						val dirVec = WVec3(side.directionVec)
						val posVec = WVec3(blockPosition).addVector(xSearchFace, ySearchFace, zSearchFace)
						val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
						val hitVec = posVec.add(WVec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5))

						if (checkVisible && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || theWorld.rayTraceBlocks(eyesPos, hitVec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) != null)) return@sideLoop

						val rotationVector = RotationUtils.getVectorForRotation(limitedRotation)
						val vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4)
						val rayTrace = theWorld.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)

						if (rayTrace != null && (rayTrace.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos != neighbor)) return@sideLoop

						facesBlock = true

						return@searchLoop
					}
				}
			}
			else
			{
				setRotation(thePlayer, placeRotation!!.rotation, keepRotationTicks)

				lockRotation = placeRotation!!.rotation
				facesBlock = true
			}

			(LiquidBounce.moduleManager[Tower::class.java] as Tower).lockRotation = null // Prevents conflict
		}

		targetPlace = placeRotation?.placeInfo
		return true
	}

	private fun calcStepSize(range: Float): Double
	{
		var accuracy: Double = rotationSearchStepsValue.get().toDouble()
		accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
		return if (range / accuracy < 0.01) 0.01 else (range / accuracy)
	}

	// RETURN HOTBAR AMOUNT
	private fun getBlocksAmount(thePlayer: IEntityPlayer): Int
	{
		var amount = 0

		val provider = classProvider

		val inventory = thePlayer.inventory
		val heldItem = thePlayer.heldItem

		(0..8).map(inventory::getStackInSlot).filter { provider.isItemBlock(it?.item) }.mapNotNull { it to (it?.item ?: return@mapNotNull null).asItemBlock() }.filter { heldItem == it.first || it.second.block.canAutoBlock }.forEach { amount += (it.first ?: return@forEach).stackSize }

		return amount
	}

	override val tag: String?
		get()
		{
			val thePlayer = mc.thePlayer ?: return null
			return "${modeValue.get()}${if (fallStartY - thePlayer.posY > 2) " CLUTCH" else if (sameYValue.get()) " SameY=${launchY - 1.0}" else ""}"
		}

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
