/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import kotlin.math.abs
import kotlin.random.Random

@ModuleInfo(name = "Aimbot", description = "Automatically faces selected entities around you.", category = ModuleCategory.COMBAT)
class Aimbot : Module()
{
	/**
	 * Target Range
	 */
	private val rangeValue = FloatValue("Range", 4.4F, 1F, 8F)

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
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 2f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 1f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) this.set(v)
		}
	}

	/**
	 * Field of View
	 */
	private val fovValue = FloatValue("FoV", 30F, 1F, 180F)

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
	 * Should we aim through walls?
	 */
	private val throughWallsValue = BoolValue("ThroughWalls", false)

	/**
	 * Lock Center
	 */
	private val centerValue = BoolValue("Center", false)

	/**
	 * Lock rotation
	 */
	private val lockValue = BoolValue("Lock", true)

	/**
	 * OnClick
	 */
	private val onClickValue = BoolValue("OnClick", false)
	private val onClickKeepValue = IntegerValue("OnClickKeepTime", 500, 0, 1000)

	/**
	 * Jitter
	 */
	private val jitterValue = BoolValue("Jitter", false)
	private val jitterRateYaw = IntegerValue("YawJitterRate", 50, 1, 100)
	private val jitterRatePitch = IntegerValue("PitchJitterRate", 50, 1, 100)
	private val minYawJitterStrengthValue: FloatValue = object : FloatValue("MinYawJitterStrength", 0f, 0f, 5f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = maxYawJitterStrengthValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val maxPitchJitterStrengthValue: FloatValue = object : FloatValue("MaxPitchJitterStrength", 1f, 0f, 5f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val i = minPitchJitterStrengthValue.get()
			if (i > newValue) this.set(i)
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

	private val hitboxDecrementValue = FloatValue("SearchCenter-HitboxShrink", 0.1f, 0.15f, 0.45f)
	private val centerSearchSensitivityValue = IntegerValue("SearchCenter-Steps", 8, 4, 20)

	private val aimFrictionValue = FloatValue("AimFriction", 0.6F, 0.3F, 1F)
	private val aimFrictionTimingValue = ListValue("AimFrictionTiming", arrayOf("Before", "After"), "Before")
	private val resetThresoldValue = FloatValue("AimUnlockThresholdSpeed", 0.8F, 0.5F, 2F)

	private val clickTimer = MSTimer()

	var target: IEntityLivingBase? = null

	private var yawMovement = 0F
	private var pitchMovement = 0F

	@EventTarget
	fun onMotion(@Suppress("UNUSED_PARAMETER") event: MotionEvent)
	{
		if (event.eventState != EventState.PRE) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()

		if (onClickValue.get() && clickTimer.hasTimePassed(onClickKeepValue.get().toLong()))
		{
			target = null
			fadeRotations(thePlayer)
			return
		}

		val range = rangeValue.get()
		val fov = fovValue.get()
		val throughWalls = throughWallsValue.get()

		val playerPredict = playerPredictValue.get()
		val playerPredictSize = RotationUtils.MinMaxPair(minPlayerPredictSizeValue.get(), maxPlayerPredictSizeValue.get())

		val jitter = jitterValue.get()

		target = EntityUtils.getEntitiesInRadius(theWorld, thePlayer, range + 2.0).asSequence().filter { EntityUtils.isSelected(it, true) }.filter { thePlayer.getDistanceToEntityBox(it) <= range }.run { if (fov < 180F) filter { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) <= fov } else this }.run { if (throughWalls) this else filter(thePlayer::canEntityBeSeen) }.minBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) }?.asEntityLivingBase()

		val entity = target ?: run {
			fadeRotations(thePlayer)
			return@onMotion
		}

		if (!lockValue.get() && RotationUtils.isFaced(theWorld, thePlayer, target, range.toDouble()))
		{
			fadeRotations(thePlayer)
			return
		}

		// Jitter
		val jitterData = if (jitter) RotationUtils.JitterData(jitterRateYaw.get(), jitterRatePitch.get(), minYawJitterStrengthValue.get(), maxYawJitterStrengthValue.get(), minPitchJitterStrengthValue.get(), maxPitchJitterStrengthValue.get()) else null

		// Apply predict to target box

		var targetBB = entity.entityBoundingBox

		if (predictValue.get())
		{
			val minPredictSize = minPredictSizeValue.get()
			val maxPredictSize = maxPredictSizeValue.get()

			val xPredict = (entity.posX - entity.prevPosX) * RandomUtils.nextFloat(minPredictSize, maxPredictSize)
			val yPredict = (entity.posY - entity.prevPosY) * RandomUtils.nextFloat(minPredictSize, maxPredictSize)
			val zPredict = (entity.posZ - entity.prevPosZ) * RandomUtils.nextFloat(minPredictSize, maxPredictSize)

			targetBB = targetBB.offset(xPredict, yPredict, zPredict)
		}

		// Search rotation
		val currentRotation = RotationUtils.clientRotation

		// Build the bit mask
		var flags = 0

		if (centerValue.get()) flags = flags or RotationUtils.LOCK_CENTER
		if (jitter) flags = flags or RotationUtils.JITTER
		if (throughWalls) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK
		if (playerPredict) flags = flags or RotationUtils.PLAYER_PREDICT
		if (predictValue.get()) flags = flags or RotationUtils.ENEMY_PREDICT

		val targetRotation = (RotationUtils.searchCenter(theWorld, thePlayer, targetBB, flags, jitterData, playerPredictSize, range, hitboxDecrementValue.get().toDouble(), centerSearchSensitivityValue.get(), 0.0) ?: return).rotation

		// TurnSpeed
		val maxTurnSpeed = maxTurnSpeedValue.get()
		val minTurnSpeed = minTurnSpeedValue.get()
		val turnSpeed = if (minTurnSpeed < 180f) minTurnSpeed + (maxTurnSpeed - minTurnSpeed) * Random.nextFloat() else 180f

		// Acceleration
		val maxAcceleration = maxAccelerationRatioValue.get()
		val minAcceleration = minAccelerationRatioValue.get()
		val acceleration = if (maxAcceleration > 0f) minAcceleration + (maxAcceleration - minAcceleration) * Random.nextFloat() else 0f

		// Limit by TurnSpeed any apply
		val limitedRotation = RotationUtils.limitAngleChange(currentRotation, targetRotation, turnSpeed, acceleration)

		yawMovement = limitedRotation.yaw - currentRotation.yaw
		pitchMovement = limitedRotation.pitch - currentRotation.pitch

		// Re-use local variable 'currentRotation'
		currentRotation.yaw += yawMovement
		currentRotation.pitch += pitchMovement

		currentRotation.applyRotationToPlayer(thePlayer)
	}

	private fun fadeRotations(thePlayer: IEntityPlayerSP)
	{

		val friction = aimFrictionValue.get()

		val unlockThr = resetThresoldValue.get()
		val before = aimFrictionTimingValue.get().equals("Before", ignoreCase = true)

		if (before && friction >= 1F) return

		if (before)
		{
			yawMovement = if (abs(yawMovement) <= unlockThr) 0F else yawMovement - yawMovement * friction
			pitchMovement = if (abs(pitchMovement) <= unlockThr) 0F else pitchMovement - pitchMovement * friction
		}

		if (yawMovement <= 0F && pitchMovement <= 0F) return

		RotationUtils.clientRotation.apply { yaw += yawMovement; pitch += pitchMovement }.applyRotationToPlayer(thePlayer)

		if (!before)
		{
			yawMovement = if (abs(yawMovement) <= unlockThr) 0F else yawMovement - yawMovement * friction
			pitchMovement = if (abs(pitchMovement) <= unlockThr) 0F else pitchMovement - pitchMovement * friction
		}
	}

	override fun onDisable()
	{
		target = null
	}

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		val fov = fovValue.get()

		if (fov < 180) RenderUtils.drawFoVCircle(fov)
	}

	override val tag: String
		get() = "${fovValue.get()}${if (onClickValue.get()) ", OnClick-${onClickKeepValue.get()}" else ""}"
}
