/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.value.BoolValue

object Rotations : Module("Rotations", Category.RENDER, gameDetecting = false, hideModule = false) {

    private val realistic by BoolValue("Realistic", true)
    private val body by BoolValue("Body", true) { !realistic }

    val debugRotations by BoolValue("DebugRotations", false)
    val experimentalCurve by BoolValue("ExperimentalLinearCurveRotation", false)
    val startSecondRotationSlow by BoolValue("StartSecondRotationSlow", false)
    val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false)
    val useStraightLinePath by BoolValue("UseStraightLinePath", true)
    
    var prevHeadPitch = 0f
    var headPitch = 0f

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        prevHeadPitch = headPitch
        headPitch = serverRotation.pitch

        if (!shouldRotate() || realistic) {
            return
        }

        thePlayer.rotationYawHead = serverRotation.yaw

        if (body) {
            thePlayer.renderYawOffset = thePlayer.rotationYawHead
        }
    }

    fun lerp(tickDelta: Float, old: Float, new: Float): Float {
        return old + (new - old) * tickDelta
    }

    /**
     * Rotate when current rotation is not null or special modules which do not make use of RotationUtils like Derp are enabled.
     */
    fun shouldRotate() = state && (Derp.handleEvents() || currentRotation != null)

    /**
     * Imitate the game's head and body rotation logic
     */
    fun shouldUseRealisticMode() = realistic && shouldRotate()

    /**
     * Which rotation should the module use?
     */
    fun getRotation() = if (Derp.handleEvents()) serverRotation else currentRotation
}
