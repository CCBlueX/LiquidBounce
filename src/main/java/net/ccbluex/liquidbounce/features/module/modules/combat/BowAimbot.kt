/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatRangeValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBow

@ModuleInfo(name = "BowAimbot", description = "Automatically aims at players when using a bow.", category = ModuleCategory.COMBAT)
class BowAimbot : Module()
{
    private val silentRotationValue = BoolValue("SilentRotation", true)

    private val predictGroup = ValueGroup("Predict")

    private val predictEnemyEnabledValue = BoolValue("Enemy", true, "Predict")

    private val predictPlayerGroup = ValueGroup("Player")
    private val predictPlayerEnabledValue = BoolValue("Enabled", true, "PlayerPredict")
    private val predictPlayerIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPlayerPredictSize" to "MinPlayerPredictSize")

    /**
     * Should we aim through walls
     */
    private val throughWallsValue = BoolValue("ThroughWalls", false)

    /**
     * Target priority
     */
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "ServerDirection", "ClientDirection"), "ServerDirection")

    private val accelerationRatioValue = FloatRangeValue("Acceleration", 0f, 0f, 0f, .99f, "MaxAccelerationRatio" to "MinAccelerationRatio")
    private val turnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 1f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
    private val resetSpeedValue = object : FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")
    {
        override fun showCondition() = silentRotationValue.get()
    }

    /**
     * Mark target
     */
    private val markValue = BoolValue("Mark", true)

    var target: EntityLivingBase? = null

    init
    {
        predictGroup.addAll(predictEnemyEnabledValue, predictPlayerGroup)
        predictPlayerGroup.addAll(predictPlayerEnabledValue, predictPlayerIntensityValue)
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

        if (thePlayer.itemInUse?.item is ItemBow)
        {
            // Build the bit mask
            var flags = 0

            if (throughWallsValue.get()) flags = flags or RotationUtils.SKIP_VISIBLE_CHECK
            if (predictEnemyEnabledValue.get()) flags = flags or RotationUtils.ENEMY_PREDICT
            if (predictPlayerEnabledValue.get()) flags = flags or RotationUtils.PLAYER_PREDICT
            if (silentRotationValue.get()) flags = flags or RotationUtils.SILENT_ROTATION
            val playerPredictSize = RotationUtils.MinMaxPair(predictPlayerIntensityValue.getMin(), predictPlayerIntensityValue.getMax())

            val entity = getTarget(theWorld, thePlayer, priorityValue.get(), playerPredictSize, flags) ?: return

            target = entity

            RotationUtils.faceBow(thePlayer, entity, RotationUtils.MinMaxPair(turnSpeedValue.getMin(), turnSpeedValue.getMax()), RotationUtils.MinMaxPair(accelerationRatioValue.getMin(), accelerationRatioValue.getMax())/*, RotationUtils.MinMaxPair(resetSpeedValue.getMin(), resetSpeedValue.getMax())*/, playerPredictSize, flags)
        }
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val currentTarget = target

        if (currentTarget != null && markValue.get()) RenderUtils.drawPlatform(currentTarget, 0x46257EFF, event.partialTicks)
    }

    private fun getTarget(theWorld: WorldClient, thePlayer: EntityLivingBase, priorityMode: String, playerPredictSize: RotationUtils.MinMaxPair, flags: Int): EntityLivingBase?
    {
        val ignoreVisibleCheck = flags and RotationUtils.SKIP_VISIBLE_CHECK != 0

        // The Target Candidates
        val targetCandidates = theWorld.loadedEntityList.asSequence().filterIsInstance<EntityLivingBase>().filter { it.isSelected(true) }.filter { ignoreVisibleCheck || thePlayer.canEntityBeSeen(it) }

        val playerPredict = flags and RotationUtils.PLAYER_PREDICT != 0

        return when (priorityMode.lowercase())
        {
            "distance" -> targetCandidates.minByOrNull(thePlayer::getDistanceToEntity)
            "serverdirection" -> targetCandidates.minByOrNull { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) }
            "clientdirection" -> targetCandidates.minByOrNull { RotationUtils.getClientRotationDifference(thePlayer, it, playerPredict, playerPredictSize) }
            "health" -> targetCandidates.minByOrNull { it.health }
            else -> null
        }
    }

    fun hasTarget(thePlayer: EntityLivingBase): Boolean
    {
        val currentTarget = target

        return currentTarget != null && (throughWallsValue.get() || thePlayer.canEntityBeSeen(currentTarget))
    }
}
