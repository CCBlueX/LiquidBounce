package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.screen.Screen
import net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket

@Suppress("unused")
object ModuleNoBooks : ClientModule("NoBooks", Category.MISC) {

    private var shouldCloseBook = false

    private val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->
        if (event.packet is OpenWrittenBookS2CPacket) {
            event.cancelEvent()
            shouldCloseBook = true
        }
    }

    private val tickHandler = tickHandler {
        if (shouldCloseBook && mc.currentScreen != null) {
            val screen: Screen? = mc.currentScreen
            mc.execute {
                if (mc.currentScreen == screen) {
                    player.closeHandledScreen()
                }
            }
            shouldCloseBook = false
        }
    }
}
