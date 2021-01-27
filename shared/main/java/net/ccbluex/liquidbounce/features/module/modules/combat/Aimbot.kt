/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import kotlin.random.Random

@ModuleInfo(name = "Aimbot", description = "Automatically faces selected entities around you.", category = ModuleCategory.COMBAT)
class Aimbot : Module()
{
	private val rangeValue = FloatValue("Range", 4.4F, 1F, 8F)

	// Aim Smoothing
	private val maxAccelerationRatio: FloatValue = object : FloatValue("MaxAccelerationRatio", 0f, 0f, .99f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minAccelerationRatio.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minAccelerationRatio: FloatValue = object : FloatValue("MinAccelerationRatio", 0f, 0f, .99f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxAccelerationRatio.get()
			if (v < newValue) this.set(v)
		}
	}

	private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 2f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeed.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 1f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeed.get()
			if (v < newValue) this.set(v)
		}
	}

	private val fovValue = FloatValue("FoV", 30F, 1F, 180F)
	private val centerValue = BoolValue("Center", false)
	private val lockValue = BoolValue("Lock", true)
	private val onClickValue = BoolValue("OnClick", false)
	private val onClickKeepValue = IntegerValue("OnClickKeepTime", 500, 0, 1000)
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
	private val centerSearchSensitivityValue = FloatValue("SearchCennterSensitivity", 0.2f, 0.15f, 0.25f)

	private val clickTimer = MSTimer()

	var target: IEntity? = null

	@EventTarget
	fun onStrafe(@Suppress("UNUSED_PARAMETER") event: StrafeEvent)
	{
		if (mc.gameSettings.keyBindAttack.isKeyDown) clickTimer.reset()

		if (onClickValue.get() && clickTimer.hasTimePassed(onClickKeepValue.get().toLong()))
		{
			target = null
			return
		}

		val thePlayer = mc.thePlayer ?: return

		val range = rangeValue.get()
		target = (mc.theWorld ?: return).loadedEntityList.asSequence().filter {
			EntityUtils.isSelected(it, true) && thePlayer.canEntityBeSeen(it) && thePlayer.getDistanceToEntityBox(it) <= range && RotationUtils.getServerRotationDifference(it) <= fovValue.get()
		}.minBy { RotationUtils.getServerRotationDifference(it) }

		target ?: return

		if (!lockValue.get() && RotationUtils.isFaced(target, range.toDouble())) return

		RotationUtils.limitAngleChange(
			Rotation(thePlayer.rotationYaw, thePlayer.rotationPitch), RotationUtils.searchCenter(
				(target ?: return).entityBoundingBox,
				if (centerValue.get()) RotationUtils.SearchCenterMode.LOCK_CENTER else RotationUtils.SearchCenterMode.SEARCH_GOOD_CENTER,
				jitterValue.get(),
				RotationUtils.JitterData(jitterRateYaw.get(), jitterRatePitch.get(), minYawJitterStrengthValue.get(), maxYawJitterStrengthValue.get(), minPitchJitterStrengthValue.get(), maxPitchJitterStrengthValue.get()),
				false,
				false,
				range,
				hitboxDecrementValue.get().toDouble(),
				centerSearchSensitivityValue.get().toDouble()
			).rotation, Random.nextFloat() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get(), Random.nextFloat() * (maxAccelerationRatio.get() - minAccelerationRatio.get()) + minAccelerationRatio.get()
		).applyRotationToPlayer(thePlayer)
	}

	override fun onDisable()
	{
		target = null
	}

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		if (fovValue.get() < 180) RenderUtils.drawFoVCircle(fovValue.get())
	}

	override val tag: String
		get() = "${fovValue.get()}${if (onClickValue.get()) ", OnClick" else ""}"
}
