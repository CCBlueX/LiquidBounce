/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object NoRotateSet : Module("NoRotateSet", ModuleCategory.MISC) {
    private val confirm by BoolValue("Confirm", true)
    private val illegalRotation by BoolValue("ConfirmIllegalRotation", false) { confirm }
    private val noZero by BoolValue("NoZero", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (event.packet is S08PacketPlayerPosLook) {
            val packet = event.packet

            if (noZero && packet.yaw == 0F && packet.pitch == 0F)
                return

            if (illegalRotation || packet.pitch <= 90 && packet.pitch >= -90 &&
                    packet.yaw != serverRotation.yaw && packet.pitch != serverRotation.pitch) {

                if (confirm)
                    sendPacket(C05PacketPlayerLook(packet.yaw, packet.pitch, thePlayer.onGround))
            }

            packet.yaw = thePlayer.rotationYaw
            packet.pitch = thePlayer.rotationPitch
        }
    }

}