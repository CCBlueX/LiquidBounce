/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

object PacketDebugger : Module("PacketDebugger", Category.MISC, gameDetecting = false, hideModule = false) {

    private val notify by ListValue("Notify", arrayOf("Chat", "Notification"), "Chat")
    val packetType by ListValue("PacketType", arrayOf("Both", "Server", "Client", "Custom"), "Both")
    private val delay by IntegerValue("Delay", 100, 0..1000)

    private val timer = MSTimer()
    val selectedPackets = mutableListOf<String>()

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.player == null || mc.world == null) {
            return
        }

        val packet = event.packet

        val isServerPacket = packet.javaClass.name.startsWith("net.minecraft.network.play.server")
        val isClientPacket = packet.javaClass.name.startsWith("net.minecraft.network.packet.c2s.play")

        if (timer.hasTimePassed(delay)) {
            when (packetType.lowercase()) {
                "both" -> logPacket(event)
                "server" -> if (isServerPacket) logPacket(event)
                "client" -> if (isClientPacket) logPacket(event)
                "custom" -> if (selectedPackets.contains(packet.javaClass.simpleName)) logPacket(event)
            }
            timer.reset()
        }
    }

    private fun logPacket(event: PacketEvent) {
        val packet = event.packet

        val packetInfo = buildString {
            append("\n")
            append("§aPacket: §b${packet.javaClass.simpleName}\n")
            append("§aEventType: §b${event.eventType}\n")
            packet.javaClass.declaredFields.forEach { field ->
                field.isAccessible = true
                append("§a${field.name}: §b${field.get(packet)}\n")
            }
        }

        if (notify == "Chat") {
            Chat.print(packetInfo)
        } else {
            hud.addNotification(Notification(packetInfo, 3000F))
        }
    }
}