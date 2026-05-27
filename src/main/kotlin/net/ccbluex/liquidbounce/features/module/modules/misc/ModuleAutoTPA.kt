/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.script.bindings.api.ScriptClient
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object ModuleAutoTPA : ClientModule("AutoTPA", ModuleCategories.MISC) {

    private val mode by enumChoice("Command", Modes.TPACCEPT)
    private val friendsOnly by boolean("OnlyFriends", true)

    private val teleportMessages = arrayOf(
        "has requested teleport", "просит телепортироваться", "хочет телепортироваться к вам",
        "просит к вам телепортироваться", "хочет телепортироваться к вам.", "отправил вам запрос на телепортацию",
        "/tpyes", "/tpaccept", "/tpno", "/tpdeny", "Z Запрос на телепортацию от", "Z Принять - /tpyes",
        "Z Отклонить - /tpno", "has requested to teleport to you", "To teleport, type /tpaccept",
        "To deny, type /tpdeny", "wants to teleport to you", "[Accept]", "[Deny]", "Incoming teleport request from",
        "Teleport to you", "has sent a TPA request", "sent a teleport request to you", "[Accept", "[Deny", "accept]",
        "deny]"
    )

    private val blacklistMessages = arrayOf(
        "✉", "[ЛС]", "You]", "Я]", "Вы]", "I]", "» You", "-> You","» I", "-> I", "» Я", "-> Я",
        "» Вы", "-> Вы", "You)", "Я)", "Вы)", "I)",
    )

    private var canAccept = false

    @Suppress("unused")
    private val onPacket = handler<PacketEvent> { e ->
        val message = when (val packet = e.packet) {
            is ClientboundSystemChatPacket -> packet.content().string
            is ClientboundDisguisedChatPacket -> packet.message().string
            else -> return@handler
        }

        if (isTeleportMessage(message)) {
            if (friendsOnly) {
                val matchedTrigger = teleportMessages.firstOrNull { message.contains(it, ignoreCase = true) } ?: ""
                val cleanMessage = message.replace(matchedTrigger, "", ignoreCase = true)

                canAccept = FriendManager.friends.any { cleanMessage.contains(it.name, ignoreCase = true) }
            } else {
                canAccept = true
            }
        }
    }

    @Suppress("unused")
    private val onTick = handler<PlayerTickEvent> {
        if (canAccept) {
            canAccept = false
            val player = mc.player ?: return@handler

            player.connection.sendCommand(mode.tag)
            ScriptClient.displayChatMessage("§a[AutoTPA] §7Teleport request accepted!")
        }
    }

    private fun isTeleportMessage(message: String): Boolean {
        return teleportMessages.any { message.contains(it, ignoreCase = true) }
            && !blacklistMessages.any { message.contains(it, ignoreCase = true) }
    }

    private enum class Modes(override val tag: String) : Tagged {
        TPACCEPT("tpaccept"),
        TPYES("tpyes"),
    }
}
