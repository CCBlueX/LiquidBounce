/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.player.EntityPlayer

object NoRotateSet : Module("NoRotateSet", Category.MISC, gameDetecting = false, hideModule = false) {
    var savedRotation = Rotation(0f, 0f)

    private val ignoreOnSpawn by BoolValue("IgnoreOnSpawn", false)
    val affectRotation by BoolValue("AffectRotation", true)

    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { affectRotation }
    private val smootherMode by ListValue("SmootherMode",
        arrayOf("Linear", "Relative"),
        "Relative"
    ) { affectRotation }

    private val simulateShortStop by BoolValue("SimulateShortStop", false) { affectRotation }
    private val startRotatingSlow by BoolValue("StartRotatingSlow", false) { affectRotation }
    private val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false) { affectRotation }
    private val useStraightLinePath by BoolValue("UseStraightLinePath", true) { affectRotation }
    private val maxHorizontalSpeedValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed)
        override fun isSupported() = affectRotation
    }
    private val maxHorizontalSpeed by maxHorizontalSpeedValue

    private val minHorizontalSpeed: Float by object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed)
        override fun isSupported() = !maxHorizontalSpeedValue.isMinimal() && affectRotation
    }

    private val maxVerticalSpeedValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed)
        override fun isSupported() = affectRotation
    }
    private val maxVerticalSpeed by maxVerticalSpeedValue

    private val minVerticalSpeed: Float by object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed)
        override fun isSupported() = !maxVerticalSpeedValue.isMinimal() && affectRotation
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset",
        5f,
        0.1f..180f
    ) { affectRotation }

    fun shouldModify(player: EntityPlayer) = handleEvents() && (!ignoreOnSpawn || player.ticksExisted != 0)

    fun rotateBackToPlayerRotation() {
        val player = mc.thePlayer ?: return

        currentRotation = player.rotation

        setTargetRotation(
            savedRotation,
            strafe = strafe != "Off",
            strict = strafe == "Strict",
            applyClientSide = false,
            turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
            angleThresholdForReset = angleThresholdUntilReset,
            smootherMode = smootherMode,
            prioritizeRequest = true,
            simulateShortStop = simulateShortStop,
            startOffSlow = startRotatingSlow,
            slowDownOnDirChange = slowDownOnDirectionChange,
            useStraightLinePath = useStraightLinePath
        )
    }
}