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
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
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
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_6
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// TODO: Visually start-stop blocking like as Xave
@ModuleInfo(name = "KillAura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, defaultKeyBinds = [Keyboard.KEY_R])
class KillAura : Module()
{
	/**
	 * OPTIONS
	 */

	private val cpsGroup = ValueGroup("CPS")
	private val maxCPS: IntegerValue = object : IntegerValue("Max", 8, 1, 20, "MaxCPS")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minCPS.get()
			if (i > newValue) set(i)

			attackDelay = TimeUtils.randomClickDelay(minCPS.get(), this.get())
		}
	}

	private val minCPS: IntegerValue = object : IntegerValue("Min", 5, 1, 20, "MinCPS")
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

	private val rangeGroup = ValueGroup("Range")
	private val attackRangeValue = object : FloatValue("Attack", 3.7f, 1f, 8f, "Range")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = swingRangeValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val throughWallsAttackRangeValue = FloatValue("ThroughWallsAttack", 3f, 0f, 8f, "ThroughWallsRange")
	private val aimRangeValue: FloatValue = FloatValue("Aim", 6f, 1f, 12f, "AimRange")
	private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f, "RangeSprintReducement")

	private val swingGroup = ValueGroup("Swing")
	private val swingValue = BoolValue("Enabled", true, "Swing")
	private val fakeSwingValue = BoolValue("FakeSwing", true, "FakeSwing")
	private val swingRangeValue: FloatValue = object : FloatValue("Range", 6f, 1f, 12f, "SwingRange")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = attackRangeValue.get()
			if (i > newValue) this.set(i)

			val i2 = aimRangeValue.get()
			if (i2 < newValue) this.set(i2)
		}
	}

	private val comboReachGroup = ValueGroup("ComboReach")
	private val comboReachValue = BoolValue("Enabled", false, "ComboReach")
	private val comboReachIncrementValue = FloatValue("Increment", 0.1F, 0.02F, 0.5F, "ComboReachIncrement")
	private val comboReachMaxValue = FloatValue("Limit", 0.5F, 0.02F, 3F, "ComboReachMax")

	private val targetGroup = ValueGroup("Target")
	private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection", "LivingTime"), "Distance")
	private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
	private val limitedMultiTargetsValue = object: IntegerValue("LimitedMultiTargets", 0, 0, 50, "LimitedMultiTargets")
	{
		override fun showCondition() = targetModeValue.get().equals("Multi", ignoreCase = true)
	}

	private val switchDelayGroup = object : ValueGroup("SwitchDelay")
	{
		override fun showCondition(): Boolean = targetModeValue.get().equals("Switch", ignoreCase = true)
	}
	private val maxSwitchDelayValue: IntegerValue = object : IntegerValue("Max", 0, 0, 1000, "MaxSwitchDelay")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minSwitchDelayValue.get()
			if (i > newValue) set(i)

			switchDelay = TimeUtils.randomDelay(minSwitchDelayValue.get(), this.get())
		}
	}

	private val minSwitchDelayValue: IntegerValue = object : IntegerValue("Min", 0, 0, 1000, "MinSwitchDelay")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxSwitchDelayValue.get()
			if (i < newValue) set(i)

			switchDelay = TimeUtils.randomDelay(this.get(), maxSwitchDelayValue.get())
		}
	}

	private val autoBlockGroup = ValueGroup("AutoBlock")
	private val autoBlockValue = ListValue("Mode", arrayOf("Off", "Fake", "Packet", "AfterTick"), "Packet")
	private val autoBlockRangeValue: FloatValue = FloatValue("Range", 6f, 1f, 12f)
	private val autoBlockRate = IntegerValue("Rate", 100, 1, 100)
	private val autoBlockHitableCheckValue = BoolValue("HitableCheck", false)
	private val autoBlockHurtTimeCheckValue = BoolValue("HurtTimeCheck", true)
	private val autoBlockWallCheckValue = BoolValue("WallCheck", false)

	private val interactAutoBlockGroup = ValueGroup("Interact")
	private val interactAutoBlockValue = BoolValue("Enabled", false, "InteractAutoBlock")
	private val interactAutoBlockRangeValue: FloatValue = FloatValue("Range", 3f, 1f, 8f)

	private val rayCastGroup = ValueGroup("RayCast")
	private val raycastValue = BoolValue("Enabled", true, "RayCast")
	private val raycastIgnoredValue = BoolValue("Ignored", false, "RayCastIgnored")
	private val livingRaycastValue = BoolValue("Living", true, "LivingRayCast")

	private val bypassGroup = ValueGroup("Bypass")
	private val keepSprintValue = BoolValue("KeepSprint", true, "KeepSprint")
	private val aacValue = BoolValue("AAC", false, "AAC")
	private val failRateValue = FloatValue("FailRate", 0f, 0f, 100f, "FailRate")
	private val suspendWhileConsumingValue = BoolValue("SuspendWhileConsuming", true, "SuspendWhileConsuming")

	private val noInventoryGroup = ValueGroup("NoInvAttack")
	private val noInventoryAttackValue = BoolValue("Enabled", false, "NoInvAttack")
	private val noInventoryDelayValue = IntegerValue("Delay", 200, 0, 500, "NoInvDelay")

	private val rotationGroup = ValueGroup("Rotation")
	private val rotationMode = ListValue("Mode", arrayOf("Off", "SearchCenter", "LockCenter", "RandomCenter", "Outborder"), "SearchCenter", "Rotation")
	private val rotationLockValue = BoolValue("Lock", true, "Rotation-Lock")
	private val silentRotationValue = BoolValue("Silent", true, "SilentRotation")
	private val randomCenterSizeValue = object : FloatValue("RandomCenterSize", 0.8F, 0.1F, 1.0F, "Rotation-RandomCenter-RandomSize")
	{
		override fun showCondition() = rotationMode.get().equals("RandomCenter", ignoreCase = true)
	}
	private val searchCenterGroup = object : ValueGroup("SearchCenter")
	{
		override fun showCondition() = rotationMode.get().equals("SearchCenter", ignoreCase = true)
	}
	private val searchCenterHitboxShrinkValue = FloatValue("Shrink", 0.15f, 0f, 0.3f, "Rotation-SearchCenter-HitboxShrink")
	private val searchCenterSensitivityValue = IntegerValue("Steps", 7, 4, 20, "Rotation-SearchCenter-Steps")

	private val jitterGroup = ValueGroup("Jitter")
	private val jitterValue = BoolValue("Enabled", false, "Jitter")
	private val jitterYawRate = IntegerValue("YawRate", 50, 0, 100, "YawJitterRate")
	private val jitterPitchRate = IntegerValue("PitchRate", 50, 0, 100, "PitchJitterRate")
	private val maxYawJitterStrengthValue: FloatValue = object : FloatValue("MaxYawStrength", 1f, 0f, 5f, "MaxPitchJitterStrength")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = minYawJitterStrengthValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minYawJitterStrengthValue: FloatValue = object : FloatValue("MinYawStrength", 0f, 0f, 5f, "MinYawJitterStrength")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = maxYawJitterStrengthValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val maxPitchJitterStrengthValue: FloatValue = object : FloatValue("MaxPitchStrength", 1f, 0f, 5f, "MaxYawJitterStrength")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = minPitchJitterStrengthValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minPitchJitterStrengthValue: FloatValue = object : FloatValue("MinPitchStrength", 0f, 0f, 5f, "MinPitchJitterStrength")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = maxPitchJitterStrengthValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val keepRotationGroup = ValueGroup("KeepRotation")
	private val keepRotationValue = BoolValue("Enabled", false, "KeepRotation")
	private val minKeepRotationTicksValue: IntegerValue = object : IntegerValue("MinTicks", 20, 0, 50, "MinKeepRotationTicks")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxKeepRotationTicksValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val maxKeepRotationTicksValue: IntegerValue = object : IntegerValue("MaxTicks", 30, 0, 50, "MaxKeepRotationTicks")
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minKeepRotationTicksValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val accelerationGroup = ValueGroup("Acceleration")
	private val maxAccelerationRatioValue: FloatValue = object : FloatValue("Max", 0f, 0f, .99f, "MaxAccelerationRatio")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minAccelerationRatioValue.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minAccelerationRatioValue: FloatValue = object : FloatValue("Min", 0f, 0f, .99f, "MinAccelerationRatio")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxAccelerationRatioValue.get()
			if (v < newValue) this.set(v)
		}
	}

	private val turnSpeedGroup = ValueGroup("TurnSpeed")
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("Max", 180f, 0f, 180f, "MaxTurnSpeed")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("Min", 180f, 0f, 180f, "MinTurnSpeed")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) set(v)
		}
	}

	private val resetSpeedGroup = ValueGroup("RotationResetSpeed")
	private val maxResetTurnSpeed: FloatValue = object : FloatValue("Max", 180f, 20f, 180f, "MaxRotationResetSpeed")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minResetTurnSpeed.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minResetTurnSpeed: FloatValue = object : FloatValue("Min", 180f, 20f, 180f, "MinRotationResetSpeed")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxResetTurnSpeed.get()
			if (v < newValue) this.set(v)
		}
	}

	private val rotationStrafeGroup = ValueGroup("Strafe")
	private val rotationStrafeValue = ListValue("Mode", arrayOf("Off", "Strict", "Silent"), "Off", "Strafe")
	private val rotationStrafeOnlyGroundValue = BoolValue("OnlyGround", false, "StrafeOnlyGround")

	private val predictGroup = ValueGroup("Predict")
	private val predictEnemyGroup = ValueGroup("Enemy")
	private val predictEnemyValue = BoolValue("Enabled", true, "Predict")
	private val maxEnemyPredictSizeValue: FloatValue = object : FloatValue("Max", 1f, -2f, 2f, "MaxPredictSize")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minEnemyPredictSizeValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minEnemyPredictSizeValue: FloatValue = object : FloatValue("Min", 1f, -2f, 2f, "MinPredictSize")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxEnemyPredictSizeValue.get()
			if (v < newValue) set(v)
		}
	}

	private val predictPlayerGroup = ValueGroup("Player")
	private val playerPredictValue = BoolValue("Enabled", true, "PlayerPredict")
	private val maxPlayerPredictSizeValue: FloatValue = object : FloatValue("Max", 1f, -2f, 2f, "MaxPlayerPredictSize")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minPlayerPredictSizeValue.get()
			if (v > newValue) set(v)
		}
	}
	private val minPlayerPredictSizeValue: FloatValue = object : FloatValue("Min", 1f, -2f, 2f, "MinPlayerPredictSize")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxPlayerPredictSizeValue.get()
			if (v < newValue) set(v)
		}
	}

	private val backtrackGroup = ValueGroup("Backtrack")
	private val backtrackValue = BoolValue("Enabled", false, "Backtrace")
	private val backtrackTicksValue: IntegerValue = IntegerValue("Ticks", 3, 1, 6, "BacktraceTicks")

	private val fovGroup = ValueGroup("FoV")
	private val fovModeValue = ListValue("Type", arrayOf("ServerRotation", "ClientRotation"), "ClientRotation", "FovMode")
	private val fovValue = FloatValue("FoV", 180f, 0f, 180f, "FoV")

	private val visualGroup = ValueGroup("Visual")
	private val fakeSharpValue = BoolValue("FakeSharp", true, "FakeSharp")
	private val particles = IntegerValue("Particles", 1, 0, 10, "Particles")

	private val markGroup = ValueGroup("Mark")
	private val markValue = ListValue("Target", arrayOf("None", "Platform", "Box"), "Platform", "Mark")
	private val markRangeValue = ListValue("Range", arrayOf("None", "AttackRange", "ExceptBlockRange", "All"), "AttackRange", "Mark-Range")
	private val markRangeAccuracyValue = FloatValue("Range-Accuracy", 10F, 0.5F, 20F, "Mark-Range-Accuracy")

	private val disableOnDeathValue = BoolValue("DisableOnDeath", true, "DisableOnDeath")

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
	private var interactBlockRange = 0f

	private var comboReach = 0f

	private var lastYaw = 0f
	private var lastPitch = 0f

	private var predictX = 1F
	private var predictY = 1F
	private var predictZ = 1F

	// Container Delay
	private var containerOpen = -1L

	// Server-side block status
	var serverSideBlockingStatus: Boolean = false

	// Client-side(= visual) block status
	var clientSideBlockingStatus: Boolean = false

	private var switchDelay = TimeUtils.randomDelay(minSwitchDelayValue.get(), maxSwitchDelayValue.get())
	private val switchDelayTimer = MSTimer()

	var debug: String? = null

	/**
	 * Did last attack failed
	 */
	private var failedToHit = false

	private var failedToRotate = false

	/**
	 * Target of auto-block
	 */
	private var autoBlockTarget: IEntityLivingBase? = null
	private var rangeMarks: List<Pair<Float, Int>>? = null

	// HARDCODED RANGE COLORS //

	private val attackRangeColor = ColorUtils.createRGB(0, 255, 0)
	private val throughWallsColor = ColorUtils.createRGB(200, 128, 0)
	private val aimRangeColor = ColorUtils.createRGB(255, 0, 0)
	private val swingRangeColor = ColorUtils.createRGB(0, 0, 255)
	private val blockRangeColor = ColorUtils.createRGB(255, 0, 255)
	private val interactBlockRangeColor = ColorUtils.createRGB(255, 64, 255)

	// HARDCODED RANGE COLORS //

	init
	{
		cpsGroup.addAll(maxCPS, minCPS)
		comboReachGroup.addAll(comboReachValue, comboReachIncrementValue, comboReachMaxValue)
		rangeGroup.addAll(attackRangeValue, throughWallsAttackRangeValue, aimRangeValue, rangeSprintReducementValue, comboReachGroup)
		swingGroup.addAll(swingValue, fakeSwingValue, swingRangeValue)
		switchDelayGroup.addAll(maxSwitchDelayValue, minSwitchDelayValue)
		targetGroup.addAll(priorityValue, targetModeValue, limitedMultiTargetsValue)
		interactAutoBlockGroup.addAll(interactAutoBlockValue, interactAutoBlockRangeValue)
		autoBlockGroup.addAll(autoBlockValue, autoBlockRangeValue, autoBlockRate, autoBlockHitableCheckValue, autoBlockHurtTimeCheckValue, autoBlockWallCheckValue, interactAutoBlockGroup)
		rayCastGroup.addAll(raycastValue, raycastIgnoredValue, livingRaycastValue)
		bypassGroup.addAll(keepSprintValue, aacValue, failRateValue, suspendWhileConsumingValue, noInventoryGroup, raycastValue)
		noInventoryGroup.addAll(noInventoryAttackValue, noInventoryDelayValue)
		searchCenterGroup.addAll(searchCenterHitboxShrinkValue, searchCenterSensitivityValue)
		jitterGroup.addAll(jitterValue, jitterYawRate, jitterPitchRate, maxYawJitterStrengthValue, minYawJitterStrengthValue, maxPitchJitterStrengthValue, minPitchJitterStrengthValue)
		keepRotationGroup.addAll(keepRotationValue, minKeepRotationTicksValue, maxKeepRotationTicksValue)
		accelerationGroup.addAll(maxAccelerationRatioValue, minAccelerationRatioValue)
		turnSpeedGroup.addAll(maxTurnSpeedValue, minTurnSpeedValue)
		resetSpeedGroup.addAll(maxResetTurnSpeed, minResetTurnSpeed)
		rotationStrafeGroup.addAll(rotationStrafeValue, rotationStrafeOnlyGroundValue)
		predictEnemyGroup.addAll(predictEnemyValue, maxEnemyPredictSizeValue, minEnemyPredictSizeValue)
		predictPlayerGroup.addAll(playerPredictValue, maxPlayerPredictSizeValue, minPlayerPredictSizeValue)
		predictGroup.addAll(predictEnemyGroup, predictPlayerGroup)
		backtrackGroup.addAll(backtrackValue, backtrackTicksValue)
		rotationGroup.addAll(rotationMode, rotationLockValue, silentRotationValue, randomCenterSizeValue, searchCenterGroup, jitterGroup, keepRotationGroup, accelerationGroup, turnSpeedGroup, resetSpeedGroup, rotationStrafeGroup, predictGroup, backtrackGroup)
		fovGroup.addAll(fovModeValue, fovValue)
		markGroup.addAll(markValue, markRangeValue, markRangeAccuracyValue)
		visualGroup.addAll(fakeSharpValue, particles, markGroup)

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

		if (currentTarget != null && RotationUtils.targetRotation != null && (thePlayer.onGround || !rotationStrafeOnlyGroundValue.get()))
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

	fun update(theWorld: IWorld, thePlayer: IEntityPlayer)
	{
		// CancelRun & NoInventory
		if (shouldCancelRun(thePlayer) || (noInventoryAttackValue.get() && (classProvider.isGuiContainer(mc.currentScreen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))) return

		// Update target
		updateTarget(theWorld, thePlayer)

		// Pre-AutoBlock
		if (autoBlockTarget != null && !autoBlockValue.get().equals("AfterTick", ignoreCase = true) && getCanBlock(thePlayer) && (!autoBlockHitableCheckValue.get() || hitable)) startBlocking(thePlayer, autoBlockTarget, interactAutoBlockValue.get())
		else if (getCanBlock(thePlayer)) stopBlocking()

		// Target
		currentTarget = target ?: return
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
		val throughWallsRange = throughWallsAttackRangeValue.get()
		aimRange = aimRangeValue.get()
		swingRange = swingRangeValue.get()
		blockRange = autoBlockRangeValue.get()
		interactBlockRange = interactAutoBlockRangeValue.get()

		val markRangeMode = markRangeValue.get().toLowerCase()
		if (markRangeMode != "none")
		{
			val arr = arrayOfNulls<Pair<Float, Int>?>(6)
			arr[0] = attackRange to attackRangeColor
			if (throughWallsRange > 0) arr[1] = throughWallsRange to throughWallsColor
			arr[2] = aimRange to aimRangeColor
			if (fakeSwingValue.get()) arr[3] = swingRange to swingRangeColor
			if (!autoBlockValue.get().equals("Off", ignoreCase = true))
			{
				arr[4] = blockRange to blockRangeColor
				if (interactAutoBlockValue.get()) arr[5] = interactBlockRange to interactBlockRangeColor
			}

			rangeMarks = arr.take(when (markRangeMode)
			{
				"attackrange" -> 2
				"exceptblockrange" -> 4
				else -> 6
			}).filterNotNull()
		}

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

		if (!markRangeValue.get().equals("None", ignoreCase = true))
		{
			val accuracy = markRangeAccuracyValue.get()

			rangeMarks?.forEach {
				GL11.glPushMatrix()
				RenderUtils.drawRadius(it.first, accuracy, it.second)
				GL11.glPopMatrix()
			}
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

			var targetBB = target.entityBoundingBox
			val partialTicks = event.partialTicks

			targetBB = if (backtrackValue.get())
			{
				val backtraceTicks = backtrackTicksValue.get()

				val bb = LocationCache.getAABBBeforeNTicks(targetEntityId, backtraceTicks, targetBB)
				val lastBB = LocationCache.getAABBBeforeNTicks(targetEntityId, backtraceTicks + 1, targetBB)

				classProvider.createAxisAlignedBB(lastBB.minX + (bb.minX - lastBB.minX) * partialTicks, lastBB.minY + (bb.minY - lastBB.minY) * partialTicks, lastBB.minZ + (bb.minZ - lastBB.minZ) * partialTicks, lastBB.maxX + (bb.maxX - lastBB.maxX) * partialTicks, lastBB.maxY + (bb.maxY - lastBB.maxY) * partialTicks, lastBB.maxZ + (bb.maxZ - lastBB.maxZ) * partialTicks).offset(-renderPosX, -renderPosY, -renderPosZ)
			}
			else
			{
				val posX = target.posX
				val posY = target.posY
				val posZ = target.posZ

				val lastTickPosX = target.lastTickPosX
				val lastTickPosY = target.lastTickPosY
				val lastTickPosZ = target.lastTickPosZ

				val x = lastTickPosX + (posX - lastTickPosX) * partialTicks - renderPosX
				val y = lastTickPosY + (posY - lastTickPosY) * partialTicks - renderPosY
				val z = lastTickPosZ + (posZ - lastTickPosZ) * partialTicks - renderPosZ

				targetBB.offset(-posX, -posY, -posZ).offset(x, y, z)
			}

			// Entity movement predict
			if (predictEnemyValue.get())
			{
				val xPredict = (target.posX - target.lastTickPosX) * predictX
				val yPredict = (target.posY - target.lastTickPosY) * predictY
				val zPredict = (target.posZ - target.lastTickPosZ) * predictZ

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
	private fun updateTarget(theWorld: IWorld, thePlayer: IEntityPlayer)
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
			if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(provider.getPotionEnum(PotionType.BLINDNESS).id) && thePlayer.ridingEntity == null || criticals.state && criticals.canCritical(thePlayer)) thePlayer.onCriticalHit(target)

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
	private fun updateRotations(theWorld: IWorld, thePlayer: IEntityPlayer, entity: IEntity, isAttackRotation: Boolean): Boolean
	{
		if (predictEnemyValue.get())
		{
			predictX = RandomUtils.nextFloat(minEnemyPredictSizeValue.get(), maxEnemyPredictSizeValue.get())
			predictY = RandomUtils.nextFloat(minEnemyPredictSizeValue.get(), maxEnemyPredictSizeValue.get())
			predictZ = RandomUtils.nextFloat(minEnemyPredictSizeValue.get(), maxEnemyPredictSizeValue.get())
		}

		val targetBox = getHitbox(entity)

		val jitter = jitterValue.get()

		// Jitter
		val jitterData = if (jitter) RotationUtils.JitterData(jitterYawRate.get(), jitterPitchRate.get(), minYawJitterStrengthValue.get(), maxYawJitterStrengthValue.get(), minPitchJitterStrengthValue.get(), maxPitchJitterStrengthValue.get()) else null

		var flags = 0

		val rotationMode = rotationMode.get()

		// Apply rotation mode to flags
		flags = flags or when (rotationMode.toLowerCase())
		{
			"lockcenter" -> RotationUtils.LOCK_CENTER
			"outborder" -> if (!attackTimer.hasTimePassed(attackDelay shr 1)) RotationUtils.OUT_BORDER else RotationUtils.RANDOM_CENTER
			"randomcenter" -> RotationUtils.RANDOM_CENTER
			else -> 0
		}

		if (jitter && (thePlayer.getDistanceToEntityBox(entity) <= max(maxAttackRange, if (fakeSwingValue.get()) swingRange else Float.MIN_VALUE))) flags = flags or RotationUtils.JITTER
		if (playerPredictValue.get()) flags = flags or RotationUtils.PLAYER_PREDICT
		if (thePlayer.getDistanceToEntityBox(entity) <= throughWallsAttackRangeValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK

		failedToRotate = false

		val searchCenter = { distance: Float, distanceOutOfRangeCallback: (() -> Unit)? -> RotationUtils.searchCenter(theWorld, thePlayer, targetBox, flags, jitterData, RotationUtils.MinMaxPair(minPlayerPredictSizeValue.get(), maxPlayerPredictSizeValue.get()), distance, searchCenterHitboxShrinkValue.get().toDouble(), searchCenterSensitivityValue.get(), randomCenterSizeValue.get().toDouble(), distanceOutOfRangeCallback) }

		// Search
		var fallBackRotation: VecRotation? = null
		val rotation = if (!rotationLockValue.get() && RotationUtils.isFaced(theWorld, thePlayer, entity, aimRange.toDouble(), getHitbox)) Rotation(lastYaw, lastPitch)
		else (searchCenter(if (isAttackRotation) attackRange else aimRange) {
			// Because of '.getDistanceToEntityBox()' is not perfect. (searchCenter() >>> 넘사벽 >>> getDistanceToEntityBox())
			failedToRotate = true

			// TODO: Make better fallback
			fallBackRotation = searchCenter(aimRange, null)
		} ?: fallBackRotation ?: return false).rotation

		lastYaw = rotation.yaw
		lastPitch = rotation.pitch

		val maxTurnSpeed = maxTurnSpeedValue.get()
		val minTurnSpeed = minTurnSpeedValue.get()

		if (rotationMode.equals("Off", ignoreCase = true) || maxTurnSpeed <= 0F) return true

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

	private val getHitbox: (IEntity) -> IAxisAlignedBB = {
		var bb = it.entityBoundingBox

		// Backtrace
		if (backtrackValue.get()) bb = LocationCache.getAABBBeforeNTicks(it.entityId, backtrackTicksValue.get(), bb)

		// Entity movement predict
		if (predictEnemyValue.get()) bb = bb.offset((it.posX - it.lastTickPosX) * predictX, (it.posY - it.lastTickPosY) * predictY, (it.posZ - it.lastTickPosZ) * predictZ)

		bb
	}

	private fun updateComboReach()
	{
		if (target == null || currentTarget == null || !hitable) comboReach = 0f
	}

	/**
	 * Check if enemy is hitable with current rotations
	 */
	private fun updateHitable(theWorld: IWorld, thePlayer: IEntity)
	{
		val currentTarget = currentTarget
		val reach = min(maxAttackRange.toDouble(), thePlayer.getDistanceToEntityBox(target ?: return) + 1)

		if (rotationMode.get().equals("Off", ignoreCase = true) || maxTurnSpeedValue.get() <= 0F || targetModeValue.get().equals("Multi", ignoreCase = true)) // Disable hitable check if turn speed is zero
		{
			hitable = currentTarget != null && thePlayer.getDistanceToEntityBox(currentTarget) <= reach
			return
		}

		val aac = aacValue.get()
		val livingRaycast = livingRaycastValue.get()
		val raycastIgnored = raycastIgnoredValue.get()

		if (raycastValue.get())
		{
			val provider = classProvider

			val distanceToTarget = currentTarget?.let(thePlayer::getDistanceToEntityBox)
			val raycastedEntity = RaycastUtils.raycastEntity(theWorld, thePlayer, reach + 1.0, lastYaw, lastPitch, getHitbox) { entity -> entity != null && (!livingRaycast || (provider.isEntityLivingBase(entity) && !provider.isEntityArmorStand(entity))) && (raycastIgnored || EntityUtils.isEnemy(entity, aac) || aac && theWorld.getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox).isNotEmpty()) }
			val distanceToRaycasted = raycastedEntity?.let(thePlayer::getDistanceToEntityBox)

			if (raycastedEntity != null && provider.isEntityLivingBase(raycastedEntity) && (LiquidBounce.moduleManager[NoFriends::class.java].state || !provider.isEntityPlayer(raycastedEntity) || !raycastedEntity.asEntityPlayer().isClientFriend())) this.currentTarget = raycastedEntity.asEntityLivingBase()

			if (distanceToTarget != null && distanceToRaycasted != null && currentTarget != raycastedEntity) debug = "[n: ${currentTarget.name}, id: ${currentTarget.entityId}] -> [n: ${raycastedEntity.name}, id: ${raycastedEntity.entityId}] (d_delta: ${DECIMALFORMAT_6.format(distanceToTarget - distanceToRaycasted)})"

			hitable = (distanceToTarget == null || distanceToTarget <= reach) && (distanceToRaycasted == null || distanceToRaycasted <= reach) && this.currentTarget == raycastedEntity
		}
		else hitable = if (currentTarget != null) RotationUtils.isFaced(theWorld, thePlayer, currentTarget, reach, getHitbox) else false
	}

	/**
	 * Start blocking
	 */
	private fun startBlocking(thePlayer: IEntity, interactEntity: IEntity?, interact: Boolean)
	{
		val autoBlockMode = autoBlockValue.get()
		val blockRate = autoBlockRate.get()

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

				val (yaw, pitch) = RotationUtils.targetRotation ?: RotationUtils.clientRotation
				val yawRadians = WMathHelper.toRadians(yaw)
				val pitchRadians = WMathHelper.toRadians(pitch)

				val func = functions

				val yawCos = func.cos(-yawRadians - WMathHelper.PI)
				val yawSin = func.sin(-yawRadians - WMathHelper.PI)
				val pitchCos = -func.cos(-pitchRadians)
				val pitchSin = func.sin(-pitchRadians)

				val range = min(interactBlockRange.toDouble(), thePlayer.getDistanceToEntityBox(interactEntity)) + 1
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
	private fun shouldCancelRun(thePlayer: IEntityPlayer): Boolean
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
	private fun getCanBlock(thePlayer: IEntityPlayer): Boolean = thePlayer.heldItem != null && classProvider.isItemSword(thePlayer.heldItem?.item) && Backend.MINECRAFT_VERSION_MINOR == 8

	/**
	 * Range
	 */
	private val maxAttackRange: Float
		get() = max(attackRange, throughWallsAttackRangeValue.get()) + comboReach

	private val maxTargetRange: Float
		get() = max(aimRange, max(maxAttackRange, if (fakeSwingValue.get()) swingRange else 0f))

	private fun getAttackRange(thePlayer: IEntity, entity: IEntity): Float
	{
		val throughWallsRange = throughWallsAttackRangeValue.get()
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
