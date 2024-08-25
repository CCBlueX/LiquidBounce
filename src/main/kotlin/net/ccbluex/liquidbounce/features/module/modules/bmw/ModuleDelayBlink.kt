package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.network.packet.Packet

object ModuleDelayBlink : Module("DelayBlink", Category.BMW, disableOnQuit = true) {

    private val delay by int("Delay", 20, 0..200, "ticks")
    private val displayDelay by boolean("DisplayDelay", true)

    private val packets = mutableListOf<Packet<*>>()
    private var ticks = 0

    val gameTickEventHandler = handler<GameTickEvent> {
        ticks++
        if (ticks > delay) {
            ticks = delay
        }
        if (displayDelay) {
            notifyAsMessage("Blink Delay: $ticks / $delay")
        }
    }

    val packetEventHandler = handler<PacketEvent> { event ->
        if (event.origin == TransferOrigin.RECEIVE) {
            return@handler
        }

        packets.add(event.packet)
        event.cancelEvent()
        if (ticks >= delay) {
            sendPacketSilently(packets.removeFirst())
        }
    }

    override fun enable() {
        packets.clear()
        ticks = 0
    }

    override fun disable() {
        packets.forEach {
            sendPacketSilently(it)
        }
    }
}
