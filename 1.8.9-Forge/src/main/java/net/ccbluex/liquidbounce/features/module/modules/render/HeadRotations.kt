/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils

@ModuleInfo(name = "HeadRotations", description = "Allows you to see server-sided head rotations.", category = ModuleCategory.RENDER)
class HeadRotations : Module() {

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (RotationUtils.serverRotation != null)
            mc.thePlayer.rotationYawHead = RotationUtils.serverRotation.yaw
    }

}
