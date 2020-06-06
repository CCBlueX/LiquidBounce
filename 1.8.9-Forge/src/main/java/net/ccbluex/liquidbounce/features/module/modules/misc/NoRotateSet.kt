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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook

@ModuleInfo(name = "NoRotateSet", description = "Prevents the server from rotating your head.", category = ModuleCategory.MISC)
class NoRotateSet : Module() {

    private val confirmValue = BoolValue("Confirm", true)
    private val illegalRotationValue = BoolValue("ConfirmIllegalRotation", false)
    private val noZeroValue = BoolValue("NoZero", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        mc.thePlayer ?: return

        if (packet is S08PacketPlayerPosLook) {
            if (noZeroValue.get() && packet.getYaw() == 0F && packet.getPitch() == 0F)
                return

            if (illegalRotationValue.get() || packet.getPitch() <= 90 && packet.getPitch() >= -90 &&
                    RotationUtils.serverRotation != null && packet.getYaw() != RotationUtils.serverRotation.yaw &&
                    packet.getPitch() != RotationUtils.serverRotation.pitch) {

                if (confirmValue.get())
                    mc.netHandler.addToSendQueue(C05PacketPlayerLook(packet.getYaw(), packet.getPitch(), mc.thePlayer.onGround))
            }

            packet.yaw = mc.thePlayer.rotationYaw
            packet.pitch = mc.thePlayer.rotationPitch
        }
    }

}