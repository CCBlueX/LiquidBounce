/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
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
	private val silentRotationValue = BoolValue("SilentRotation", true)

	private val predictValue = BoolValue("Predict", true)

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
	 * Should we aim through walls
	 */
	private val throughWallsValue = BoolValue("ThroughWalls", false)

	/**
	 * Target priority
	 */
	private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection"), "ServerDirection")

	/**
	 * Limit TurnSpeed
	 */
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 1f, 180f)
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) this.set(v)
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
	 * Mark target
	 */
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
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (classProvider.isItemBow(thePlayer.itemInUse?.item))
		{
			// Build the bit mask
			var flags = 0

			if (throughWallsValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK
			if (predictValue.get()) flags = flags or RotationUtils.ENEMY_PREDICT
			if (playerPredictValue.get()) flags = flags or RotationUtils.PLAYER_PREDICT
			if (silentRotationValue.get()) flags = flags or RotationUtils.SILENT_ROTATION

			val minPlayerPredictSize = minPlayerPredictSizeValue.get()
			val maxPlayerPredictSize = maxPlayerPredictSizeValue.get()

			val entity = getTarget(theWorld, thePlayer, priorityValue.get(), minPlayerPredictSize, maxPlayerPredictSize, flags) ?: return

			target = entity

			RotationUtils.faceBow(thePlayer, entity, minTurnSpeedValue.get(), maxTurnSpeedValue.get(), minAccelerationRatioValue.get(), maxAccelerationRatioValue.get(), minPlayerPredictSize, maxPlayerPredictSize, flags)
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val currentTarget = target

		if (currentTarget != null && !priorityValue.get().equals("Multi", ignoreCase = true) && markValue.get()) RenderUtils.drawPlatform(currentTarget, Color(37, 126, 255, 70))
	}

	private fun getTarget(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, priorityMode: String, minPlayerPredictSize: Float, maxPlayerPredictSize: Float, flags: Int): IEntity?
	{
		val ignoreVisibleCheck = flags and RotationUtils.SKIP_VISIBLE_CHECK != 0

		// The Target Candidates
		val targetCandidates = theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, true) }.filter { ignoreVisibleCheck || thePlayer.canEntityBeSeen(it) }

		val playerPredict = flags and RotationUtils.PLAYER_PREDICT != 0

		return when (priorityMode.toLowerCase())
		{
			"distance" -> targetCandidates.minBy(thePlayer::getDistanceToEntity)
			"serverdirection" -> targetCandidates.minBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }
			"clientdirection" -> targetCandidates.minBy { RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, minPlayerPredictSize, maxPlayerPredictSize) }
			"health" -> targetCandidates.minBy { it.asEntityLivingBase().health }
			else -> null
		}
	}

	fun hasTarget(thePlayer: IEntityPlayerSP): Boolean
	{
		val currentTarget = target

		return currentTarget != null && thePlayer.canEntityBeSeen(currentTarget)
	}
}
