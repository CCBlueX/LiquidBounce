/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import kotlin.math.cos
import kotlin.math.sin

class TeleportCubeCraft : SpeedMode("TeleportCubeCraft") {
    private val timer = MSTimer()
    override fun onMotion() {}
    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {
        if (MovementUtils.isMoving && mc.thePlayer!!.onGround && timer.hasTimePassed(300L)) {
            val yaw = MovementUtils.direction
            val length = (LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed?)!!.cubecraftPortLengthValue.get()
            event.x = -sin(yaw) * length
            event.z = cos(yaw) * length
            timer.reset()
        }
    }
}