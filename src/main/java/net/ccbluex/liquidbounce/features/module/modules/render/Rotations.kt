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

    var prevHeadPitch = 0f
    var headPitch = 0f

    @EventTarget
    fun onMotion(event: MotionEvent) {
        player ?: return

        prevHeadPitch = headPitch
        headPitch = serverRotation.pitch

        if (!shouldRotate() || realistic) {
            return
        }

        player.rotationYawHead = serverRotation.yaw

        if (body) {
            player.renderYawOffset = player.rotationYawHead
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