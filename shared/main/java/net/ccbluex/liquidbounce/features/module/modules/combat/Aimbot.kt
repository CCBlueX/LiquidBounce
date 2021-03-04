/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
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

	private val hitboxDecrementValue = FloatValue("EnemyHitboxDecrement", 0.2f, 0.15f, 0.45f)
	private val centerSearchSensitivityValue = FloatValue("SearchCenterSensitivity", 0.2f, 0.15f, 0.25f)

	private val clickTimer = MSTimer()

	var target: IEntity? = null

	@EventTarget
	fun onStrafe(@Suppress("UNUSED_PARAMETER") event: MotionEvent)
	{
		if (event.eventState != EventState.PRE) return

		if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()

		if (onClickValue.get() && clickTimer.hasTimePassed(onClickKeepValue.get().toLong()))
		{
			target = null
			return
		}

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val range = rangeValue.get()
		val fov = fovValue.get()
		val throughWalls = throughWallsValue.get()

		val playerPredict = playerPredictValue.get()
		val minPlayerPredictSize = minPlayerPredictSizeValue.get()
		val maxPlayerPredictSize = maxPlayerPredictSizeValue.get()

		target = theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, true) }.filter { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) <= fov }.filter { (throughWalls || thePlayer.canEntityBeSeen(it)) }.filter { thePlayer.getDistanceToEntityBox(it) <= range }.minBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }

		val entity = target ?: return

		if (!lockValue.get() && RotationUtils.isFaced(theWorld, thePlayer, target, range.toDouble())) return

		// Jitter
		val jitterData = RotationUtils.JitterData(jitterRateYaw.get(), jitterRatePitch.get(), minYawJitterStrengthValue.get(), maxYawJitterStrengthValue.get(), minPitchJitterStrengthValue.get(), maxPitchJitterStrengthValue.get())

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
		val currentRotation = Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch)
		val hitboxDecrement = hitboxDecrementValue.get().toDouble()
		val searchSensitivity = centerSearchSensitivityValue.get().toDouble()

		// Build the bit mask
		var flags = 0

		if (centerValue.get()) flags = flags or RotationUtils.LOCK_CENTER
		if (jitterValue.get()) flags = flags or RotationUtils.JITTER
		if (throughWalls) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK
		if (playerPredict) flags = flags or RotationUtils.PLAYER_PREDICT
		if (predictValue.get()) flags = flags or RotationUtils.ENEMY_PREDICT

		val targetRotation = (RotationUtils.searchCenter(theWorld, thePlayer, targetBB, flags, jitterData, minPlayerPredictSize, maxPlayerPredictSize, range, hitboxDecrement, searchSensitivity) ?: return).rotation

		// TurnSpeed
		val maxTurnSpeed = maxTurnSpeedValue.get()
		val minTurnSpeed = minTurnSpeedValue.get()
		val turnSpeed = if (minTurnSpeed < 180f) minTurnSpeed + (maxTurnSpeed - minTurnSpeed) * Random.nextFloat() else 180f

		// Acceleration
		val maxAcceleration = maxAccelerationRatioValue.get()
		val minAcceleration = minAccelerationRatioValue.get()
		val acceleration = if (maxAcceleration > 0f) minAcceleration + (maxAcceleration - minAcceleration) * Random.nextFloat() else 0f

		// Limit by TurnSpeed any apply
		RotationUtils.limitAngleChange(currentRotation, targetRotation, turnSpeed, acceleration).applyRotationToPlayer(thePlayer)
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
