package net.ccbluex.liquidbounce.features.special

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.network.play.client.C09PacketHeldItemChange

class Manager : Listenable, MinecraftInstance() {
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C09PacketHeldItemChange) {
            InventoryUtils.currentSlot = event.packet.slotId
        }
    }

    override fun handleEvents() = true
}