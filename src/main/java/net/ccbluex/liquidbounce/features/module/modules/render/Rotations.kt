/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.targetRotation
import net.ccbluex.liquidbounce.value.BoolValue

object Rotations : Module("Rotations", ModuleCategory.RENDER) {

    private val body by BoolValue("Body", true)

    var prevHeadPitch = 0f
    var headPitch = 0f

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        prevHeadPitch = headPitch
        headPitch = serverRotation.pitch

        if (!shouldRotate()) {
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
    fun shouldRotate() = state && (Derp.state || targetRotation != null)
}
