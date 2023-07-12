/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.sin
import kotlin.math.cos

class Redesky : FlyMode("Redesky") {
    override fun onMotion() {}

    override fun onEnable() {
        if (mc.thePlayer.onGround) { redeskyVClip1(Fly.redeskyHeight) }
    }

    override fun onDisable() {
        redeskyHClip2(0.0)
        super.onDisable()
    }

    override fun onUpdate() {
        mc.timer.timerSpeed = 0.3f
        redeskyHClip2(7.0)
        redeskyVClip2(10.0)
        redeskyVClip1(-0.5f)
        redeskyHClip1(2.0)
        redeskySpeed(1)
        mc.thePlayer.motionY = -0.01
    }

    override fun onMove(event: MoveEvent) {}


    private fun redeskyHClip1(horizontal: Double) {
        val playerYaw = mc.thePlayer.rotationYaw.toRadiansD()
        mc.thePlayer.setPosition(
            mc.thePlayer.posX + horizontal * -sin(playerYaw),
            mc.thePlayer.posY,
            mc.thePlayer.posZ + horizontal * cos(playerYaw)
        )
    }
    private fun redeskyHClip2(horizontal: Double) {
        val playerYaw = mc.thePlayer.rotationYaw.toRadiansD()
        sendPacket(
            C04PacketPlayerPosition(
                mc.thePlayer.posX + horizontal * -sin(playerYaw),
                mc.thePlayer.posY,
                mc.thePlayer.posZ + horizontal * cos(playerYaw), false
            )
        )
    }
    
    private fun redeskyVClip1(vertical: Float) {
        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + vertical, mc.thePlayer.posZ)
    }
    private fun redeskyVClip2(vertical: Double) =
    sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + vertical, mc.thePlayer.posZ, false))

    private fun redeskySpeed(speed: Int) {
        val playerYaw = mc.thePlayer.rotationYaw.toRadiansD()
        mc.thePlayer.motionX = speed * -sin(playerYaw)
        mc.thePlayer.motionZ = speed * cos(playerYaw)
    }
}
