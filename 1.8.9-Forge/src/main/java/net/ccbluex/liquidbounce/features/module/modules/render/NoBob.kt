package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "KeinBob", description = "Disables the view bobbing effect.", category = ModuleCategory.RENDER)
class NoBob : Module() {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.thePlayer.distanceWalkedModified = 0f
    }
}
