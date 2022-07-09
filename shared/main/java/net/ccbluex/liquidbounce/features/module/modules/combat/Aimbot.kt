/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.getEntitiesInRadius
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import kotlin.math.abs

@ModuleInfo(name = "Aimbot", description = "Automatically faces selected entities around you.", category = ModuleCategory.COMBAT)
class Aimbot : Module()
{
    private val rangeValue = FloatValue("Range", 4.4F, 1F, 8F)

    private val rotationAccelerationRatioValue = FloatRangeValue("Acceleration", 0f, 0f, 0f, .99f, "MaxAccelerationRatio" to "MinAccelerationRatio")
    private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 1f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")

    private val fovValue = FloatValue("FoV", 30F, 1F, 180F)

    private val predictGroup = ValueGroup("Predict")
    private val predictEnemyGroup = ValueGroup("Enemy")
    private val predictEnemyValue = BoolValue("Enabled", true, description = "Predicts the next position of target")
    private val predictEnemyIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPredictSize" to "MinPredictSize")

    private val predictPlayerGroup = ValueGroup("Player")
    private val playerPredictValue = BoolValue("Enabled", true, description = "Predicts the nex position of player")
    private val predictPlayerIntensityValue = FloatRangeValue("Intensity", 1f, 1f, -2f, 2f, "MaxPlayerPredictSize" to "MinPlayerPredictSize")

    /**
     * Should we aim through walls?
     */
    private val throughWallsValue = BoolValue("ThroughWalls", false, description = "Disable visible checks")

    /**
     * Lock Center
     */
    private val centerValue = BoolValue("Center", false, description = "Lock the aim to the center of target hitbox")

    /**
     * Lock rotation
     */
    private val lockValue = BoolValue("Lock", true, description = "Always aim the target")

    private val onClickGroup = ValueGroup("OnClick")
    private val onClickValue = BoolValue("Enabled", false, description = "Only aim while LMB is pressed")
    private val onClickKeepValue = IntegerValue("KeepTime", 500, 0, 1000)

    private val jitterGroup = ValueGroup("Jitter")
    private val jitterValue = BoolValue("Enabled", false, description = "Shake your aim (to make your aim looks more legit)")
    private val jitterRateYaw = IntegerValue("YawRate", 50, 1, 100)
    private val jitterRatePitch = IntegerValue("PitchRate", 50, 1, 100)
    private val jitterYawIntensityValue = FloatRangeValue("YawIntensity", 0f, 1f, 0f, 5f, "MaxYawStrength" to "MinYawStrength")
    private val jitterPitchIntensityValue = FloatRangeValue("PitchIntensity", 0f, 1f, 0f, 5f, "MaxPitchStrength" to "MinPitchStrength")

    private val searchCenterGroup = ValueGroup("SearchCenter")
    private val hitboxDecrementValue = FloatValue("Shrink", 0.1f, 0.15f, 0.45f)
    private val centerSearchSensitivityValue = IntegerValue("Steps", 8, 4, 20)

    private val frictionGroup = ValueGroup("Friction")
    private val aimFrictionValue = FloatValue("Friction", 0.6F, 0.3F, 1F)
    private val aimFrictionTimingValue = ListValue("Timing", arrayOf("Before", "After"), "Before")
    private val resetThresoldValue = FloatValue("UnlockThreshold", 0.8F, 0.5F, 2F)

    private val clickTimer = MSTimer()

    var target: IEntityLivingBase? = null

    private var yawMovement = 0F
    private var pitchMovement = 0F

    init
    {
        predictEnemyGroup.addAll(predictEnemyValue, predictEnemyIntensityValue)
        predictPlayerGroup.addAll(playerPredictValue, predictPlayerIntensityValue)
        predictGroup.addAll(predictEnemyGroup, predictPlayerGroup)
        onClickGroup.addAll(onClickValue, onClickKeepValue)
        jitterGroup.addAll(jitterValue, jitterRateYaw, jitterRatePitch, jitterYawIntensityValue, jitterPitchIntensityValue)
        searchCenterGroup.addAll(hitboxDecrementValue, centerSearchSensitivityValue)
        frictionGroup.addAll(aimFrictionValue, aimFrictionTimingValue, resetThresoldValue)
    }

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
        val playerPredictSize = RotationUtils.MinMaxPair(predictPlayerIntensityValue.getMin(), predictPlayerIntensityValue.getMax())

        val jitter = jitterValue.get()

        target = theWorld.getEntitiesInRadius(thePlayer, range + 2.0).asSequence().filter { it.isSelected(true) }.filter { thePlayer.getDistanceToEntityBox(it) <= range }.run { if (fov < 180F) filter { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) <= fov } else this }.run { if (throughWalls) this else filter(thePlayer::canEntityBeSeen) }.minBy { RotationUtils.getServerRotationDifference(thePlayer, it, playerPredict, playerPredictSize) }?.asEntityLivingBase()

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
        val jitterData = if (jitter) RotationUtils.JitterData(jitterRateYaw.get(), jitterRatePitch.get(), jitterYawIntensityValue.getMin(), jitterYawIntensityValue.getMax(), jitterPitchIntensityValue.getMin(), jitterPitchIntensityValue.getMax()) else null

        // Apply predict to target box

        var targetBB = entity.entityBoundingBox

        if (predictEnemyValue.get())
        {
            val xPredict = (entity.posX - entity.lastTickPosX) * predictEnemyIntensityValue.getRandom()
            val yPredict = (entity.posY - entity.lastTickPosY) * predictEnemyIntensityValue.getRandom()
            val zPredict = (entity.posZ - entity.lastTickPosZ) * predictEnemyIntensityValue.getRandom()

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
        if (predictEnemyValue.get()) flags = flags or RotationUtils.ENEMY_PREDICT

        val targetRotation = (RotationUtils.searchCenter(theWorld, thePlayer, targetBB, flags, jitterData, playerPredictSize, range, hitboxDecrementValue.get().toDouble(), centerSearchSensitivityValue.get(), 0.0) ?: return).rotation

        val turnSpeed = rotationTurnSpeedValue.getRandomStrict()
        val acceleration = rotationAccelerationRatioValue.getRandomStrict()

        // Limit by TurnSpeed any apply
        val limitedRotation = RotationUtils.limitAngleChange(currentRotation, targetRotation, turnSpeed, acceleration)

        yawMovement = limitedRotation.yaw - currentRotation.yaw
        pitchMovement = limitedRotation.pitch - currentRotation.pitch

        // Re-use local variable 'currentRotation'
        currentRotation.yaw += yawMovement
        currentRotation.pitch += pitchMovement

        currentRotation.applyRotationToPlayer(thePlayer)
    }

    private fun fadeRotations(thePlayer: IEntityPlayer)
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
