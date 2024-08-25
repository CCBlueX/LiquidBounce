/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket

object NoBooks : Module("NoBooks", Category.RENDER, gameDetecting = false, hideModule = false) {
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is CustomPayloadS2CPacket && packet.channel == "MC|BOpen") event.cancelEvent()
    }
}