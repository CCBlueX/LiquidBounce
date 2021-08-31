/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.ValueGroup

@ModuleInfo(name = "BowAimbot", description = "Automatically aims at players when using a bow.", category = ModuleCategory.COMBAT)
class BowAimbot : Module()
{
	private val silentRotationValue = BoolValue("SilentRotation", true)

	private val predictGroup = ValueGroup("Predict")

	private val predictEnemyValue = BoolValue("Enemy", true, "Predict")

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

	/**
	 * Should we aim through walls
	 */
	private val throughWallsValue = BoolValue("ThroughWalls", false)

	/**
	 * Target priority
	 */
	private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection"), "ServerDirection")

	private val turnSpeedGroup = ValueGroup("TurnSpeed")
	private val maxTurnSpeedValue: FloatValue = object : FloatValue("Max", 180f, 1f, 180f, "MaxTurnSpeed")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = minTurnSpeedValue.get()
			if (v > newValue) this.set(v)
		}
	}
	private val minTurnSpeedValue: FloatValue = object : FloatValue("Min", 180f, 1f, 180f, "MinTurnSpeed")
	{
		override fun onChanged(oldValue: Float, newValue: Float)
		{
			val v = maxTurnSpeedValue.get()
			if (v < newValue) this.set(v)
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

	/**
	 * Mark target
	 */
	private val markValue = BoolValue("Mark", true)

	var target: IEntityLivingBase? = null

	init
	{
		predictGroup.addAll(predictEnemyValue, predictPlayerGroup)
		predictPlayerGroup.addAll(playerPredictValue, maxPlayerPredictSizeValue, minPlayerPredictSizeValue)
		turnSpeedGroup.addAll(maxTurnSpeedValue, minTurnSpeedValue)
		accelerationGroup.addAll(maxAccelerationRatioValue, minAccelerationRatioValue)
	}

	override fun onDisable()
	{
		target = null
	}

	@EventTarget
	fun onMotion(@Suppress("UNUSED_PARAMETER") event: MotionEvent)
	{
		if (event.eventState != EventState.PRE) return

		target = null
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (classProvider.isItemBow(thePlayer.itemInUse?.item))
		{
			// Build the bit mask
			var flags = 0

			if (throughWallsValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK
			if (predictEnemyValue.get()) flags = flags or RotationUtils.ENEMY_PREDICT
			if (playerPredictValue.get()) flags = flags or RotationUtils.PLAYER_PREDICT
			if (silentRotationValue.get()) flags = flags or RotationUtils.SILENT_ROTATION
			val playerPredictSize = RotationUtils.MinMaxPair(minPlayerPredictSizeValue.get(), maxPlayerPredictSizeValue.get())

			val entity = getTarget(theWorld, thePlayer, priorityValue.get(), playerPredictSize, flags) ?: return

			target = entity

			RotationUtils.faceBow(thePlayer, entity, RotationUtils.MinMaxPair(minTurnSpeedValue.get(), maxTurnSpeedValue.get()), RotationUtils.MinMaxPair(minAccelerationRatioValue.get(), maxAccelerationRatioValue.get()), playerPredictSize, flags)
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val currentTarget = target

		if (currentTarget != null && !priorityValue.get().equals("Multi", ignoreCase = true) && markValue.get()) RenderUtils.drawPlatform(currentTarget, 0x46257EFF)
	}

	private fun getTarget(theWorld: IWorldClient, thePlayer: IEntityLivingBase, priorityMode: String, playerPredictSize: RotationUtils.MinMaxPair, flags: Int): IEntityLivingBase?
	{
		val ignoreVisibleCheck = flags and RotationUtils.SKIP_VISIBLE_CHECK != 0

		// The Target Candidates
		val targetCandidates = theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, true) }.filter { ignoreVisibleCheck || thePlayer.canEntityBeSeen(it) }.map(IEntity::asEntityLivingBase)

		val playerPredict = flags and RotationUtils.PLAYER_PREDICT != 0

		return when (priorityMode.toLowerCase())
		{
			"distance" -> targetCandidates.minBy(thePlayer::getDistanceToEntity)
			"serverdirection" -> targetCandidates.minBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) }
			"clientdirection" -> targetCandidates.minBy { RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, playerPredictSize) }
			"health" -> targetCandidates.minBy { it.asEntityLivingBase().health }
			else -> null
		}
	}

	fun hasTarget(thePlayer: IEntityLivingBase): Boolean
	{
		val currentTarget = target

		return currentTarget != null && thePlayer.canEntityBeSeen(currentTarget)
	}
}
