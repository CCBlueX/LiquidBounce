/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S38PacketPlayerListItem.Action.UPDATE_LATENCY

object AntiVanish : Module("AntiVanish", Category.MISC, gameDetecting = false, hideModule = false) {

    private val warn by ListValue("Warn", arrayOf("Chat", "Notification"), "Chat")

    private var alertClearVanish = false

    override fun onDisable() {
        alertClearVanish = false
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        // Reset check on world change
        alertClearVanish = false
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        val packet = event.packet

        if (packet is S38PacketPlayerListItem) {
            handlePlayerList(packet)
        }
    }

    private fun handlePlayerList(packet: S38PacketPlayerListItem) {
        val action = packet.action
        val entries = packet.entries

        if (action == UPDATE_LATENCY) {
            val playerListSize = mc.netHandler?.playerInfoMap?.size ?: 0

            if (entries.size != playerListSize) {
                if (warn == "Chat") {
                    Chat.print("§aA player might be vanished.")
                } else {
                    hud.addNotification(Notification("§aA player might be vanished.", 3000F))
                }

                alertClearVanish = false
            } else {
                if (alertClearVanish)
                    return

                if (warn == "Chat") {
                    Chat.print("§cNo players are vanished")
                } else {
                    hud.addNotification(Notification("§cNo players are vanished", 3000F))
                }

                alertClearVanish = true
            }
        }
    }
}