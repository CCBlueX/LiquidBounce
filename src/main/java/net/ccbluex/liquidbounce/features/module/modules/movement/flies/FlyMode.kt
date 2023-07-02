/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.utils.MinecraftInstance

abstract class FlyMode(val flyModeName: String) : MinecraftInstance() {
    abstract fun onMotion()
    abstract fun onUpdate()
    abstract fun onMove(event: MoveEvent)
    open fun onTick() {}
    open fun onEnable() {}
    open fun onDisable() {}

}
