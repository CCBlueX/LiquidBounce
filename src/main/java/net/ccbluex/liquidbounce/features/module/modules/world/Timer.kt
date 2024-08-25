/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

object Timer : Module("Timer", Category.WORLD, gameDetecting = false, hideModule = false) {

    private val mode by ListValue("Mode", arrayOf("OnMove", "NoMove", "Always"), "OnMove")
    private val speed by FloatValue("Speed", 2F, 0.1F..10F)

    override fun onDisable() {
        if (mc.player == null)
            return

        mc.timer.timerSpeed = 1F
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mode == "Always" || mode == "OnMove" && isMoving || mode == "NoMove" && !isMoving) {
            mc.timer.timerSpeed = speed
            return
        }

        mc.timer.timerSpeed = 1F
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (event.worldClient != null)
            return

        state = false
    }
}
