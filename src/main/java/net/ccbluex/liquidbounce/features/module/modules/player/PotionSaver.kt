/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.minecraft.network.play.client.C03PacketPlayer

object PotionSaver : Module("PotionSaver", ModuleCategory.PLAYER) {

    @EventTarget
    fun onPacket(e: PacketEvent) {
        val packet = e.packet

        if (packet is C03PacketPlayer && mc.thePlayer?.isUsingItem == false && !packet.rotating &&
            (!packet.isMoving || (packet.x == mc.thePlayer.lastTickPosX && packet.y == mc.thePlayer.lastTickPosY && packet.z == mc.thePlayer.lastTickPosZ)))
            e.cancelEvent()
    }

}