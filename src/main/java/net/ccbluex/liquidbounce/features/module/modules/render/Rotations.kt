/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(
    name = "Rotations",
    description = "Allows you to see server-sided head and body rotations.",
    category = ModuleCategory.RENDER
)
class Rotations : Module() {

    private val bodyValue = BoolValue("Body", true)

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (!shouldRotate()) {
            return
        }

        thePlayer.rotationYawHead = RotationUtils.serverRotation.yaw

        if (bodyValue.get()) {
            thePlayer.renderYawOffset = thePlayer.rotationYawHead
        }
    }

    private fun getState(module: Class<*>) = LiquidBounce.moduleManager[module].state

    /**
     * Rotate when current rotation is not null or special modules which do not make use of RotationUtils like Derp are enabled.
     */
    fun shouldRotate(): Boolean {
        return state && (getState(Derp::class.java) || RotationUtils.targetRotation != null)
    }
}
