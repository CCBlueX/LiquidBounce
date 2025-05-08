package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket

object ModuleNoSlotSet : ClientModule("NoSlotSet", Category.PLAYER) {
    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet !is UpdateSelectedSlotS2CPacket) {
            return@handler
        }

        event.cancelEvent()
        player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(SilentHotbar.serversideSlot))
    }
}
