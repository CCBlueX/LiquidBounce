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
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceTrajectory
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemEgg
import net.minecraft.item.ItemEnderPearl
import net.minecraft.item.ItemSnowball
import java.awt.Color

object BowAimbot : Module("BowAimbot", ModuleCategory.COMBAT) {

    private val bow by BoolValue("Bow", true, subjective = true)
    private val egg by BoolValue("Egg", true, subjective = true)
    private val snowball by BoolValue("Snowball", true, subjective = true)
    private val pearl by BoolValue("EnderPearl", false, subjective = true)

    private val priority by ListValue("Priority",
        arrayOf("Health", "Distance", "Direction"),
        "Direction",
        subjective = true
    )

    private val predict by BoolValue("Predict", true)
    private val predictSize by FloatValue("PredictSize", 2F, 0.1F..5F) { predict }

    private val throughWalls by BoolValue("ThroughWalls", false, subjective = true)
    private val mark by BoolValue("Mark", true, subjective = true)

    private val silent by BoolValue("Silent", true)
    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { silent }
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative")
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 120f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)

        override fun isSupported() = silent
    }
    private val maxTurnSpeed by maxTurnSpeedValue

    private val minTurnSpeed by object : FloatValue("MinTurnSpeed", 80f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxTurnSpeed)

        override fun isSupported() = !maxTurnSpeedValue.isMinimal() && silent
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { silent }

    private var target: Entity? = null

    override fun onDisable() {
        target = null
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST)
            return

        target = null
        var targetRotation: Rotation? = null

        when (val item = mc.thePlayer.heldItem?.item) {
            is ItemBow -> {
                if (!bow || !mc.thePlayer.isUsingItem)
                    return

                target = getTarget(throughWalls, priority)

                targetRotation = faceTrajectory(target ?: return, predict, predictSize)
            }

            is ItemEgg, is ItemSnowball, is ItemEnderPearl -> {
                if (!egg && item is ItemEgg || !snowball && item is ItemSnowball || !pearl && item is ItemEnderPearl)
                    return

                target = getTarget(throughWalls, priority)

                targetRotation = faceTrajectory(target ?: return,
                    predict,
                    predictSize,
                    gravity = 0.03f,
                    velocity = 0.5f
                )
            }
        }

        val limitedRotation = limitAngleChange(
            currentRotation ?: mc.thePlayer.rotation,
            targetRotation ?: return,
            nextFloat(minTurnSpeed, maxTurnSpeed),
            smootherMode
        )

        if (silent) {
            setTargetRotation(
                limitedRotation,
                strafe = strafe != "Off",
                strict = strafe == "Strict",
                resetSpeed = minTurnSpeed to maxTurnSpeed,
                angleThresholdForReset = angleThresholdUntilReset,
                smootherMode = this.smootherMode
            )
        } else {
            limitedRotation.toPlayer(mc.thePlayer)
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (target != null && priority != "Multi" && mark)
            drawPlatform(target!!, Color(37, 126, 255, 70))
    }

    private fun getTarget(throughWalls: Boolean, priorityMode: String): Entity? {
        val targets = mc.theWorld.loadedEntityList.filter {
            it is EntityLivingBase && isSelected(it, true) && (throughWalls || mc.thePlayer.canEntityBeSeen(it))
        }

        return when (priorityMode.uppercase()) {
            "DISTANCE" -> targets.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) }
            "DIRECTION" -> targets.minByOrNull { getRotationDifference(it) }
            "HEALTH" -> targets.minByOrNull { (it as EntityLivingBase).health }
            else -> null
        }
    }

    fun hasTarget() = target != null && mc.thePlayer.canEntityBeSeen(target)
}
