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
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_6
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOutCubic
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// TODO: Asynchronously start-stop blocking as Xave
// TODO: BlockCPS option
@ModuleInfo(name = "KillAura", description = "Automatically attacks targets around you.", category = ModuleCategory.COMBAT, defaultKeyBinds = [Keyboard.KEY_R])
class KillAura : Module()
{
	/**
	 * OPTIONS
	 */

	private val cpsValue: IntegerRangeValue = object : IntegerRangeValue("CPS", 5, 8, 1, 20, "MaxCPS" to "MinCPS", "Number of attack tries per a second")
	{
		override fun onMaxValueChanged(oldValue: Int, newValue: Int)
		{
			attackDelay = TimeUtils.randomClickDelay(getMin(), newValue)
		}

		override fun onMinValueChanged(oldValue: Int, newValue: Int)
		{
			attackDelay = TimeUtils.randomClickDelay(newValue, getMax())
		}
	}

	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
	private val cooldownValue = FloatValue("Cooldown", 1f, 0f, 1f)

	private val rangeGroup = ValueGroup("Range")
	private val rangeAttackGroup = ValueGroup("Attack")
	private val rangeAttackOnGroundValue = object : FloatValue("OnGround", 3.7f, 1f, 8f, "Range")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = swingRangeValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val rangeAttackOffGroundValue = object : FloatValue("OffGround", 3.7f, 1f, 8f, "Range")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = swingRangeValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val rangeThroughWallsAttackValue = FloatValue("ThroughWallsAttack", 3f, 0f, 8f, "ThroughWallsRange")
	private val rangeAimValue: FloatValue = FloatValue("Aim", 6f, 1f, 12f, "AimRange")
	private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f, "RangeSprintReducement")

	private val swingGroup = ValueGroup("Swing")
	private val swingEnabledValue = BoolValue("Enabled", true, "Swing")
	private val swingFakeSwingValue = BoolValue("FakeSwing", true, "FakeSwing")
	private val swingRangeValue: FloatValue = object : FloatValue("Range", 6f, 1f, 12f, "SwingRange")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = min(rangeAttackOnGroundValue.get(), rangeAttackOffGroundValue.get())
			if (i > newValue) this.set(i)

			val i2 = rangeAimValue.get()
			if (i2 < newValue) this.set(i2)
		}
	}

	private val comboReachGroup = ValueGroup("ComboReach")
	private val comboReachEnabledValue = BoolValue("Enabled", false, "ComboReach")
	private val comboReachIncrementValue = FloatValue("Increment", 0.1F, 0.02F, 0.5F, "ComboReachIncrement")
	private val comboReachLimitValue = FloatValue("Limit", 0.5F, 0.02F, 3F, "ComboReachMax")

	private val targetGroup = ValueGroup("Target")
	private val targetPriorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection", "LivingTime"), "Distance", "Priority")
	private val targetModeValue = ListValue("Mode", arrayOf("Single", "Switch", "Multi"), "Switch", "TargetMode")
	private val targetLimitedMultiTargetsValue = object : IntegerValue("LimitedMultiTargets", 0, 0, 50, "LimitedMultiTargets")
	{
		override fun showCondition() = targetModeValue.get().equals("Multi", ignoreCase = true)
	}

	private val switchDelayValue: IntegerRangeValue = object : IntegerRangeValue("SwitchDelay", 0, 0, 0, 1000, "MaxSwitchDelay" to "MinSwitchDelay")
	{
		override fun onMaxValueChanged(oldValue: Int, newValue: Int)
		{
			switchDelay = TimeUtils.randomDelay(getMin(), newValue)
		}

		override fun onMinValueChanged(oldValue: Int, newValue: Int)
		{
			switchDelay = TimeUtils.randomDelay(newValue, getMax())
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
	private val interactAutoBlockEnabledValue = BoolValue("Enabled", false, "InteractAutoBlock")
	private val interactAutoBlockRangeValue: FloatValue = FloatValue("Range", 3f, 1f, 8f)

	private val rayCastGroup = ValueGroup("RayCast")
	private val rayCastEnabledValue = BoolValue("Enabled", true, "RayCast")
	private val rayCastSkipEnemyCheckValue = BoolValue("SkipEnemyCheck", false, "RayCastIgnored", description = "Disable the enemy checks in raycast filter")
	private val rayCastLivingOnlyValue = BoolValue("LivingOnly", true, "LivingRayCast", description = "Only include living entities; drop otherwise")
	private val rayCastIncludeCollidedValue = BoolValue("IncludeCollided", true, "AAC", description = "Include entities which were collided with the target")

	private val bypassGroup = ValueGroup("Bypass")
	private val bypassKeepSprintValue = BoolValue("KeepSprint", true, "KeepSprint", description = "Don't cancel sprinting while attacking enemy")
	private val bypassAACValue = BoolValue("AAC", false, "AAC", description = "Bypass several anti-cheats such as AAC")
	private val bypassFailRateValue = FloatValue("FailRate", 0f, 0f, 100f, "FailRate")
	private val bypassSuspendWhileConsumingValue = BoolValue("SuspendWhileConsuming", true, "SuspendWhileConsuming", description = "Suspend KillAura if you're consuming something")

	private val noInventoryGroup = ValueGroup("NoInvAttack")
	private val noInventoryAttackEnabledValue = BoolValue("Enabled", false, "NoInvAttack", description = "Suspend KillAura if your inventory is open")
	private val noInventoryDelayValue = IntegerValue("Delay", 200, 0, 500, "NoInvDelay", description = "Time between inventory close and resume of KillAura")

	private val rotationGroup = ValueGroup("Rotation")
	private val rotationMode = ListValue("Mode", arrayOf("Off", "SearchCenter", "LockCenter", "RandomCenter", "Outborder"), "SearchCenter", "Rotation")
	private val rotationLockValue = BoolValue("Lock", true, "Rotation-Lock")
	private val rotationLockExpandRangeValue = object : FloatValue("FacedCheckBoxExpand", 0.0f, 0.0F, 2.0F)
	{
		override fun showCondition() = !rotationLockValue.get()
	}
	private val rotationSilentValue = BoolValue("Silent", true, "SilentRotation")
	private val rotationRandomCenterSizeValue = object : FloatValue("RandomCenterSize", 0.8F, 0.1F, 1.0F, "Rotation-RandomCenter-RandomSize")
	{
		override fun showCondition() = rotationMode.get().equals("RandomCenter", ignoreCase = true)
	}
	private val rotationSearchCenterGroup = object : ValueGroup("SearchCenter")
	{
		override fun showCondition() = rotationMode.get().equals("SearchCenter", ignoreCase = true)
	}
	private val rotationSearchCenterHitboxShrinkValue = FloatValue("Shrink", 0.15f, 0f, 0.3f, "Rotation-SearchCenter-HitboxShrink", description = "Shrinkage of the enemy hitbox when rotation calculation")
	private val rotationSearchCenterSensitivityValue = IntegerValue("Steps", 7, 4, 20, "Rotation-SearchCenter-Steps", description = "Steps of rotation calculation")

	private val rotationJitterGroup = ValueGroup("Jitter")
	private val rotationJitterEnabledValue = BoolValue("Enabled", false, "Jitter")
	private val rotationJitterYawRate = IntegerValue("YawRate", 50, 0, 100, "YawJitterRate")
	private val rotationJitterPitchRate = IntegerValue("PitchRate", 50, 0, 100, "PitchJitterRate")
	private val rotationJitterYawIntensityValue = FloatRangeValue("YawIntensity", 0f, 1f, 0f, 5f, "MaxYawJitterStrength" to "MinYawJitterStrength")
	private val rotationJitterPitchIntensityValue = FloatRangeValue("PitchIntensity", 0f, 1f, 0f, 5f, "MaxPitchJitterStrength" to "MinPitchJitterStrength")

	private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
	private val rotationKeepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
	private val rotationKeepRotationTicks = IntegerRangeValue("Ticks", 20, 30, 0, 60, "MaxKeepRotationTicks" to "MinKeepRotationTicks")

	private val rotationLockAfterTeleportGroup = ValueGroup("LockAfterTeleport")
	private val rotationLockAfterTeleportEnabledValue = BoolValue("Enabled", false)
	private val rotationLockAfterTeleportDelayValue = IntegerRangeValue("Delay", 100, 100, 0, 500)

	private val rotationAccelerationRatioValue = FloatRangeValue("Acceleration", 0f, 0f, 0f, .99f, "MaxAccelerationRatio" to "MinAccelerationRatio")
	private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 0f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
	private val rotationResetSpeedValue = object : FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")
	{
		override fun showCondition() = rotationSilentValue.get()
	}

	private val rotationStrafeGroup = ValueGroup("Strafe")
	private val rotationStrafeValue = ListValue("Mode", arrayOf("Off", "Strict", "Silent"), "Off", "Strafe")
	private val rotationStrafeOnlyGroundValue = BoolValue("OnlyGround", false, "StrafeOnlyGround")

	private val rotationPredictGroup = ValueGroup("Predict")
	private val rotationPredictEnemyGroup = ValueGroup("Enemy")
	private val rotationPredictEnemyEnabledValue = BoolValue("Enabled", true, "Predict")
	private val rotationPredictEnemyIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPredictSize" to "MinPredictSize")

	private val rotationPredictPlayerGroup = ValueGroup("Player")
	private val rotationPredictPlayerEnabledValue = BoolValue("Enabled", true, "PlayerPredict")
	private val rotationPredictPlayerIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPlayerPredictSize" to "MinPlayerPredictSize")

	private val rotationBacktrackGroup = ValueGroup("Backtrack")
	private val rotationBacktrackEnabledValue = BoolValue("Enabled", false, "Backtrace")
	private val rotationBacktrackTicksValue: IntegerValue = IntegerValue("Ticks", 3, 1, 6, "BacktraceTicks")

	private val fovGroup = ValueGroup("FoV")
	private val fovModeValue = ListValue("Type", arrayOf("ServerRotation", "ClientRotation"), "ClientRotation", "FovMode")
	private val fovValue = FloatValue("FoV", 180f, 0f, 180f, "FoV")

	private val visualGroup = ValueGroup("Visual")
	private val visualFakeSharpValue = BoolValue("FakeSharp", true, "FakeSharp")
	private val visualParticles = IntegerValue("Particles", 1, 0, 10, "Particles")

	private val visualMarkGroup = ValueGroup("Mark")
	private val visualMarkTargetValue = ListValue("Target", arrayOf("None", "Platform", "Box"), "Platform", "Mark")

	private val visualMarkRangeGroup = ValueGroup("Range")
	private val visualMarkRangeModeValue = ListValue("Mode", arrayOf("None", "AttackRange", "ExceptBlockRange", "All"), "AttackRange", "Mark-Range")
	private val visualMarkRangeLineWidthValue = FloatValue("LineWidth", 1f, 0.5f, 2f)
	private val visualMarkRangeAccuracyValue = FloatValue("Accuracy", 10F, 0.5F, 20F, "Mark-Range-Accuracy")
	private val visualMarkRangeFadeSpeedValue = IntegerValue("FadeSpeed", 5, 1, 9)

	private val visualMarkRangeColorGroup = object : ValueGroup("Color")
	{
		override fun showCondition() = !visualMarkRangeModeValue.get().equals("None", ignoreCase = true)
	}
	private val visualMarkRangeColorAttackValue = RGBColorValue("Attack", 0, 255, 0)
	private val visualMarkRangeColorThroughWallsAttackValue = RGBColorValue("ThroughWallsAttack", 200, 128, 0)

	private val visualMarkRangeColorAimValue = object : RGBColorValue("Aim", 255, 0, 0)
	{
		override fun showCondition() = !rotationMode.get().equals("Off", ignoreCase = true) && !visualMarkRangeModeValue.get().equals("AttackRange", ignoreCase = true)
	}
	private val visualMarkRangeColorSwingValue = object : RGBColorValue("Swing", 0, 0, 255)
	{
		override fun showCondition() = swingEnabledValue.get() && !visualMarkRangeModeValue.get().equals("AttackRange", ignoreCase = true)
	}

	private val visualMarkRangeColorBlockValue = object : RGBColorValue("Block", 255, 0, 255)
	{
		override fun showCondition() = !autoBlockValue.get().equals("Off", ignoreCase = true) && arrayOf("None", "AttackRange", "ExceptBlockRange").none { visualMarkRangeModeValue.get().equals(it, ignoreCase = true) }
	}
	private val visualMarkRangeColorInteractBlockValue = object : RGBColorValue("InteractBlock", 255, 64, 255)
	{
		override fun showCondition() = !autoBlockValue.get().equals("Off", ignoreCase = true) && arrayOf("None", "AttackRange", "ExceptBlockRange").none { visualMarkRangeModeValue.get().equals(it, ignoreCase = true) }
	}

	private val disableOnDeathValue = BoolValue("DisableOnDeath", true)

	/**
	 * MODULE
	 */

	// Target
	var target: IEntityLivingBase? = null
	private var currentTarget: IEntityLivingBase? = null
	private var hitable = false
	private val previouslySwitchedTargets = mutableSetOf<Int>()

	private var lastTargetID: Int = -1

	// Attack delay
	private val attackTimer = MSTimer()
	private var attackDelay = 0L
	private var clicks = 0

	// Suspend killaura timer
	private val suspendTimer = MSTimer()
	private var suspend = 0L

	// Lock rotation timer
	private val lockRotationTimer = MSTimer()
	private var lockRotationDelay = 0L
	private var lockRotation: Rotation? = null

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

	private var switchDelay = switchDelayValue.getRandomDelay()
	private val switchDelayTimer = MSTimer()

	var updateHitableDebug: Array<String>? = null
	var updateRotationsDebug: Array<String>? = null
	var startBlockingDebug: Array<String>? = null

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
	private var easingRangeMarks: ArrayList<Float>? = null

	init
	{
		comboReachGroup.addAll(comboReachEnabledValue, comboReachIncrementValue, comboReachLimitValue)
		rangeAttackGroup.addAll(rangeAttackOnGroundValue, rangeAttackOffGroundValue)
		rangeGroup.addAll(rangeAttackGroup, rangeThroughWallsAttackValue, rangeAimValue, rangeSprintReducementValue, comboReachGroup)
		swingGroup.addAll(swingEnabledValue, swingFakeSwingValue, swingRangeValue)
		targetGroup.addAll(targetPriorityValue, targetModeValue, targetLimitedMultiTargetsValue)
		interactAutoBlockGroup.addAll(interactAutoBlockEnabledValue, interactAutoBlockRangeValue)
		autoBlockGroup.addAll(autoBlockValue, autoBlockRangeValue, autoBlockRate, autoBlockHitableCheckValue, autoBlockHurtTimeCheckValue, autoBlockWallCheckValue, interactAutoBlockGroup)
		rayCastGroup.addAll(rayCastEnabledValue, rayCastSkipEnemyCheckValue, rayCastLivingOnlyValue, rayCastIncludeCollidedValue)
		bypassGroup.addAll(bypassKeepSprintValue, bypassAACValue, bypassFailRateValue, bypassSuspendWhileConsumingValue, noInventoryGroup, rayCastGroup)
		noInventoryGroup.addAll(noInventoryAttackEnabledValue, noInventoryDelayValue)
		rotationSearchCenterGroup.addAll(rotationSearchCenterHitboxShrinkValue, rotationSearchCenterSensitivityValue)
		rotationJitterGroup.addAll(rotationJitterEnabledValue, rotationJitterYawRate, rotationJitterPitchRate, rotationJitterYawIntensityValue, rotationJitterPitchIntensityValue)
		rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationTicks)
		rotationLockAfterTeleportGroup.addAll(rotationLockAfterTeleportEnabledValue, rotationLockAfterTeleportDelayValue)
		rotationStrafeGroup.addAll(rotationStrafeValue, rotationStrafeOnlyGroundValue)
		rotationPredictEnemyGroup.addAll(rotationPredictEnemyEnabledValue, rotationPredictEnemyIntensityValue)
		rotationPredictPlayerGroup.addAll(rotationPredictPlayerEnabledValue, rotationPredictPlayerIntensityValue)
		rotationPredictGroup.addAll(rotationPredictEnemyGroup, rotationPredictPlayerGroup)
		rotationBacktrackGroup.addAll(rotationBacktrackEnabledValue, rotationBacktrackTicksValue)
		rotationGroup.addAll(rotationMode, rotationLockValue, rotationLockExpandRangeValue, rotationSilentValue, rotationRandomCenterSizeValue, rotationSearchCenterGroup, rotationJitterGroup, rotationKeepRotationGroup, rotationLockAfterTeleportGroup, rotationAccelerationRatioValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationStrafeGroup, rotationPredictGroup, rotationBacktrackGroup)
		fovGroup.addAll(fovModeValue, fovValue)
		visualMarkRangeColorGroup.addAll(visualMarkRangeColorAttackValue, visualMarkRangeColorThroughWallsAttackValue, visualMarkRangeColorAimValue, visualMarkRangeColorSwingValue, visualMarkRangeColorBlockValue, visualMarkRangeColorInteractBlockValue)
		visualMarkRangeGroup.addAll(visualMarkRangeModeValue, visualMarkRangeLineWidthValue, visualMarkRangeAccuracyValue, visualMarkRangeColorGroup)
		visualMarkGroup.addAll(visualMarkTargetValue, visualMarkRangeGroup)
		visualGroup.addAll(visualFakeSharpValue, visualParticles, visualMarkGroup)

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
			LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "KillAura", "Disabled KillAura due world change", 1000L)
		}
	}

	@EventTarget
	fun onTick(@Suppress("UNUSED_PARAMETER") event: TickEvent)
	{
		if (mc.thePlayer == null || mc.theWorld == null)
		{
			state = false
			LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "KillAura", "Disabled KillAura due world change", 1000L)
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
			if (autoBlockValue.get().equals("AfterTick", true) && canAutoBlock(thePlayer)) startBlocking(thePlayer, currentTarget, interactAutoBlockEnabledValue.get() && hitable)

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
		if (shouldCancelRun(thePlayer) || (noInventoryAttackEnabledValue.get() && (classProvider.isGuiContainer(mc.currentScreen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))) return

		// Update target
		updateTarget(theWorld, thePlayer)

		// Pre-AutoBlock
		if (autoBlockTarget != null && !autoBlockValue.get().equals("AfterTick", ignoreCase = true) && canAutoBlock(thePlayer) && (!autoBlockHitableCheckValue.get() || hitable)) startBlocking(thePlayer, autoBlockTarget, interactAutoBlockEnabledValue.get()) else if (canAutoBlock(thePlayer)) stopBlocking()

		// Target
		currentTarget = target ?: return
		if (!targetModeValue.get().equals("Switch", ignoreCase = true) && currentTarget.isEnemy(bypassAACValue.get())) target = currentTarget
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

		attackRange = (if (thePlayer.onGround) rangeAttackOnGroundValue else rangeAttackOffGroundValue).get()
		aimRange = rangeAimValue.get()
		swingRange = swingRangeValue.get()
		blockRange = autoBlockRangeValue.get()
		interactBlockRange = interactAutoBlockRangeValue.get()

		// Range mark
		val markRangeMode = visualMarkRangeModeValue.get().toLowerCase()
		if (markRangeMode != "none")
		{
			val arr = arrayOfNulls<Pair<Float, Int>?>(6)

			arr[0] = attackRange to visualMarkRangeColorAttackValue.get()

			val throughWallsRange = rangeThroughWallsAttackValue.get()
			if (throughWallsRange > 0) arr[1] = throughWallsRange to visualMarkRangeColorThroughWallsAttackValue.get()

			arr[2] = aimRange to visualMarkRangeColorAimValue.get()

			if (swingFakeSwingValue.get()) arr[3] = swingRange to visualMarkRangeColorSwingValue.get()

			if (!autoBlockValue.get().equals("Off", ignoreCase = true))
			{
				arr[4] = blockRange to visualMarkRangeColorBlockValue.get()
				if (interactAutoBlockEnabledValue.get()) arr[5] = interactBlockRange to visualMarkRangeColorInteractBlockValue.get()
			}

			rangeMarks = arr.take(when (markRangeMode)
			{
				"attackrange" -> 2
				"exceptblockrange" -> 4
				else -> 6
			}).filterNotNull()
		}

		if (noInventoryAttackEnabledValue.get() && (classProvider.isGuiContainer(screen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))
		{
			target = null
			currentTarget = null
			hitable = false
			comboReach = 0.0F

			if (classProvider.isGuiContainer(screen)) containerOpen = System.currentTimeMillis()

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
		if (noInventoryAttackEnabledValue.get() && (provider.isGuiContainer(screen) || System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get()))
		{
			target = null
			currentTarget = null
			hitable = false
			comboReach = 0.0F
			if (provider.isGuiContainer(screen)) containerOpen = System.currentTimeMillis()
			return
		}

		if (!visualMarkRangeModeValue.get().equals("None", ignoreCase = true))
		{
			val lineWidth = visualMarkRangeLineWidthValue.get()
			val accuracy = visualMarkRangeAccuracyValue.get()

			rangeMarks?.let { rangeMarks ->
				easingRangeMarks?.let { easingRangeMarks ->
					for (i in easingRangeMarks.indices) rangeMarks[i].let { (originalRange, color) ->
						GL11.glPushMatrix()
						RenderUtils.drawRadius(easingRangeMarks[i], accuracy, lineWidth, color)
						GL11.glPopMatrix()

						easingRangeMarks[i] = easeOutCubic(easingRangeMarks[i], originalRange, visualMarkRangeFadeSpeedValue.get())
					}
				} ?: run { easingRangeMarks = rangeMarks.mapTo(ArrayList(), Pair<Float, Int>::first) }
			}
		}

		val target = target ?: return
		val targetEntityId = target.entityId

		val markMode = visualMarkTargetValue.get().toLowerCase()

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

			targetBB = if (rotationBacktrackEnabledValue.get())
			{
				val backtraceTicks = rotationBacktrackTicksValue.get()

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
			if (rotationPredictEnemyEnabledValue.get())
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
			attackDelay = cpsValue.getRandomClickDelay()
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

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (classProvider.isSPacketPlayerPosLook(event.packet))
		{
			val thePlayer = mc.thePlayer ?: return

			val tpPacket = event.packet.asSPacketPlayerPosLook()

			if (rotationLockAfterTeleportEnabledValue.get())
			{
				lockRotation = Rotation(tpPacket.yaw, tpPacket.pitch)
				lockRotationTimer.reset()
				lockRotationDelay = rotationLockAfterTeleportDelayValue.getRandomDelay()

				if (rotationSilentValue.get()) RotationUtils.setTargetRotation(lockRotation, 0) else lockRotation?.applyRotationToPlayer(thePlayer)
			}
		}
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
		val failRate = bypassFailRateValue.get()
		val aac = bypassAACValue.get()

		val openInventory = aac && provider.isGuiContainer(mc.currentScreen)
		val limitedMultiTargets = targetLimitedMultiTargetsValue.get()

		// FailRate
		failedToHit = failRate > 0 && Random.nextInt(100) <= failRate

		// Close inventory when open
		if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

		// Check is not hitable or check failrate
		val fakeAttack = !hitable || failedToHit || failedToRotate

		if (fakeAttack)
		{
			if (swingEnabledValue.get() && distance <= swingRange && (failedToHit || swingFakeSwingValue.get()))
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
				if ((isBlocking || (canAutoBlock(thePlayer) && distance <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(thePlayer, theCurrentTarget, interactAutoBlockEnabledValue.get())
			}
		}
		else
		{
			if (comboReachEnabledValue.get()) comboReach = (comboReach + comboReachIncrementValue.get()).coerceAtMost(comboReachLimitValue.get())

			// Attack
			if (targetModeValue.get().equals("Multi", ignoreCase = true))
			{
				var targets = 0

				run {
					theWorld.getEntitiesInRadius(thePlayer, maxAttackRange + 2.0).filter { it.isEnemy(aac) }.filter { thePlayer.getDistanceToEntityBox(it) <= getAttackRange(thePlayer, it) }.map(IEntity::asEntityLivingBase).forEach { entity ->
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
			switchDelay = switchDelayValue.getRandomDelay()
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
		if (target != null) lastTargetID = target?.entityId ?: -1

		// Reset fixed target to null
		target = null

		// Settings
		val hurtTime = hurtTimeValue.get()
		val fov = fovValue.get()
		val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)
		val playerPredict = rotationPredictPlayerEnabledValue.get()
		val playerPredictSize = RotationUtils.MinMaxPair(rotationPredictPlayerIntensityValue.getMin(), rotationPredictPlayerIntensityValue.getMax())

		// Find possible targets
		val targets = mutableListOf<IEntityLivingBase>()
		val abTargets = mutableListOf<IEntityLivingBase>()

		val aac = bypassAACValue.get()
		val fovMode = fovModeValue.get()

		val autoBlockHurtTimeCheck = autoBlockHurtTimeCheckValue.get()
		val smartBlock = autoBlockWallCheckValue.get()

		val entityList = theWorld.getEntitiesInRadius(thePlayer, maxTargetRange + 2.0).filter { it.isEnemy(aac) }.filterNot { switchMode && previouslySwitchedTargets.contains(it.entityId) }.run { if (fov < 180f) filter { (if (fovMode.equals("ServerRotation", ignoreCase = true)) RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) else RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, playerPredictSize)) <= fov } else this }.map { it.asEntityLivingBase() to thePlayer.getDistanceToEntityBox(it) }

		entityList.forEach { (entity, distance) ->
			val entityHurtTime = entity.hurtTime

			if (distance <= blockRange && (!autoBlockHurtTimeCheck || entityHurtTime <= hurtTime) && (!smartBlock || RotationUtils.isVisible(theWorld, thePlayer, RotationUtils.getCenter(entity.entityBoundingBox))) /* Simple wall check */) abTargets.add(entity)
			if (distance <= getAttackRange(thePlayer, entity) && entityHurtTime <= hurtTime) targets.add(entity)
		}

		// If there is no attackable entities found, search about pre-aimable entities and pre-swingable entities instead.
		if (targets.isEmpty()) entityList.filter { it.second <= maxTargetRange }.forEach { targets.add(it.first) }

		val checkIsClientTarget = { entity: IEntity -> if (entity.isClientTarget()) -1000000.0 else 0.0 }

		// Sort targets by priority
		when (targetPriorityValue.get().toLowerCase())
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

		val swing = swingEnabledValue.get()

		// Stop blocking
		if (thePlayer.isBlocking || serverSideBlockingStatus)
		{
			netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.RELEASE_USE_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))
			serverSideBlockingStatus = false
			clientSideBlockingStatus = false
		}

		// Call attack event
		LiquidBounce.eventManager.callEvent(AttackEvent(entity, WVec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))

		// Attack target
		if (swing && Backend.MINECRAFT_VERSION_MINOR == 8) thePlayer.swingItem()

		netHandler.addToSendQueue(provider.createCPacketUseEntity(entity, ICPacketUseEntity.WAction.ATTACK))

		CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)

		if (swing && Backend.MINECRAFT_VERSION_MINOR != 8) thePlayer.swingItem()

		if (!bypassKeepSprintValue.get() && mc.playerController.currentGameType != IWorldSettings.WGameType.SPECTATOR) thePlayer.attackTargetEntityWithCurrentItem(entity)

		// Extra critical effects
		val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

		val crackSize = visualParticles.get()
		if (crackSize > 0) repeat(crackSize) {
			val target = target ?: return@attackEntity

			// Critical Effect
			if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(provider.getPotionEnum(PotionType.BLINDNESS).id) && thePlayer.ridingEntity == null || criticals.state && criticals.canCritical(thePlayer)) thePlayer.onCriticalHit(target)

			// Enchant Effect
			if (functions.getModifierForCreature(thePlayer.heldItem, target.creatureAttribute) > 0.0f || visualFakeSharpValue.get()) thePlayer.onEnchantmentCritical(target)
		}

		// Start blocking after attack
		if ((thePlayer.isBlocking || (canAutoBlock(thePlayer) && thePlayer.getDistanceToEntityBox(entity) <= blockRange)) && !autoBlockValue.get().equals("AfterTick", true)) startBlocking(thePlayer, entity, interactAutoBlockEnabledValue.get())

		@Suppress("ConstantConditionIf") if (Backend.MINECRAFT_VERSION_MINOR != 8) thePlayer.resetCooldown()
	}

	/**
	 * Update killaura rotations to enemy
	 */
	private fun updateRotations(theWorld: IWorld, thePlayer: IEntityPlayer, entity: IEntity, isAttackRotation: Boolean): Boolean
	{
		val predictEnemy = rotationPredictEnemyEnabledValue.get()
		if (predictEnemy)
		{
			predictX = rotationPredictEnemyIntensityValue.getRandom()
			predictY = rotationPredictEnemyIntensityValue.getRandom()
			predictZ = rotationPredictEnemyIntensityValue.getRandom()
		}

		val targetBox = getHitbox(entity, 0.0)

		val jitter = rotationJitterEnabledValue.get()

		// Jitter
		val jitterData = if (jitter) RotationUtils.JitterData(rotationJitterYawRate.get(), rotationJitterPitchRate.get(), rotationJitterYawIntensityValue.getMin(), rotationJitterYawIntensityValue.getMax(), rotationJitterPitchIntensityValue.getMin(), rotationJitterPitchIntensityValue.getMax()) else null

		var flags = 0

		val rotationMode = rotationMode.get().toLowerCase()

		// Apply rotation mode to flags
		flags = flags or when (rotationMode)
		{
			"lockcenter" -> RotationUtils.LOCK_CENTER
			"outborder" -> if (!attackTimer.hasTimePassed(attackDelay shr 1)) RotationUtils.OUT_BORDER else RotationUtils.RANDOM_CENTER
			"randomcenter" -> RotationUtils.RANDOM_CENTER
			else -> 0
		}

		val predictPlayer = rotationPredictPlayerEnabledValue.get()

		if (jitter && (thePlayer.getDistanceToEntityBox(entity) <= max(maxAttackRange, if (swingFakeSwingValue.get()) swingRange else Float.MIN_VALUE))) flags = flags or RotationUtils.JITTER
		if (predictPlayer) flags = flags or RotationUtils.PLAYER_PREDICT
		if (thePlayer.getDistanceToEntityBox(entity) <= rangeThroughWallsAttackValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK

		failedToRotate = false

		val searchCenter = { distance: Float, distanceOutOfRangeCallback: (() -> Unit)? -> RotationUtils.searchCenter(theWorld, thePlayer, targetBox, flags, jitterData, RotationUtils.MinMaxPair(rotationPredictPlayerIntensityValue.getMin(), rotationPredictPlayerIntensityValue.getMax()), distance, rotationSearchCenterHitboxShrinkValue.get().toDouble(), rotationSearchCenterSensitivityValue.get(), rotationRandomCenterSizeValue.get().toDouble(), distanceOutOfRangeCallback) }

		// Search
		var fallBackRotation: VecRotation? = null
		var useFallback = false
		val rotation = if (rotationLockAfterTeleportEnabledValue.get() && lockRotation != null && !lockRotationTimer.hasTimePassed(lockRotationDelay)) lockRotation!!
		else if (!rotationLockValue.get() && RotationUtils.isFaced(theWorld, thePlayer, entity, aimRange.toDouble()) { getHitbox(entity, rotationLockExpandRangeValue.get().toDouble()) }) Rotation(lastYaw, lastPitch)
		else (searchCenter(if (isAttackRotation) attackRange else aimRange) {
			// Because of '.getDistanceToEntityBox()' is not perfect. (searchCenter() >>> 넘사벽 >>> getDistanceToEntityBox())
			failedToRotate = true

			// TODO: Make better fallback
			fallBackRotation = searchCenter(aimRange, null)
		} ?: run {
			useFallback = true
			fallBackRotation
		} ?: return false).rotation

		lastYaw = rotation.yaw
		lastPitch = rotation.pitch

		if (rotationMode.equals("Off", ignoreCase = true))
		{
			updateRotationsDebug = arrayOf("state".equalTo("DISABLED", "\u00A74"), "reason" equalTo "Rotation is turned off".withParentheses("\u00A7c"))
			return true
		}

		if (rotationTurnSpeedValue.getMax() <= 0F)
		{
			updateRotationsDebug = arrayOf("state".equalTo("DISABLED", "\u00A74"), "reason" equalTo "TurnSpeed is zero or negative".withParentheses("\u00A7c"))
			return true
		}

		// Limit TurnSpeed
		val turnSpeed = rotationTurnSpeedValue.getRandomStrict()

		// Acceleration
		val acceleration = rotationAccelerationRatioValue.getRandomStrict()

		val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation, turnSpeed, acceleration)

		lastYaw = limitedRotation.yaw
		lastPitch = limitedRotation.pitch

		val commonDebug = arrayOf(

			"state".equalTo("SUCCESS", "\u00A7a"), // rotationResult
			"fallback" equalTo useFallback, // useFallback
			"jitter" equalTo (flags and RotationUtils.JITTER != 0), // jitter
			"skipVisibleChecks" equalTo (flags and RotationUtils.SKIP_VISIBLE_CHECK != 0) // skipVisibleCheck

		)

		if (rotationSilentValue.get())
		{
			val keepLength = if (rotationKeepRotationEnabledValue.get() && rotationKeepRotationTicks.getMax() > 0) rotationKeepRotationTicks.getRandom() else 0

			updateRotationsDebug = arrayOf(*commonDebug, "applicationMode" equalTo "silent", "keepTicks" equalTo keepLength)

			RotationUtils.setTargetRotation(limitedRotation, keepLength)
		}
		else
		{
			updateRotationsDebug = arrayOf(*commonDebug, "applicationMode" equalTo "direct")

			limitedRotation.applyRotationToPlayer(thePlayer)
		}

		val maxResetSpeed = rotationResetSpeedValue.getMax().coerceAtLeast(10F)
		val minResetSpeed = rotationResetSpeedValue.getMin().coerceAtLeast(10F)
		if (maxResetSpeed < 180) RotationUtils.setNextResetTurnSpeed(minResetSpeed, maxResetSpeed)

		return true
	}

	private val getHitbox: (IEntity, Double) -> IAxisAlignedBB = { target: IEntity, expand: Double ->
		var bb = target.entityBoundingBox

		val collisionExpand = target.collisionBorderSize.toDouble()
		bb = bb.expand(collisionExpand, collisionExpand, collisionExpand)

		// Backtrace
		if (rotationBacktrackEnabledValue.get()) bb = LocationCache.getAABBBeforeNTicks(target.entityId, rotationBacktrackTicksValue.get(), bb)

		// Entity movement predict
		if (rotationPredictEnemyEnabledValue.get()) bb = bb.offset((target.posX - target.lastTickPosX) * predictX, (target.posY - target.lastTickPosY) * predictY, (target.posZ - target.lastTickPosZ) * predictZ)

		bb.expand(expand, expand, expand)
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

		val updateHitableByRange = {
			hitable = currentTarget != null && thePlayer.getDistanceToEntityBox(currentTarget) <= reach
			arrayOf("raycast" equalTo false, "rangeCheck" equalTo hitable)
		}

		if (rotationMode.get().equals("Off", ignoreCase = true))
		{
			updateHitableDebug = arrayOf(*updateHitableByRange(), "reason" equalTo "Rotation is turned off".withParentheses("\u00A7c"))
			return
		}

		if (rotationTurnSpeedValue.getMax() <= 0F)
		{
			updateHitableDebug = arrayOf(*updateHitableByRange(), "reason" equalTo "TurnSpeed is zero or negative".withParentheses("\u00A7c"))
			return
		}

		if (targetModeValue.get().equals("Multi", ignoreCase = true))
		{
			updateHitableDebug = arrayOf(*updateHitableByRange(), "reason" equalTo "MultiAura".withParentheses("\u00A7c"))
			return
		}

		val aac = bypassAACValue.get()
		val livingOnly = rayCastLivingOnlyValue.get()
		val skipEnemyCheck = rayCastSkipEnemyCheckValue.get()
		val includeCollidedWithTarget = rayCastIncludeCollidedValue.get()

		if (rayCastEnabledValue.get())
		{
			val provider = classProvider

			val distanceToTarget = currentTarget?.let(thePlayer::getDistanceToEntityBox)
			val raycastedEntity = theWorld.raycastEntity(thePlayer, reach + 1.0, lastYaw, lastPitch, { getHitbox(it, if (rotationLockValue.get()) 0.0 else rotationLockExpandRangeValue.get().toDouble()) }) { entity -> entity != null && (!livingOnly || (provider.isEntityLivingBase(entity) && !provider.isEntityArmorStand(entity))) && (skipEnemyCheck || entity.isEnemy(aac) || includeCollidedWithTarget && theWorld.getEntitiesWithinAABBExcludingEntity(entity, entity.entityBoundingBox).isNotEmpty()) }
			val distanceToRaycasted = raycastedEntity?.let(thePlayer::getDistanceToEntityBox)

			if (raycastedEntity != null && provider.isEntityLivingBase(raycastedEntity) && (LiquidBounce.moduleManager[NoFriends::class.java].state || !provider.isEntityPlayer(raycastedEntity) || !raycastedEntity.asEntityPlayer().isClientFriend())) this.currentTarget = raycastedEntity.asEntityLivingBase()

			updateHitableDebug = if (distanceToTarget != null)
			{
				if (distanceToRaycasted != null)
				{
					if (currentTarget != raycastedEntity) arrayOf("raycast" equalTo true, "result" equalTo "\u00A7aSUCCESS", "from" equalTo listOf("name".equalTo(currentTarget.name, "\u00A7e\u00A7l"), "id".equalTo(currentTarget.entityId, "\u00A7e")).serialize().withParentheses("\u00A78"), "to" equalTo listOf("name".equalTo(raycastedEntity.name, "\u00A7e\u00A7l"), "id".equalTo(raycastedEntity.entityId, "\u00A7e")).serialize().withParentheses("\u00A78"), "distance" equalTo DECIMALFORMAT_6.format(distanceToRaycasted - distanceToTarget))
					else arrayOf("raycast" equalTo true, "result" equalTo "\u00A7eEQUAL", "reason" equalTo "currentTarget = raycastedTarget".withParentheses("\u00A7c"))
				}
				else arrayOf("raycast" equalTo false, "reason" equalTo "raycastedTarget is null".withParentheses("\u00A7c"))
			}
			else arrayOf("raycast" equalTo false, "reason" equalTo "currentTarget is null".withParentheses("\u00A7c"))

			hitable = (distanceToTarget == null || distanceToTarget <= reach) && (distanceToRaycasted == null || distanceToRaycasted <= reach) && this.currentTarget == raycastedEntity
		}
		else
		{
			hitable = if (currentTarget != null) RotationUtils.isFaced(theWorld, thePlayer, currentTarget, reach) { getHitbox(it, if (rotationLockValue.get()) 0.0 else rotationLockExpandRangeValue.get().toDouble()) } else false
			updateHitableDebug = arrayOf("raycast" equalTo false, "faced" equalTo hitable)
		}
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

				val boundingBox = getHitbox(interactEntity, 0.0)

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
				startBlockingDebug = if (movingObject != null && movingObject.typeOfHit != IMovingObjectPosition.WMovingObjectType.MISS)
				{
					val hitVec = movingObject.hitVec

					netHandler.addToSendQueue(provider.createCPacketUseEntity(interactEntity, WVec3(hitVec.xCoord - interactEntity.posX, hitVec.yCoord - interactEntity.posY, hitVec.zCoord - interactEntity.posZ)))
					netHandler.addToSendQueue(provider.createCPacketUseEntity(interactEntity, ICPacketUseEntity.WAction.INTERACT))

					if (movingObject.typeOfHit == IMovingObjectPosition.WMovingObjectType.BLOCK) arrayOf("result".equalTo("BLOCK HIT", "\u00A7e"), "blockPos" equalTo "\u00A7e${movingObject.blockPos}", "blockSide" equalTo movingObject.sideHit, "hitVec" equalTo hitVec)
					else arrayOf("result".equalTo("ENTITY HIT", "\u00A7a"), "name" equalTo "\u00A7e${movingObject.entityHit?.name}", "dispName" equalTo movingObject.entityHit?.displayName?.formattedText, "hitVec" equalTo hitVec)
				}
				else arrayOf("result".equalTo("FAILED", "\u00A7c"), "reason" equalTo "raytraceResult = null".withParentheses("\u00A74"))
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

		val shouldDisableOnDeath = thePlayer.spectator || !thePlayer.isAlive(false)

		if (shouldDisableOnDeath && disableOnDeathValue.get())
		{
			state = false
			LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "KillAura", "Disabled KillAura due player death", 1000L)
		}

		return shouldDisableOnDeath || (bypassSuspendWhileConsumingValue.get() && thePlayer.isUsingItem && thePlayer.heldItem == thePlayer.itemInUse && (classProvider.isItemFood(thePlayer.heldItem?.item) || classProvider.isItemPotion(thePlayer.heldItem?.item))) || !suspendTimer.hasTimePassed(suspend) || (moduleManager[Blink::class.java] as Blink).state || moduleManager[FreeCam::class.java].state
	}

	/**
	 * Check if player is able to block
	 */
	private fun canAutoBlock(thePlayer: IEntityPlayer): Boolean = thePlayer.heldItem != null && classProvider.isItemSword(thePlayer.heldItem?.item) && Backend.MINECRAFT_VERSION_MINOR == 8

	/**
	 * Range
	 */
	private val maxAttackRange: Float
		get() = max(attackRange, rangeThroughWallsAttackValue.get()) + comboReach

	private val maxTargetRange: Float
		get() = max(aimRange, max(maxAttackRange, if (swingFakeSwingValue.get()) swingRange else 0f))

	private fun getAttackRange(thePlayer: IEntity, entity: IEntity): Float
	{
		val throughWallsRange = rangeThroughWallsAttackValue.get()
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
