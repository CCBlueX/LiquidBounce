/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.MinecraftVersion
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// TODO: Fix broken target searching algorithm
// TODO: Visually start-stop blocking like as Xave
@ModuleInfo(
	name = "KillAura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R
)
class KillAura : Module()
{
	/**
	 * OPTIONS
	 */

	// CPS - Attack speed
	private val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minCPS.get()
			if (i > newValue) set(i)

			attackDelay = TimeUtils.randomClickDelay(minCPS.get(), this.get())
		}
	}

	private val minCPS: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxCPS.get()
			if (i < newValue) set(i)

			attackDelay = TimeUtils.randomClickDelay(this.get(), maxCPS.get())
		}
	}

	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
	private val cooldownValue = FloatValue("Cooldown", 1f, 0f, 1f)

	// Range
	private val attackRangeValue = object : FloatValue("Range", 3.7f, 1f, 8f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i2 = aimRangeValue.get()
			if (i2 < newValue) this.set(i2)
			val i = swingRangeValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val aimRangeValue: FloatValue = FloatValue("AimRange", 6f, 1f, 12f)

	private val throughWallsRangeValue = FloatValue("ThroughWallsRange", 3f, 0f, 8f)
	private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f)

	// Bypass
	private val fakeSwingValue = BoolValue("FakeSwing", true)

	// Range
	private val swingRangeValue: FloatValue = object : FloatValue("SwingRange", 6f, 1f, 12f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = attackRangeValue.get()
			if (i > newValue) this.set(i)
		}
	}

	// Modes
	private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection", "LivingTime"), "Distance")
	private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")

	// Bypass
	private val swingValue = BoolValue("Swing", true)
	private val keepSprintValue = BoolValue("KeepSprint", true)

	// AutoBlock
	private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Fake", "Packet", "AfterTick"), "Packet")
	private val interactAutoBlockValue = BoolValue("InteractAutoBlock", false)
	private val autoBlockHitableCheckValue = BoolValue("AutoBlockHitableCheck", false)
	private val autoBlockHurtTimeCheckValue = BoolValue("AutoBlockHurtTimeCheck", true)
	private val blockRangeValue: FloatValue = FloatValue("BlockRange", 6f, 1f, 12f)
	private val blockRate = IntegerValue("BlockRate", 100, 1, 100)

	// Raycast
	private val raycastValue = BoolValue("RayCast", true)
	private val raycastIgnoredValue = BoolValue("RayCastIgnored", false)
	private val livingRaycastValue = BoolValue("LivingRayCast", true)

	// Bypass
	private val aacValue = BoolValue("AAC", false)
	private val rotationsValue = BoolValue("Rotations", true)
	private val silentRotationValue = BoolValue("SilentRotation", true)
	private val jitterValue = BoolValue("Jitter", false)
	private val jitterRateYaw = IntegerValue("YawJitterRate", 50, 0, 100)
	private val jitterRatePitch = IntegerValue("PitchJitterRate", 50, 0, 100)
	private val maxPitchJitterStrengthValue: FloatValue = object : FloatValue("MaxPitchJitterStrength", 1f, 0f, 5f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = minPitchJitterStrengthValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minYawJitterStrengthValue: FloatValue = object : FloatValue("MinYawJitterStrength", 0f, 0f, 5f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = maxYawJitterStrengthValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val maxYawJitterStrengthValue: FloatValue = object : FloatValue("MaxYawJitterStrength", 1f, 0f, 5f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = minYawJitterStrengthValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minPitchJitterStrengthValue: FloatValue = object : FloatValue("MinPitchJitterStrength", 0f, 0f, 5f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = maxPitchJitterStrengthValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val lockCenterValue = BoolValue("LockCenter", false)
	private val randomCenterValue = BoolValue("RandomCenter", false)
	private val outborderValue = BoolValue("Outborder", false)

	private val hitboxDecrementValue = FloatValue("EnemyHitboxDecrement", 0.2f, 0.15f, 0.45f)
	private val centerSearchSensitivityValue = FloatValue("SearchCenterSensitivity", 0.2f, 0.15f, 0.25f)

	/**
	 * Keep Rotation
	 */
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

	/**
	 * Acceleration
	 */
	private val maxAccelerationRatioValue: FloatValue = object : FloatValue("MaxAccelerationRatio", 0f, 0f, .99f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minAccelerationRatioValue.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minAccelerationRatioValue: FloatValue = object : FloatValue("MinAccelerationRatio", 0f, 0f, .99f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxAccelerationRatioValue.get()
			if (v < newValue) this.set(v)
		}
	}

	/**
	 * TurnSpeed
	 */
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 0f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 0f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) set(v)
		}
	}

	/**
	 * Rotation Reset TurnSpeed
	 */
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

	/**
	 * Rotation strafe (to bypass AAC4)
	 */
	private val rotationStrafeValue = ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off")

	/**
	 * Field of View
	 */
	private val fovValue = FloatValue("FoV", 180f, 0f, 180f)
	private val fovModeValue = ListValue("FoVMode", arrayOf("ServerRotation", "ClientRotation"), "ClientRotation")

	/**
	 * Target Predict
	 */
	private val predictValue = BoolValue("Predict", true)
	private val maxPredictSizeValue: FloatValue = object : FloatValue("MaxPredictSize", 1f, -2f, 2f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minPredictSizeValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minPredictSizeValue: FloatValue = object : FloatValue("MinPredictSize", 1f, -2f, 2f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxPredictSizeValue.get()
			if (v < newValue) set(v)
		}
	}

	/**
	 * Player Predict
	 */
	private val playerPredictValue = BoolValue("PlayerPredict", true)
	private val maxPlayerPredictSizeValue: FloatValue = object : FloatValue("MaxPlayerPredictSize", 1f, -2f, 2f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minPlayerPredictSizeValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minPlayerPredictSizeValue: FloatValue = object : FloatValue("MinPlayerPredictSize", 1f, -2f, 2f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxPlayerPredictSizeValue.get()
			if (v < newValue) set(v)
		}
	}

	/**
	 * Bypass
	 */
	private val failRateValue = FloatValue("FailRate", 0f, 0f, 100f)
	private val noInventoryAttackValue = BoolValue("NoInvAttack", false)
	private val noInventoryDelayValue = IntegerValue("NoInvDelay", 200, 0, 500)
	private val limitedMultiTargetsValue = IntegerValue("LimitedMultiTargets", 0, 0, 50)

	/**
	 * Visual
	 */
	private val markValue = BoolValue("Mark", true)
	private val fakeSharpValue = BoolValue("FakeSharp", true)
	private val particles = IntegerValue("Particles", 1, 0, 10)

	/**
	 * MODULE
	 */

	// Target
	var target: IEntityLivingBase? = null
	private var currentTarget: IEntityLivingBase? = null
	private var hitable = false
	private val previouslySwitchedTargets = mutableListOf<Int>()

	// Attack delay
	private val attackTimer = MSTimer()
	private var attackDelay = 0L
	private var clicks = 0

	// Suspend killaura timer
	private val suspendTimer = MSTimer()
	private var suspend = 0L

	// Ranges
	private var attackRange = 0f
	private var aimRange = 0f
	private var swingRange = 0f
	private var blockRange = 0f

	private var fakeYaw = 0f
	private var fakePitch = 0f

	// Container Delay
	private var containerOpen = -1L

	// Server-side block status
	var serverSideBlockingStatus: Boolean = false

	// Client-side(= visual) block status
	var clientSideBlockingStatus: Boolean = false

	/**
	 * Did last attack failed
	 */
	private var failedToHit = false

	/**
	 * Target of auto-block
	 */
	private var autoBlockTarget: IEntityLivingBase? = null

	init
	{
		cooldownValue.isSupported = Backend.REPRESENTED_BACKEND_VERSION != MinecraftVersion.MC_1_8
	}

	/**
	 * Enable kill aura module
	 */
	override fun onEnable()
	{
		updateTarget(mc.theWorld ?: return, mc.thePlayer ?: return)
	}

	/**
	 * Disable kill aura module
	 */
	override fun onDisable()
	{
		target = null
		currentTarget = null
		hitable = false
		failedToHit = false
		previouslySwitchedTargets.clear()
		attackTimer.reset()
		clicks = 0
		stopBlocking()
	}

	/**
	 * Motion event
	 */
	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (event.eventState == EventState.POST)
		{
			target ?: return
			currentTarget ?: return

			// Update hitable
			updateHitable(theWorld, thePlayer)

			// Delayed-AutoBlock
			if (autoBlockValue.get().equals("AfterTick", true) && canBlock && currentTarget != null) startBlocking(currentTarget, interactAutoBlockValue.get() && hitable)

			return
		}

		if (rotationStrafeValue.get().equals("Off", true)) update(theWorld, thePlayer)
	}

	/**
	 * Strafe event
	 */
	@EventTarget
	fun onStrafe(event: StrafeEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (rotationStrafeValue.get().equals("Off", true)) return

		update(theWorld, thePlayer)

		if (currentTarget != null && RotationUtils.targetRotation != null)
		{
			when (rotationStrafeValue.get().toLowerCase())
			{
				"strict" ->
				{
					val (yaw, _) = RotationUtils.targetRotation ?: return
					var strafe = event.strafe
					var forward = event.forward
					val friction = event.friction

					var f = strafe * strafe + forward * forward

					if (f >= 1.0E-4F)
					{
						f = sqrt(f)

						if (f < 1.0F) f = 1.0F

						f = friction / f
						strafe *= f
						forward *= f

						val yawRadians = WMathHelper.toRadians(yaw)
						val yawSin = functions.sin(yawRadians)
						val yawCos = functions.cos(yawRadians)

						thePlayer.motionX += strafe * yawCos - forward * yawSin
						thePlayer.motionZ += forward * yawCos + strafe * yawSin
					}
					event.cancelEvent()
				}

				"silent" ->
				{
					update(theWorld, thePlayer)

					RotationUtils.targetRotation?.applyStrafeToPlayer(event)
					event.cancelEvent()
				}
			}
		}
	}

	fun update(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{

		// CancelRun & NoInventory
		if (cancelRun || (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))) return

		// Update target
		updateTarget(theWorld, thePlayer)

		// Pre-AutoBlock
		if (autoBlockTarget != null && !autoBlockValue.get().equals("AfterTick", ignoreCase = true) && canBlock && (!autoBlockHitableCheckValue.get() || hitable)) startBlocking(autoBlockTarget, interactAutoBlockValue.get())
		else if (canBlock) stopBlocking()

		target ?: return

		// Target
		currentTarget = target
		if (!targetModeValue.get().equals("Switch", ignoreCase = true) && EntityUtils.isEnemy(currentTarget, aacValue.get())) target = currentTarget
	}

	/**
	 * Update event
	 */
	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		if (cancelRun)
		{
			target = null
			currentTarget = null
			hitable = false
			stopBlocking()
			return
		}

		attackRange = attackRangeValue.get()
		aimRange = aimRangeValue.get()
		swingRange = swingRangeValue.get()
		blockRange = blockRangeValue.get()

		if (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))
		{
			target = null
			currentTarget = null
			hitable = false
			if (classProvider.isGuiContainer(mc.currentScreen)) containerOpen = System.currentTimeMillis()
			return
		}

		target ?: return

		if (target != null && currentTarget != null && (Backend.MINECRAFT_VERSION_MINOR == 8 || (mc.thePlayer ?: return).getCooledAttackStrength(0.0F) >= cooldownValue.get()))
		{
			while (clicks > 0)
			{
				runAttack()
				clicks--
			}
		}
	}

	/**
	 * Render event
	 */
	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		if (cancelRun)
		{
			target = null
			currentTarget = null
			hitable = false
			stopBlocking()
			return
		}

		// NoInventory
		if (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))
		{
			target = null
			currentTarget = null
			hitable = false
			if (classProvider.isGuiContainer(mc.currentScreen)) containerOpen = System.currentTimeMillis()
			return
		}

		target ?: return

		if (markValue.get() && !targetModeValue.get().equals("Multi", ignoreCase = true)) // Draw Mark
			RenderUtils.drawPlatform(
				target!!, if (hitable)
				{
					if (failedToHit) Color(0, 0, 255, 70)
					else Color(0, 255, 0, 70)
				}
				else Color(255, 0, 0, 70)
			)

		if (currentTarget != null && attackTimer.hasTimePassed(attackDelay) && (currentTarget ?: return).hurtTime <= hurtTimeValue.get())
		{
			clicks++
			attackTimer.reset()
			attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
		}
	}

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		if (fovValue.get() < 180) RenderUtils.drawFoVCircle(fovValue.get())
	}

	/**
	 * Handle entity move
	 */
	@EventTarget
	fun onEntityMove(event: EntityMovementEvent)
	{
		val movedEntity = event.movedEntity

		if (target == null || movedEntity != currentTarget) return

		updateHitable(mc.theWorld ?: return, mc.thePlayer ?: return)
	}

	/**
	 * Attack enemy
	 */
	private fun runAttack()
	{
		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		val distance = thePlayer.getDistanceToEntityBox(currentTarget!!)

		// Settings
		val failRate = failRateValue.get()
		val swing = swingValue.get()
		val multi = targetModeValue.get().equals("Multi", ignoreCase = true)
		val openInventory = aacValue.get() && classProvider.isGuiContainer(mc.currentScreen)

		// FailRate
		failedToHit = failRate > 0 && Random.nextInt(100) <= failRate

		// Close inventory when open
		if (openInventory) mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

		// Check is not hitable or check failrate
		val fakeAttack = !hitable || failedToHit

		if (fakeAttack)
		{

			// Stop Blocking before FAKE attack
			if (thePlayer.isBlocking || serverSideBlockingStatus)
			{
				mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))
				serverSideBlockingStatus = false
				clientSideBlockingStatus = false
			}

			// FAKE Swing (to bypass hit/miss rate checks)
			if (swing && (failedToHit || (fakeSwingValue.get() && distance <= swingRange))) thePlayer.swingItem()

			// Start blocking after FAKE attack
			if ((thePlayer.isBlocking || (canBlock && distance <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(currentTarget!!, interactAutoBlockValue.get())
		}
		else
		{

			// Attack
			if (multi)
			{
				var targets = 0

				for (entity in theWorld.loadedEntityList)
				{
					val dist = thePlayer.getDistanceToEntityBox(entity)

					if (classProvider.isEntityLivingBase(entity) && EntityUtils.isEnemy(entity, aacValue.get()) && dist <= getAttackRange(thePlayer, entity))
					{
						attackEntity(entity.asEntityLivingBase())

						targets += 1

						if (limitedMultiTargetsValue.get() != 0 && limitedMultiTargetsValue.get() <= targets) break
					}
				}
			}
			else attackEntity(currentTarget!!)
		}

		previouslySwitchedTargets.add(if (aacValue.get()) (target ?: return).entityId else (currentTarget ?: return).entityId)

		if (!fakeAttack && target == currentTarget) target = null

		// Open inventory
		if (openInventory) mc.netHandler.addToSendQueue(createOpenInventoryPacket())
	}

	/**
	 * Update current target
	 */
	private fun updateTarget(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{

		// Reset fixed target to null
		target = null

		// Settings
		val hurtTime = hurtTimeValue.get()
		val fov = fovValue.get()
		val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)
		val playerPredict = playerPredictValue.get()
		val minPlayerPredictSize = minPlayerPredictSizeValue.get()
		val maxPlayerPredictSize = maxPlayerPredictSizeValue.get()

		// Find possible targets
		val targets = mutableListOf<IEntityLivingBase>()
		val abTargets = mutableListOf<IEntityLivingBase>()

		for (entity in theWorld.loadedEntityList)
		{
			if (!classProvider.isEntityLivingBase(entity) || !EntityUtils.isEnemy(entity, aacValue.get()) || switchMode && previouslySwitchedTargets.contains(entity.entityId)) continue

			val distance = thePlayer.getDistanceToEntityBox(entity)
			val entityFov = when (fovModeValue.get())
			{
				"ServerRotation" -> RotationUtils.getServerRotationDifference(thePlayer, entity, playerPredict, minPlayerPredictSize, maxPlayerPredictSize)
				else -> RotationUtils.getClientRotationDifference(thePlayer, entity, playerPredict, minPlayerPredictSize, maxPlayerPredictSize)
			}

			if (fov == 180F || entityFov <= fov)
			{
				if (distance <= blockRange && (!autoBlockHurtTimeCheckValue.get() || entity.asEntityLivingBase().hurtTime <= hurtTime)) abTargets.add(entity.asEntityLivingBase())
				if (distance <= getAttackRange(thePlayer, entity) && entity.asEntityLivingBase().hurtTime <= hurtTime) targets.add(entity.asEntityLivingBase()) // Attack attack-ables first.
			}
		}

		if (targets.isEmpty()) for (entity in theWorld.loadedEntityList)  // If there is no attackable entities found, search about pre-aimable entities and pre-swingable entities instead.
		{
			if (!classProvider.isEntityLivingBase(entity) || !EntityUtils.isEnemy(entity, aacValue.get()) || switchMode && previouslySwitchedTargets.contains(entity.entityId)) continue

			val distance = thePlayer.getDistanceToEntityBox(entity)
			val entityFov = when (fovModeValue.get())
			{
				"ServerRotation" -> RotationUtils.getServerRotationDifference(thePlayer, entity, playerPredict, minPlayerPredictSize, maxPlayerPredictSize)
				else -> RotationUtils.getClientRotationDifference(thePlayer, entity, playerPredict, minPlayerPredictSize, maxPlayerPredictSize)
			}

			if ((fov == 180F || entityFov <= fov) && distance <= maxTargetRange && entity.asEntityLivingBase().hurtTime <= hurtTime) targets.add(entity.asEntityLivingBase())
		}

		// Sort targets by priority
		when (priorityValue.get().toLowerCase())
		{
			"distance" ->
			{

				// Sort by distance
				targets.sortBy(thePlayer::getDistanceToEntityBox) // Sort by distance
				abTargets.sortBy(thePlayer::getDistanceToEntityBox)
			}

			"health" ->
			{

				// Sort by health
				targets.sortBy(IEntityLivingBase::health)
				abTargets.sortBy(IEntityLivingBase::health)
			}

			"serverdirection" ->
			{

				// Sort by server-sided rotation difference
				targets.sortBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }
				abTargets.sortBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }
			}

			"clientdirection" ->
			{

				// Sort by client-sided rotation difference
				targets.sortBy { RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }
				abTargets.sortBy { RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }
			}

			"livingtime" ->
			{

				// Sort by existence
				targets.sortBy { -it.ticksExisted }
				abTargets.sortBy { -it.ticksExisted }
			}
		}

		autoBlockTarget = abTargets.firstOrNull()

		// Find best target
		for (entity in targets)
		{ // Update rotations to current target
			if (thePlayer.getDistanceToEntityBox(entity) <= aimRange && !updateRotations(theWorld, thePlayer, entity)) continue

			// Set target to current entity
			target = entity
			return
		}

		// Cleanup previouslySwitchedTargets when no target found and try again
		if (previouslySwitchedTargets.isNotEmpty())
		{
			previouslySwitchedTargets.clear()
			updateTarget(theWorld, thePlayer)
		}
	}

	/**
	 * Attack [entity]
	 */
	private fun attackEntity(entity: IEntityLivingBase)
	{
		val thePlayer = mc.thePlayer ?: return

		// Stop blocking
		if (thePlayer.isBlocking || serverSideBlockingStatus)
		{
			mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))
			serverSideBlockingStatus = false
			clientSideBlockingStatus = false
		}

		// Call attack event
		LiquidBounce.eventManager.callEvent(AttackEvent(entity))

		// Attack target
		if (swingValue.get() && Backend.MINECRAFT_VERSION_MINOR == 8) thePlayer.swingItem()

		mc.netHandler.addToSendQueue(classProvider.createCPacketUseEntity(entity, ICPacketUseEntity.WAction.ATTACK))

		if (swingValue.get() && Backend.MINECRAFT_VERSION_MINOR != 8) thePlayer.swingItem()

		if (!keepSprintValue.get() && mc.playerController.currentGameType != IWorldSettings.WGameType.SPECTATOR) thePlayer.attackTargetEntityWithCurrentItem(entity)

		// Extra critical effects
		val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

		val crackSize = particles.get()
		if (crackSize > 0) repeat(crackSize) {
			// Critical Effect
			if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS)) && thePlayer.ridingEntity == null || criticals.state && criticals.canCritical(thePlayer)) thePlayer.onCriticalHit(target ?: return)

			// Enchant Effect
			if (functions.getModifierForCreature(thePlayer.heldItem, (target ?: return@attackEntity).creatureAttribute) > 0.0f || fakeSharpValue.get()) thePlayer.onEnchantmentCritical(target ?: return)
		}

		// Start blocking after attack
		if ((thePlayer.isBlocking || (canBlock && thePlayer.getDistanceToEntityBox(entity) <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(entity, interactAutoBlockValue.get())

		@Suppress("ConstantConditionIf") if (Backend.MINECRAFT_VERSION_MINOR != 8) thePlayer.resetCooldown()
	}

	/**
	 * Update killaura rotations to enemy
	 */
	private fun updateRotations(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, entity: IEntity): Boolean
	{
		var targetBox = entity.entityBoundingBox

		// Entity movement predict
		if (predictValue.get())
		{
			val minPredictSize = minPredictSizeValue.get()
			val maxPredictSize = maxPredictSizeValue.get()

			val xPredict = (entity.posX - entity.prevPosX - (thePlayer.posX - thePlayer.prevPosX)) * RandomUtils.nextFloat(minPredictSize, maxPredictSize)
			val yPredict = (entity.posY - entity.prevPosY - (thePlayer.posY - thePlayer.prevPosY)) * RandomUtils.nextFloat(minPredictSize, maxPredictSize)
			val zPredict = (entity.posZ - entity.prevPosZ - (thePlayer.posZ - thePlayer.prevPosZ)) * RandomUtils.nextFloat(minPredictSize, maxPredictSize)

			targetBox = targetBox.offset(xPredict, yPredict, zPredict)
		}

		// Rotation Mode
		val mode = when
		{
			lockCenterValue.get() -> RotationUtils.SearchCenterMode.LOCK_CENTER
			outborderValue.get() && !attackTimer.hasTimePassed(attackDelay shr 1) -> RotationUtils.SearchCenterMode.OUT_BORDER
			randomCenterValue.get() -> RotationUtils.SearchCenterMode.RANDOM_GOOD_CENTER
			else -> RotationUtils.SearchCenterMode.SEARCH_GOOD_CENTER
		}

		// Jitter
		val jitterEnabled = jitterValue.get() && (thePlayer.getDistanceToEntityBox(entity) <= min(maxAttackRange, if (fakeSwingValue.get()) swingRange else Float.MAX_VALUE))
		val jitterYawRate = jitterRateYaw.get()
		val jitterPitchRate = jitterRatePitch.get()
		val jitterMinYaw = minYawJitterStrengthValue.get()
		val jitterMaxYaw = maxYawJitterStrengthValue.get()
		val jitterMinPitch = minPitchJitterStrengthValue.get()
		val jitterMaxPitch = maxPitchJitterStrengthValue.get()
		val jitterData = RotationUtils.JitterData(jitterYawRate, jitterPitchRate, jitterMinYaw, jitterMaxYaw, jitterMinPitch, jitterMaxPitch)

		val canAttackThroughWalls = thePlayer.getDistanceToEntityBox(entity) <= throughWallsRangeValue.get()
		val hitboxDecrement = hitboxDecrementValue.get().toDouble()
		val searchSensitivity = centerSearchSensitivityValue.get().toDouble()

		// Player Predict
		val playerPrediction = playerPredictValue.get()
		val minPlayerPredictSize = minPlayerPredictSizeValue.get()
		val maxPlayerPredictSize = maxPlayerPredictSizeValue.get()

		// Search
		val (_, rotation) = RotationUtils.searchCenter(theWorld, thePlayer, targetBox, mode, jitterEnabled, jitterData, playerPrediction, minPlayerPredictSize, maxPlayerPredictSize, canAttackThroughWalls, aimRange, hitboxDecrement, searchSensitivity) ?: return false

		fakeYaw = rotation.yaw
		fakePitch = rotation.pitch

		val maxTurnSpeed = maxTurnSpeedValue.get()
		val minTurnSpeed = minTurnSpeedValue.get()

		if (!rotationsValue.get() || maxTurnSpeed <= 0F) return true

		// Limit TurnSpeed
		val turnSpeed = minTurnSpeed + (maxTurnSpeed - minTurnSpeed) * Random.nextFloat()

		// Acceleration
		val maxAcceleration = maxAccelerationRatioValue.get()
		val minAcceleration = minAccelerationRatioValue.get()
		val acceleration = minAcceleration + (maxAcceleration - minAcceleration) * Random.nextFloat()

		val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, turnSpeed, acceleration)

		fakeYaw = limitedRotation.yaw
		fakePitch = limitedRotation.pitch

		if (silentRotationValue.get())
		{
			val keepLength = if (keepRotationValue.get())
			{
				val maxKeepLength = maxKeepRotationTicksValue.get()
				val minKeepLength = minKeepRotationTicksValue.get()

				if (maxKeepLength == minKeepLength) maxKeepLength else minKeepLength + Random.nextInt(maxKeepLength - minKeepLength)
			}
			else 0

			RotationUtils.setTargetRotation(limitedRotation, keepLength)
		}
		else limitedRotation.applyRotationToPlayer(thePlayer)

		val maxResetSpeed = maxResetTurnSpeed.get().coerceAtLeast(20F)
		val minResetSpeed = minResetTurnSpeed.get().coerceAtLeast(20F)
		RotationUtils.setNextResetTurnSpeed(minResetSpeed, maxResetSpeed)

		return true
	}

	/**
	 * Check if enemy is hitable with current rotations
	 */
	private fun updateHitable(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		if (!rotationsValue.get() || maxTurnSpeedValue.get() <= 0F) // Disable hitable check if turn speed is zero
		{
			hitable = true
			return
		}

		val multiaura = targetModeValue.get().equals("Multi", ignoreCase = true)
		val reach = min(maxAttackRange.toDouble(), thePlayer.getDistanceToEntityBox(target!!)) + 1

		if (raycastValue.get())
		{
			val raycastedEntity = RaycastUtils.raycastEntity(reach, fakeYaw, fakePitch, object : RaycastUtils.EntityFilter
			{
				override fun canRaycast(entity: IEntity?): Boolean
				{
					val aac = aacValue.get()
					val livingRaycast = livingRaycastValue.get()
					val raycastIgnored = raycastIgnoredValue.get()

					return (!livingRaycast || (classProvider.isEntityLivingBase(entity) && !classProvider.isEntityArmorStand(entity))) && (EntityUtils.isEnemy(entity, aac) || raycastIgnored || aac && theWorld.getEntitiesWithinAABBExcludingEntity(entity, entity!!.entityBoundingBox).isNotEmpty())
				}
			})

			if (raycastedEntity != null && classProvider.isEntityLivingBase(raycastedEntity) && (LiquidBounce.moduleManager[NoFriends::class.java].state || !(classProvider.isEntityPlayer(raycastedEntity) && raycastedEntity.asEntityPlayer().isClientFriend()))) currentTarget = raycastedEntity.asEntityLivingBase()

			hitable = currentTarget == raycastedEntity
		}
		else hitable = if (currentTarget != null) if (multiaura) thePlayer.getDistanceToEntityBox(currentTarget!!) <= (reach - 1) else RotationUtils.isFaced(currentTarget, reach) else false
	}

	/**
	 * Start blocking
	 */
	private fun startBlocking(interactEntity: IEntity?, interact: Boolean)
	{
		val thePlayer = mc.thePlayer ?: return

		val autoBlockMode = autoBlockValue.get()
		val blockRate = blockRate.get()

		// BlockRate check
		if (!(blockRate > 0 && Random.nextInt(100) <= blockRate)) return

		val visual = !autoBlockMode.equals("Off", true) // Fake, Packet, AfterTick
		val packet = visual && !autoBlockMode.equals("Fake", true) // Packet, AfterTick

		if (packet && !serverSideBlockingStatus)
		{

			// Interact block
			if (interact && interactEntity != null)
			{
				val positionEye = mc.renderViewEntity?.getPositionEyes(1F)

				val expandSize = interactEntity.collisionBorderSize.toDouble()
				val boundingBox = interactEntity.entityBoundingBox.expand(expandSize, expandSize, expandSize)

				val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch)
				val yawRadians = WMathHelper.toRadians(yaw)
				val pitchRadians = WMathHelper.toRadians(pitch)

				val yawCos = functions.cos(-yawRadians - WMathHelper.PI)
				val yawSin = functions.sin(-yawRadians - WMathHelper.PI)
				val pitchCos = -functions.cos(-pitchRadians)
				val pitchSin = functions.sin(-pitchRadians)

				val range = min(maxAttackRange.toDouble(), thePlayer.getDistanceToEntityBox(interactEntity)) + 1
				val lookAt = positionEye!!.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

				val movingObject = boundingBox.calculateIntercept(positionEye, lookAt)
				if (movingObject != null)
				{
					val hitVec = movingObject.hitVec

					mc.netHandler.addToSendQueue(
						classProvider.createCPacketUseEntity(
							interactEntity, WVec3(
								hitVec.xCoord - interactEntity.posX, hitVec.yCoord - interactEntity.posY, hitVec.zCoord - interactEntity.posZ
							)
						)
					)
					mc.netHandler.addToSendQueue(classProvider.createCPacketUseEntity(interactEntity, ICPacketUseEntity.WAction.INTERACT))
				}
			}

			mc.netHandler.addToSendQueue(
				classProvider.createCPacketPlayerBlockPlacement(
					WBlockPos(-1, -1, -1), 255, (mc.thePlayer ?: return).inventory.getCurrentItemInHand(), 0.0F, 0.0F, 0.0F
				)
			)
			serverSideBlockingStatus = true
		}

		if (!clientSideBlockingStatus && visual) clientSideBlockingStatus = true
	}

	/**
	 * Stop blocking
	 */
	private fun stopBlocking()
	{
		if (serverSideBlockingStatus)
		{
			mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))
			serverSideBlockingStatus = false
		}
		clientSideBlockingStatus = false
	}

	/**
	 * Check if run should be cancelled
	 */
	private val cancelRun: Boolean
		get()
		{
			val blink = LiquidBounce.moduleManager[Blink::class.java] as Blink
			return mc.thePlayer!!.spectator || !EntityUtils.isAlive(mc.thePlayer!!, aacValue.get()) || blink.state || LiquidBounce.moduleManager[FreeCam::class.java].state || !suspendTimer.hasTimePassed(suspend)
		}

	/**
	 * Check if player is able to block
	 */
	private val canBlock: Boolean
		get() = mc.thePlayer!!.heldItem != null && classProvider.isItemSword(mc.thePlayer!!.heldItem!!.item) && Backend.MINECRAFT_VERSION_MINOR == 8

	/**
	 * Range
	 */
	private val maxAttackRange: Float
		get() = max(attackRange, throughWallsRangeValue.get())

	private val maxTargetRange: Float
		get() = max(aimRange, max(maxAttackRange, if (fakeSwingValue.get()) swingRange else 0f))

	private fun getAttackRange(thePlayer: IEntityPlayerSP, entity: IEntity) = (if (thePlayer.getDistanceToEntityBox(entity) >= throughWallsRangeValue.get()) attackRange else throughWallsRangeValue.get()) - if (thePlayer.sprinting) rangeSprintReducementValue.get() else 0F

	/**
	 * HUD Tag
	 */
	override val tag: String
		get() = targetModeValue.get()

	val hasTarget: Boolean
		get() = state && target != null

	fun suspend(time: Long)
	{
		suspend = time
		suspendTimer.reset()
	}
}
