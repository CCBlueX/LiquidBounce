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
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
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
import net.ccbluex.liquidbounce.utils.extensions.isClientTarget
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// TODO: Visually start-stop blocking like as Xave
@ModuleInfo(name = "KillAura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
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

			val i2 = aimRangeValue.get()
			if (i < newValue) this.set(i2)
		}
	}

	/**
	 * Combo Reach
	 */
	private val comboReachValue = BoolValue("ComboReach", false)
	private val comboReachIncrementValue = FloatValue("ComboReachIncrement", 0.1F, 0.02F, 0.5F)
	private val comboReachMaxValue = FloatValue("ComboReachMax", 0.5F, 0.02F, 3F)

	// Modes
	private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection", "LivingTime"), "Distance")
	private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")

	// Switch Delay
	private val maxSwitchDelayValue: IntegerValue = object : IntegerValue("MaxSwitchDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minSwitchDelayValue.get()
			if (i > newValue) set(i)

			switchDelay = TimeUtils.randomDelay(minSwitchDelayValue.get(), this.get())
		}
	}

	private val minSwitchDelayValue: IntegerValue = object : IntegerValue("MinSwitchDelay", 0, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxSwitchDelayValue.get()
			if (i < newValue) set(i)

			switchDelay = TimeUtils.randomDelay(this.get(), maxSwitchDelayValue.get())
		}
	}

	// Bypass
	private val swingValue = BoolValue("Swing", true)
	private val keepSprintValue = BoolValue("KeepSprint", true)

	// AutoBlock
	private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Fake", "Packet", "AfterTick"), "Packet")
	private val interactAutoBlockValue = BoolValue("InteractAutoBlock", false)
	private val autoBlockHitableCheckValue = BoolValue("AutoBlockHitableCheck", false)
	private val autoBlockHurtTimeCheckValue = BoolValue("AutoBlockHurtTimeCheck", true)
	private val autoBlockWallCheckValue = BoolValue("AutoBlockWallCheck", false) // a.k.a. SmartBlock
	private val blockRangeValue: FloatValue = FloatValue("BlockRange", 6f, 1f, 12f)
	private val blockRate = IntegerValue("BlockRate", 100, 1, 100)

	// Raycast
	private val raycastValue = BoolValue("RayCast", true)
	private val raycastIgnoredValue = BoolValue("RayCastIgnored", false)
	private val livingRaycastValue = BoolValue("LivingRayCast", true)

	// Bypass
	private val aacValue = BoolValue("AAC", false)
	private val rotationsValue = BoolValue("Rotations", true)
	private val lockValue = BoolValue("Lock", true)
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
	private val randomCenterSizeValue = FloatValue("RandomCenterSize", 0.8F, 0.1F, 1.0F)
	private val outborderValue = BoolValue("Outborder", false)

	private val hitboxDecrementValue = FloatValue("EnemyHitboxDecrement", 0.15f, 0f, 0.3f)
	private val centerSearchSensitivityValue = FloatValue("SearchCenterSensitivity", 0.2f, 0.1f, 0.25f)

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
	 * Backtrace
	 */
	private val backtraceValue = BoolValue("Backtrace", false)
	private val backtraceTicksValue: IntegerValue = IntegerValue("BacktraceTicks", 3, 1, 6)

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
	private val markValue = ListValue("Mark", arrayOf("None", "Platform", "Box"), "Platform")
	private val fakeSharpValue = BoolValue("FakeSharp", true)
	private val particles = IntegerValue("Particles", 1, 0, 10)

	private val disableOnDeathValue = BoolValue("DisableOnDeath", true)
	private val suspendWhileConsumingValue = BoolValue("SuspendWhileConsuming", true)

	/**
	 * MODULE
	 */

	// Target
	var target: IEntityLivingBase? = null
	private var currentTarget: IEntityLivingBase? = null
	private var hitable = false
	private val previouslySwitchedTargets = mutableSetOf<Int>()

	private var lastTargetID: Int = 0

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
	private var comboReach = 0f

	private var lastYaw = 0f
	private var lastPitch = 0f

	private var predict = 1f

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

	private var failedToRotate = false

	/**
	 * Target of auto-block
	 */
	private var autoBlockTarget: IEntityLivingBase? = null

	private var switchDelay = TimeUtils.randomDelay(minSwitchDelayValue.get(), maxSwitchDelayValue.get())
	private val switchDelayTimer = MSTimer()

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
		comboReach = 0.0F
		stopBlocking()
	}

	@EventTarget
	fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent)
	{
		if (disableOnDeathValue.get())
		{
			state = false
			LiquidBounce.hud.addNotification("KillAura", "Disabled KillAura due world change", 1000L, Color.red)
		}
	}

	@EventTarget
	fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent?)
	{
		if (mc.thePlayer == null || mc.theWorld == null)
		{
			state = false
			LiquidBounce.hud.addNotification("KillAura", "Disabled KillAura due world change", 1000L, Color.red)
		}
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
			updateComboReach()

			target ?: return
			currentTarget ?: return

			// Update hitable
			updateHitable(theWorld, thePlayer)

			// Delayed-AutoBlock
			if (autoBlockValue.get().equals("AfterTick", true) && getCanBlock(thePlayer)) startBlocking(thePlayer, currentTarget, interactAutoBlockValue.get() && hitable)

			return
		}
		else if (rotationStrafeValue.get().equals("Off", true)) update(theWorld, thePlayer)
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

						val func = functions

						val yawRadians = WMathHelper.toRadians(yaw)
						val yawSin = func.sin(yawRadians)
						val yawCos = func.cos(yawRadians)

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
		if (shouldCancelRun(thePlayer) || (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))) return

		// Update target
		updateTarget(theWorld, thePlayer)

		// Pre-AutoBlock
		if (autoBlockTarget != null && !autoBlockValue.get().equals("AfterTick", ignoreCase = true) && getCanBlock(thePlayer) && (!autoBlockHitableCheckValue.get() || hitable)) startBlocking(thePlayer, autoBlockTarget, interactAutoBlockValue.get())
		else if (getCanBlock(thePlayer)) stopBlocking()

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
		val thePlayer = mc.thePlayer ?: return

		if (shouldCancelRun(thePlayer))
		{
			target = null
			currentTarget = null
			hitable = false
			comboReach = 0.0F
			stopBlocking()
			return
		}

		val screen = mc.currentScreen

		val provider = classProvider

		attackRange = attackRangeValue.get()
		aimRange = aimRangeValue.get()
		swingRange = swingRangeValue.get()
		blockRange = blockRangeValue.get()

		if (noInventoryAttackValue.get() && (provider.isGuiContainer(screen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))
		{
			target = null
			currentTarget = null
			hitable = false
			comboReach = 0.0F

			if (provider.isGuiContainer(screen)) containerOpen = System.currentTimeMillis()

			return
		}

		target ?: return

		if (target != null && currentTarget != null && (Backend.MINECRAFT_VERSION_MINOR == 8 || thePlayer.getCooledAttackStrength(0.0F) >= cooldownValue.get()))
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
		if (shouldCancelRun(mc.thePlayer ?: return))
		{
			target = null
			currentTarget = null
			hitable = false
			comboReach = 0.0F
			stopBlocking()
			return
		}

		val screen = mc.currentScreen

		val provider = classProvider

		// NoInventory
		if (noInventoryAttackValue.get() && (provider.isGuiContainer(screen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))
		{
			target = null
			currentTarget = null
			hitable = false
			comboReach = 0.0F
			if (provider.isGuiContainer(screen)) containerOpen = System.currentTimeMillis()
			return
		}

		val target = target ?: return
		val targetEntityId = target.entityId

		val markMode = markValue.get().toLowerCase()

		if (markMode != "none" && !targetModeValue.get().equals("Multi", ignoreCase = true))
		{
			val markColor = when
			{
				failedToHit -> 0x460000FF
				hitable -> 0x4600FF00
				else -> 0x46FF0000
			}

			val renderManager = mc.renderManager
			val renderPosX = renderManager.renderPosX
			val renderPosY = renderManager.renderPosY
			val renderPosZ = renderManager.renderPosZ

			val renderPartialTicks = mc.timer.renderPartialTicks

			var targetBB = target.entityBoundingBox

			targetBB = if (backtraceValue.get())
			{
				val backtraceTicks = backtraceTicksValue.get()

				val bb = LocationCache.getAABBBeforeNTicks(targetEntityId, backtraceTicks, targetBB)
				val lastBB = LocationCache.getAABBBeforeNTicks(targetEntityId, backtraceTicks + 1, targetBB)

				classProvider.createAxisAlignedBB(lastBB.minX + (bb.minX - lastBB.minX) * renderPartialTicks, lastBB.minY + (bb.minY - lastBB.minY) * renderPartialTicks, lastBB.minZ + (bb.minZ - lastBB.minZ) * renderPartialTicks, lastBB.maxX + (bb.maxX - lastBB.maxX) * renderPartialTicks, lastBB.maxY + (bb.maxY - lastBB.maxY) * renderPartialTicks, lastBB.maxZ + (bb.maxZ - lastBB.maxZ) * renderPartialTicks).offset(-renderPosX, -renderPosY, -renderPosZ)
			}
			else
			{
				val posX = target.posX
				val posY = target.posY
				val posZ = target.posZ

				val lastTickPosX = target.lastTickPosX
				val lastTickPosY = target.lastTickPosY
				val lastTickPosZ = target.lastTickPosZ

				val x = lastTickPosX + (posX - lastTickPosX) * renderPartialTicks - renderPosX
				val y = lastTickPosY + (posY - lastTickPosY) * renderPartialTicks - renderPosY
				val z = lastTickPosZ + (posZ - lastTickPosZ) * renderPartialTicks - renderPosZ

				targetBB.offset(-posX, -posY, -posZ).offset(x, y, z)
			}

			// Entity movement predict
			if (predictValue.get())
			{
				val xPredict = (target.posX - target.prevPosX) * predict
				val yPredict = (target.posY - target.prevPosY) * predict
				val zPredict = (target.posZ - target.prevPosZ) * predict

				targetBB = targetBB.offset(xPredict, yPredict, zPredict)
			}

			when (markMode)
			{
				"platform" -> classProvider.createAxisAlignedBB(targetBB.minX, targetBB.maxY + 0.2, targetBB.minZ, targetBB.maxX, targetBB.maxY + 0.26, targetBB.maxZ)
				"box" -> classProvider.createAxisAlignedBB(targetBB.minX, targetBB.minY, targetBB.minZ, targetBB.maxX, targetBB.maxY, targetBB.maxZ)
				else -> null
			}?.let { RenderUtils.drawAxisAlignedBB(it, markColor) }
		}


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

		updateComboReach()

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
		val netHandler = mc.netHandler

		val provider = classProvider

		val theTarget = target ?: return
		val theCurrentTarget = currentTarget ?: return

		val distance = thePlayer.getDistanceToEntityBox(theCurrentTarget)

		// Settings
		val failRate = failRateValue.get()
		val aac = aacValue.get()

		val openInventory = aac && provider.isGuiContainer(mc.currentScreen)
		val limitedMultiTargets = limitedMultiTargetsValue.get()

		// FailRate
		failedToHit = failRate > 0 && Random.nextInt(100) <= failRate

		// Close inventory when open
		if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

		// Check is not hitable or check failrate
		val fakeAttack = !hitable || failedToHit || failedToRotate

		if (fakeAttack)
		{
			if (swingValue.get() && distance <= swingRange && (failedToHit || fakeSwingValue.get()))
			{
				val isBlocking = thePlayer.isBlocking

				// Stop Blocking before FAKE attack
				if (isBlocking || serverSideBlockingStatus)
				{
					netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
					serverSideBlockingStatus = false
					clientSideBlockingStatus = false
				}

				// FAKE Swing (to bypass hit/miss rate checks)
				thePlayer.swingItem()

				// Start blocking after FAKE attack
				if ((isBlocking || (getCanBlock(thePlayer) && distance <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(thePlayer, theCurrentTarget, interactAutoBlockValue.get())
			}
		}
		else
		{
			if (comboReachValue.get()) comboReach = (comboReach + comboReachIncrementValue.get()).coerceAtMost(comboReachMaxValue.get())

			// Attack
			if (targetModeValue.get().equals("Multi", ignoreCase = true))
			{
				var targets = 0

				run {
					EntityUtils.getEntitiesInRadius(theWorld, thePlayer, maxAttackRange + 2.0).filter { EntityUtils.isEnemy(it, aac) }.filter { thePlayer.getDistanceToEntityBox(it) <= getAttackRange(thePlayer, it) }.map(IEntity::asEntityLivingBase).forEach { entity ->
						attackEntity(entity)
						targets += 1

						if (limitedMultiTargets != 0 && targets >= limitedMultiTargets) return@run
					}
				}
			}
			else attackEntity(theCurrentTarget)
		}

		if (switchDelayTimer.hasTimePassed(switchDelay))
		{
			previouslySwitchedTargets.add(if (aac) theTarget.entityId else theCurrentTarget.entityId)

			switchDelayTimer.reset()
			switchDelay = TimeUtils.randomDelay(minSwitchDelayValue.get(), maxSwitchDelayValue.get())
		}

		if (!fakeAttack && theTarget == theCurrentTarget)
		{
			lastTargetID = theTarget.entityId
			target = null
		}

		// Open inventory
		if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())
	}

	/**
	 * Update current target
	 */
	private fun updateTarget(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		if (target != null) lastTargetID = target!!.entityId

		// Reset fixed target to null
		target = null

		// Settings
		val hurtTime = hurtTimeValue.get()
		val fov = fovValue.get()
		val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)
		val playerPredict = playerPredictValue.get()
		val playerPredictSize = RotationUtils.MinMaxPair(minPlayerPredictSizeValue.get(), maxPlayerPredictSizeValue.get())

		// Find possible targets
		val targets = mutableListOf<IEntityLivingBase>()
		val abTargets = mutableListOf<IEntityLivingBase>()

		val aac = aacValue.get()
		val fovMode = fovModeValue.get()

		val autoBlockHurtTimeCheck = autoBlockHurtTimeCheckValue.get()
		val smartBlock = autoBlockWallCheckValue.get()

		val entityList = EntityUtils.getEntitiesInRadius(theWorld, thePlayer, maxTargetRange + 2.0).filter { EntityUtils.isEnemy(it, aac) }.filterNot { switchMode && previouslySwitchedTargets.contains(it.entityId) }.run { if (fov < 180f) filter { (if (fovMode == "ServerRotation") RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) else RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, playerPredictSize)) <= fov } else this }.map { it.asEntityLivingBase() to thePlayer.getDistanceToEntityBox(it) }

		entityList.forEach { (entity, distance) ->
			val entityHurtTime = entity.hurtTime

			if (distance <= blockRange && (!autoBlockHurtTimeCheck || entityHurtTime <= hurtTime) && (!smartBlock || RotationUtils.isVisible(theWorld, thePlayer, RotationUtils.getCenter(entity.entityBoundingBox))) /* Simple wall check */) abTargets.add(entity)
			if (distance <= getAttackRange(thePlayer, entity) && entityHurtTime <= hurtTime) targets.add(entity)
		}

		// If there is no attackable entities found, search about pre-aimable entities and pre-swingable entities instead.
		if (targets.isEmpty()) entityList.filter { it.second <= maxTargetRange }.forEach { targets.add(it.first) }

		val checkIsClientTarget = { entity: IEntity -> if (entity.isClientTarget()) -1000000.0 else 0.0 }

		// Sort targets by priority
		when (priorityValue.get().toLowerCase())
		{
			"distance" ->
			{
				// Sort by distance
				val selector = { entity: IEntity -> thePlayer.getDistanceToEntityBox(entity) + checkIsClientTarget(entity) }

				targets.sortBy(selector)
				abTargets.sortBy(selector)
			}

			"health" ->
			{
				// Sort by health
				val selector = { entity: IEntityLivingBase -> entity.health + checkIsClientTarget(entity) }

				targets.sortBy(selector)
				abTargets.sortBy(selector)
			}

			"serverdirection" ->
			{
				// Sort by server-sided rotation difference
				val selector = { entity: IEntityLivingBase -> RotationUtils.getServerRotationDifference(thePlayer, entity, playerPredict, playerPredictSize) + checkIsClientTarget(entity) }

				targets.sortBy(selector)
				abTargets.sortBy(selector)
			}

			"clientdirection" ->
			{
				// Sort by client-sided rotation difference
				val selector = { entity: IEntityLivingBase -> RotationUtils.getClientRotationDifference(thePlayer, entity, playerPredict, playerPredictSize) + checkIsClientTarget(entity) }

				targets.sortBy(selector)
				abTargets.sortBy(selector)
			}

			"livingtime" ->
			{
				// Sort by existence
				val selector = { entity: IEntityLivingBase -> -entity.ticksExisted + checkIsClientTarget(entity) }

				targets.sortBy(selector)
				abTargets.sortBy(selector)
			}
		}

		autoBlockTarget = abTargets.firstOrNull()

		// Find best target
		targets.firstOrNull {
			// Update rotations to current target
			val distance = thePlayer.getDistanceToEntityBox(it)
			distance <= aimRange && updateRotations(theWorld, thePlayer, it, distance <= attackRange)
		}?.let { entity ->
			// Set target to current entity
			target = entity

			if (entity.entityId != lastTargetID) comboReach = 0f

			return@updateTarget
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
		val netHandler = mc.netHandler

		val provider = classProvider

		val swing = swingValue.get()

		// Stop blocking
		if (thePlayer.isBlocking || serverSideBlockingStatus)
		{
			netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
			serverSideBlockingStatus = false
			clientSideBlockingStatus = false
		}

		// Call attack event
		LiquidBounce.eventManager.callEvent(AttackEvent(entity))

		// Attack target
		if (swing && Backend.MINECRAFT_VERSION_MINOR == 8) thePlayer.swingItem()

		netHandler.addToSendQueue(provider.createCPacketUseEntity(entity, ICPacketUseEntity.WAction.ATTACK))

		CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)

		if (swing && Backend.MINECRAFT_VERSION_MINOR != 8) thePlayer.swingItem()

		if (!keepSprintValue.get() && mc.playerController.currentGameType != IWorldSettings.WGameType.SPECTATOR) thePlayer.attackTargetEntityWithCurrentItem(entity)

		// Extra critical effects
		val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

		val crackSize = particles.get()
		if (crackSize > 0) repeat(crackSize) {
			val target = target ?: return@attackEntity

			// Critical Effect
			if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(provider.getPotionEnum(PotionType.BLINDNESS)) && thePlayer.ridingEntity == null || criticals.state && criticals.canCritical(thePlayer)) thePlayer.onCriticalHit(target)

			// Enchant Effect
			if (functions.getModifierForCreature(thePlayer.heldItem, target.creatureAttribute) > 0.0f || fakeSharpValue.get()) thePlayer.onEnchantmentCritical(target)
		}

		// Start blocking after attack
		if ((thePlayer.isBlocking || (getCanBlock(thePlayer) && thePlayer.getDistanceToEntityBox(entity) <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(thePlayer, entity, interactAutoBlockValue.get())

		@Suppress("ConstantConditionIf") if (Backend.MINECRAFT_VERSION_MINOR != 8) thePlayer.resetCooldown()
	}

	/**
	 * Update killaura rotations to enemy
	 */
	private fun updateRotations(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, entity: IEntity, isAttackRotation: Boolean): Boolean
	{
		if (predictValue.get()) predict = RandomUtils.nextFloat(minPredictSizeValue.get(), maxPredictSizeValue.get())

		val targetBox = aabbGetter(entity)

		val jitter = jitterValue.get()

		// Jitter
		val jitterData = if (jitter) RotationUtils.JitterData(jitterRateYaw.get(), jitterRatePitch.get(), minYawJitterStrengthValue.get(), maxYawJitterStrengthValue.get(), minPitchJitterStrengthValue.get(), maxPitchJitterStrengthValue.get()) else null

		val hitboxDecrement = hitboxDecrementValue.get().toDouble()
		val searchSensitivity = centerSearchSensitivityValue.get().toDouble()

		// Build the bit mask
		var flags = 0

		// Apply rotation mode to flags
		flags = flags or when
		{
			lockCenterValue.get() -> RotationUtils.LOCK_CENTER
			outborderValue.get() && !attackTimer.hasTimePassed(attackDelay shr 1) -> RotationUtils.OUT_BORDER
			randomCenterValue.get() -> RotationUtils.RANDOM_CENTER
			else -> 0
		}

		if (jitter && (thePlayer.getDistanceToEntityBox(entity) <= max(maxAttackRange, if (fakeSwingValue.get()) swingRange else Float.MIN_VALUE))) flags = flags or RotationUtils.JITTER
		if (playerPredictValue.get()) flags = flags or RotationUtils.PLAYER_PREDICT
		if (thePlayer.getDistanceToEntityBox(entity) <= throughWallsRangeValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK

		failedToRotate = false

		val searchCenter = { distance: Float, distanceOutOfRangeCallback: (() -> Unit)? -> RotationUtils.searchCenter(theWorld, thePlayer, targetBox, flags, jitterData, RotationUtils.MinMaxPair(minPlayerPredictSizeValue.get(), maxPlayerPredictSizeValue.get()), distance, hitboxDecrement, searchSensitivity, randomCenterSizeValue.get().toDouble(), distanceOutOfRangeCallback) }

		// Search
		var fallBackRotation: VecRotation? = null
		val rotation = if (!lockValue.get() && RotationUtils.isFaced(theWorld, thePlayer, entity, aimRange.toDouble(), aabbGetter)) Rotation(lastYaw, lastPitch)
		else (searchCenter(if (isAttackRotation) attackRange else aimRange) {
			failedToRotate = true

			// TODO: Make better fallback
			fallBackRotation = searchCenter(aimRange, null)
		} ?: fallBackRotation ?: return false).rotation

		lastYaw = rotation.yaw
		lastPitch = rotation.pitch

		val maxTurnSpeed = maxTurnSpeedValue.get()
		val minTurnSpeed = minTurnSpeedValue.get()

		if (!rotationsValue.get() || maxTurnSpeed <= 0F) return true

		// Limit TurnSpeed
		val turnSpeed = if (minTurnSpeed < 180f) minTurnSpeed + (maxTurnSpeed - minTurnSpeed) * Random.nextFloat() else 180f

		// Acceleration
		val maxAcceleration = maxAccelerationRatioValue.get()
		val minAcceleration = minAccelerationRatioValue.get()
		val acceleration = if (maxAcceleration > 0f) minAcceleration + (maxAcceleration - minAcceleration) * Random.nextFloat() else 0f

		val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, turnSpeed, acceleration)

		lastYaw = limitedRotation.yaw
		lastPitch = limitedRotation.pitch

		if (silentRotationValue.get())
		{
			val maxKeepLength = maxKeepRotationTicksValue.get()
			val keepLength = if (keepRotationValue.get() && maxKeepLength > 0)
			{
				val minKeepLength = minKeepRotationTicksValue.get()
				if (maxKeepLength == minKeepLength) maxKeepLength else minKeepLength + Random.nextInt(maxKeepLength - minKeepLength)
			}
			else 0

			RotationUtils.setTargetRotation(limitedRotation, keepLength)
		}
		else limitedRotation.applyRotationToPlayer(thePlayer)

		val maxResetSpeed = maxResetTurnSpeed.get().coerceAtLeast(20F)
		val minResetSpeed = minResetTurnSpeed.get().coerceAtLeast(20F)
		if (maxResetSpeed < 180) RotationUtils.setNextResetTurnSpeed(minResetSpeed, maxResetSpeed)

		return true
	}

	private val aabbGetter: (IEntity) -> IAxisAlignedBB = {
		var bb = it.entityBoundingBox

		bb = if (backtraceValue.get()) LocationCache.getAABBBeforeNTicks(it.entityId, backtraceTicksValue.get(), bb) else bb

		// Entity movement predict
		if (predictValue.get())
		{
			val xPredict = (it.posX - it.prevPosX) * predict
			val yPredict = (it.posY - it.prevPosY) * predict
			val zPredict = (it.posZ - it.prevPosZ) * predict

			bb = bb.offset(xPredict, yPredict, zPredict)
		}

		bb
	}

	private fun updateComboReach()
	{
		if (target == null || currentTarget == null || !hitable) comboReach = 0f
	}

	/**
	 * Check if enemy is hitable with current rotations
	 */
	private fun updateHitable(theWorld: IWorldClient, thePlayer: IEntityPlayerSP)
	{
		val currentTarget = currentTarget
		val reach = min(maxAttackRange.toDouble(), thePlayer.getDistanceToEntityBox(target ?: return) + 1)

		if (!rotationsValue.get() || maxTurnSpeedValue.get() <= 0F || targetModeValue.get().equals("Multi", ignoreCase = true)) // Disable hitable check if turn speed is zero
		{
			hitable = if (currentTarget != null) thePlayer.getDistanceToEntityBox(currentTarget) <= reach else false
			return
		}

		val aac = aacValue.get()
		val livingRaycast = livingRaycastValue.get()
		val raycastIgnored = raycastIgnoredValue.get()

		if (raycastValue.get())
		{
			val provider = classProvider

			val raycastedEntity = RaycastUtils.raycastEntity(theWorld, thePlayer, reach + 1.0, lastYaw, lastPitch, aabbGetter) { entity -> entity != null && (!livingRaycast || (provider.isEntityLivingBase(entity) && !provider.isEntityArmorStand(entity))) && (EntityUtils.isEnemy(entity, aac) || raycastIgnored || aac && theWorld.getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox).isNotEmpty()) }

			if (raycastedEntity != null && provider.isEntityLivingBase(raycastedEntity) && (LiquidBounce.moduleManager[NoFriends::class.java].state || !provider.isEntityPlayer(raycastedEntity) || !raycastedEntity.asEntityPlayer().isClientFriend())) this.currentTarget = raycastedEntity.asEntityLivingBase()

			hitable = this.currentTarget == raycastedEntity
		}
		else hitable = if (currentTarget != null) RotationUtils.isFaced(theWorld, thePlayer, currentTarget, reach, aabbGetter) else false
	}

	/**
	 * Start blocking
	 */
	private fun startBlocking(thePlayer: IEntityPlayerSP, interactEntity: IEntity?, interact: Boolean)
	{
		val autoBlockMode = autoBlockValue.get()
		val blockRate = blockRate.get()

		// BlockRate check
		if (blockRate <= 0 || Random.nextInt(100) > blockRate) return

		val visual = !autoBlockMode.equals("Off", true) // Fake, Packet, AfterTick
		val packet = visual && !autoBlockMode.equals("Fake", true) // Packet, AfterTick

		if (packet && !serverSideBlockingStatus)
		{
			val provider = classProvider

			val netHandler = mc.netHandler

			// Interact block
			if (interact && interactEntity != null)
			{
				val positionEye = thePlayer.getPositionEyes(1F)

				val expandSize = interactEntity.collisionBorderSize.toDouble()
				val boundingBox = interactEntity.entityBoundingBox.expand(expandSize, expandSize, expandSize)

				val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch)
				val yawRadians = WMathHelper.toRadians(yaw)
				val pitchRadians = WMathHelper.toRadians(pitch)

				val func = functions

				val yawCos = func.cos(-yawRadians - WMathHelper.PI)
				val yawSin = func.sin(-yawRadians - WMathHelper.PI)
				val pitchCos = -func.cos(-pitchRadians)
				val pitchSin = func.sin(-pitchRadians)

				val range = min(maxAttackRange.toDouble(), thePlayer.getDistanceToEntityBox(interactEntity)) + 1
				val lookAt = positionEye.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

				val movingObject = boundingBox.calculateIntercept(positionEye, lookAt)
				if (movingObject != null)
				{
					val hitVec = movingObject.hitVec

					netHandler.addToSendQueue(provider.createCPacketUseEntity(interactEntity, WVec3(hitVec.xCoord - interactEntity.posX, hitVec.yCoord - interactEntity.posY, hitVec.zCoord - interactEntity.posZ)))
					netHandler.addToSendQueue(provider.createCPacketUseEntity(interactEntity, ICPacketUseEntity.WAction.INTERACT))
				}
			}

			netHandler.addToSendQueue(provider.createCPacketPlayerBlockPlacement(WBlockPos(-1, -1, -1), 255, (mc.thePlayer ?: return).inventory.getCurrentItemInHand(), 0.0F, 0.0F, 0.0F))
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
			val provider = classProvider

			mc.netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
			serverSideBlockingStatus = false
		}
		clientSideBlockingStatus = false
	}

	/**
	 * Check if run should be cancelled
	 */
	private fun shouldCancelRun(thePlayer: IEntityPlayerSP): Boolean
	{
		val moduleManager = LiquidBounce.moduleManager

		val shouldDisableOnDeath = thePlayer.spectator || !EntityUtils.isAlive(thePlayer, false)

		if (shouldDisableOnDeath && disableOnDeathValue.get())
		{
			state = false
			LiquidBounce.hud.addNotification("KillAura", "Disabled KillAura due player death", 1000L, Color.red)
		}

		return shouldDisableOnDeath || (suspendWhileConsumingValue.get() && thePlayer.isUsingItem && thePlayer.heldItem == thePlayer.itemInUse && (classProvider.isItemFood(thePlayer.heldItem?.item) || classProvider.isItemPotion(thePlayer.heldItem?.item))) || !suspendTimer.hasTimePassed(suspend) || (moduleManager[Blink::class.java] as Blink).state || moduleManager[FreeCam::class.java].state
	}

	/**
	 * Check if player is able to block
	 */
	private fun getCanBlock(thePlayer: IEntityPlayerSP): Boolean = thePlayer.heldItem != null && classProvider.isItemSword(thePlayer.heldItem?.item) && Backend.MINECRAFT_VERSION_MINOR == 8

	/**
	 * Range
	 */
	private val maxAttackRange: Float
		get() = max(attackRange, throughWallsRangeValue.get()) + comboReach

	private val maxTargetRange: Float
		get() = max(aimRange, max(maxAttackRange, if (fakeSwingValue.get()) swingRange else 0f))

	private fun getAttackRange(thePlayer: IEntityPlayerSP, entity: IEntity): Float
	{
		val throughWallsRange = throughWallsRangeValue.get()
		return (if (thePlayer.getDistanceToEntityBox(entity) >= throughWallsRange) attackRange else throughWallsRange) - if (thePlayer.sprinting) rangeSprintReducementValue.get() else 0F + comboReach
	}

	/**
	 * HUD Tag
	 */
	override val tag: String
		get() = "${targetModeValue.get()}, ${DECIMALFORMAT_1.format(maxTargetRange)}, ${DECIMALFORMAT_1.format(maxAttackRange)}"

	val hasTarget: Boolean
		get() = state && target != null

	fun suspend(time: Long)
	{
		suspend = time
		suspendTimer.reset()
	}
}
