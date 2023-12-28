package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.strafe

object JsMovementUtil {

    fun strafe() = player.strafe()

    fun strafeSpeed(speed: Double) = player.strafe(speed.toFloat())

    fun velocity() = player.velocity

    fun addVelocity(x: Double, y: Double, z: Double) = player.addVelocity(x, y, z)

    fun stopVelocity() = player.setVelocity(0.0, 0.0, 0.0)

    fun jump() = player.jump()

}
