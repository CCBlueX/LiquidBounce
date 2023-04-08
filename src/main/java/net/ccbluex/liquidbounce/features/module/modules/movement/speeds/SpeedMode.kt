/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.MinecraftInstance

abstract class SpeedMode(val modeName: String) : MinecraftInstance() {
    val isActive: Boolean
        get() = !mc.thePlayer.isSneaking && Speed.state && Speed.modeValue.get() == modeName

    abstract fun onMotion()
    abstract fun onUpdate()
    abstract fun onMove(event: MoveEvent)
    open fun onTick() {}
    open fun onEnable() {}
    open fun onDisable() {}

}