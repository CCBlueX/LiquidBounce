/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.minecraft.network.play.client.C03PacketKeepAlive

class AirWalk : FlyMode("AirWalk") {
    override fun onMotion() {}

    override fun onEnable() {}

    override fun onDisable() {
        super.onDisable()
    }
    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) event.packet.onGround = true
    }
    override fun onUpdate() {
        mc.thePlayer.motionY = 0.0
    }
    override fun onMove(event: MoveEvent) {}
}
