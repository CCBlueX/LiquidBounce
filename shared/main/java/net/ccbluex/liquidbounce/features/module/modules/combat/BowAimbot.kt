/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color

@ModuleInfo(name = "BowAimbot", description = "Automatically aims at players when using a bow.", category = ModuleCategory.COMBAT)
class BowAimbot : Module()
{

	private val silentValue = BoolValue("Silent", true)
	private val predictValue = BoolValue("Predict", true)

	//	private val predictSizeValue = FloatValue("PredictSize", 2F, 0.1F, 5F)
	private val playerPredictValue = BoolValue("PlayerPredict", false)
	private val throughWallsValue = BoolValue("ThroughWalls", false)
	private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection"), "ServerDirection")
	private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeed.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeed.get()
			if (v < newValue) this.set(v)
		}
	}

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
	private val markValue = BoolValue("Mark", true)

	private var target: IEntity? = null

	override fun onDisable()
	{
		target = null
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		target = null
		val thePlayer = mc.thePlayer ?: return

		if (classProvider.isItemBow(thePlayer.itemInUse?.item))
		{
			val entity = getTarget(thePlayer, throughWallsValue.get(), priorityValue.get()) ?: return

			target = entity
			RotationUtils.faceBow(thePlayer, entity, silentValue.get(), predictValue.get(), playerPredictValue.get(), minTurnSpeed.get(), maxTurnSpeed.get(), minAccelerationRatio.get(), maxAccelerationRatio.get())
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		if (target != null && !priorityValue.get().equals("Multi", ignoreCase = true) && markValue.get()) RenderUtils.drawPlatform(target!!, Color(37, 126, 255, 70))
	}

	private fun getTarget(thePlayer: IEntityPlayerSP, throughWalls: Boolean, priorityMode: String): IEntity?
	{
		val targets = (mc.theWorld ?: return null).loadedEntityList.asSequence().filter { classProvider.isEntityLivingBase(it) && EntityUtils.isSelected(it, true) && (throughWalls || thePlayer.canEntityBeSeen(it)) }

		return when (priorityMode.toLowerCase())
		{
			"distance" -> targets.minBy(thePlayer::getDistanceToEntity)
			"serverdirection" -> targets.minBy { RotationUtils.getServerRotationDifference(thePlayer, it) }
			"clientdirection" -> targets.minBy { RotationUtils.getClientRotationDifference(thePlayer, it) }
			"health" -> targets.minBy { it.asEntityLivingBase().health }
			else -> null
		}
	}

	fun hasTarget() = target != null && mc.thePlayer!!.canEntityBeSeen(target!!)
}
