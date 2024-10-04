/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.RotationUpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceTrajectory
import net.ccbluex.liquidbounce.utils.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
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

object BowAimbot : Module("BowAimbot", Category.COMBAT, hideModule = false) {

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

    private val simulateShortStop by BoolValue("SimulateShortStop", false)

    private val startRotatingSlow by BoolValue("StartRotatingSlow", false)

    private val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false)
    private val useStraightLinePath by BoolValue("UseStraightLinePath", true)

    private val maxHorizontalSpeedValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed)
    }
    private val maxHorizontalSpeed by maxHorizontalSpeedValue

    private val minHorizontalSpeed: Float by object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed)
        override fun isSupported() = !maxHorizontalSpeedValue.isMinimal()
    }

    private val maxVerticalSpeedValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed)
    }
    private val maxVerticalSpeed by maxVerticalSpeedValue

    private val minVerticalSpeed: Float by object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed)
        override fun isSupported() = !maxVerticalSpeedValue.isMinimal()
    }
    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f)
    private val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..2f)

    private var target: Entity? = null

    override fun onDisable() {
        target = null
    }

    @EventTarget
    fun onRotationUpdate(event: RotationUpdateEvent) {
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

        setTargetRotation(
            targetRotation ?: return,
            strafe = silent && strafe != "Off",
            strict = silent && strafe == "Strict",
            applyClientSide = !silent,
            turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
            angleThresholdForReset = angleThresholdUntilReset,
            smootherMode = smootherMode,
            simulateShortStop = simulateShortStop,
            startOffSlow = startRotatingSlow,
            slowDownOnDirChange = slowDownOnDirectionChange,
            useStraightLinePath = useStraightLinePath,
            minRotationDifference = minRotationDifference
        )
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (target != null && priority != "Multi" && mark) {
            drawPlatform(target!!, Color(37, 126, 255, 70))
        }
    }

    private fun getTarget(throughWalls: Boolean, priorityMode: String): Entity? {
        val targets = mc.theWorld.loadedEntityList.filter {
            it is EntityLivingBase && isSelected(it, true) && (throughWalls || mc.thePlayer.canEntityBeSeen(it))
        }

        return when (priorityMode.uppercase()) {
            "DISTANCE" -> targets.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) }
            "DIRECTION" -> targets.minByOrNull { rotationDifference(it) }
            "HEALTH" -> targets.minByOrNull { (it as EntityLivingBase).health }
            else -> null
        }
    }

    fun hasTarget() = target != null && mc.thePlayer.canEntityBeSeen(target)
}
