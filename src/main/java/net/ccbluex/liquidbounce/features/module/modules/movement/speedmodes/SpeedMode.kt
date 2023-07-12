/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.utils.MinecraftInstance

open class SpeedMode(val modeName: String) : MinecraftInstance() {
    open fun onMotion() {}
    open fun onUpdate() {}
    open fun onMove(event: MoveEvent) {}
    open fun onTick() {}
    open fun onEnable() {}
    open fun onDisable() {}

}