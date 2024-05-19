/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object Freeze : Module("Freeze", Category.MOVEMENT) {
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    override fun onEnable() {
        player ?: return

        x = player.posX
        y = player.posY
        z = player.posZ
        motionX = player.motionX
        motionY = player.motionY
        motionZ = player.motionZ
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        player.motionX = 0.0
        player.motionY = 0.0
        player.motionZ = 0.0
        player.setPositionAndRotation(x, y, z, player.rotationYaw, player.rotationPitch)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            event.cancelEvent()
        }
        if (event.packet is S08PacketPlayerPosLook) {
            x = event.packet.x
            y = event.packet.y
            z = event.packet.z
            motionX = 0.0
            motionY = 0.0
            motionZ = 0.0
        }
    }

    override fun onDisable() {
        player.motionX = motionX
        player.motionY = motionY
        player.motionZ = motionZ
        player.setPositionAndRotation(x, y, z, player.rotationYaw, player.rotationPitch)
    }
}
