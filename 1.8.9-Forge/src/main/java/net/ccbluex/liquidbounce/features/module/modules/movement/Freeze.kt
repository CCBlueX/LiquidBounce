package net.ccbluex.liquidbounce.features.module.modules.movement

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
@ModuleInfo(name = "Frieren", description = "Allows you to stay stuck in mid air.", category = ModuleCategory.MOVEMENT)
class Freeze : Module() {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.thePlayer.isDead = true
        mc.thePlayer.rotationYaw = mc.thePlayer.cameraYaw
        mc.thePlayer.rotationPitch = mc.thePlayer.cameraPitch
    }

    override fun onDisable() {
        mc.thePlayer?.isDead = false
    }
}
