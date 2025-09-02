package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket

object ModuleNoBooks : ClientModule("NoBooks", Category.MISC) {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->
        if (event.packet is OpenWrittenBookS2CPacket) {
            event.cancelEvent()

            mc.execute {
                if (player.currentScreenHandler.syncId == event.packet.hand.ordinal) {
                    player.closeHandledScreen()
                }
            }
        }
    }
}
