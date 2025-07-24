/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue.presets
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand

object AutoQueueGommeDuels : Choice("GommeDuels") {

    private var inMatch = false

    private var winMessage by text("WinMessage", "GG, nice try")
    private var loseMessage by text("LoseMessage", "GG, bist wohl besser als ich!")

    private var controlKillAura by boolean("ControlKillAura", true)

    override val parent: ChoiceConfigurable<*>
        get() = presets

    override fun enable() {
        chat(regular("Please set your server language to German (DE) to use AutoPlay"))
        chat(regular("AutoPlay will automatically queue for Duels and re-join if you get disconnected"))
        super.enable()
    }

    val repeatable = tickHandler {
        val inGameHud = mc.inGameHud ?: return@tickHandler
        val playerListHeader = inGameHud.playerListHud.header

        if (playerListHeader == null) {
            inMatch = false
            return@tickHandler
        }

        // Check if we are on GommeHD.net
        val headerText = playerListHeader.convertToString()
        if (!headerText.contains("GommeHD.net")) {
            inMatch = false

            notification("AutoPlay", "Not on GommeHD.net", NotificationEvent.Severity.ERROR)
            waitTicks(20)
            return@tickHandler
        }

        // Check in which situation we are
        if (headerText.contains("Lobby")) {
            handleLobbySituation()
        } else if (headerText.contains("Duels")) {
            handleDuelsSituation()
        } else {
            inMatch = false
        }
    }

    @Suppress("unused")
    val chatReceiveEvent = sequenceHandler<ChatReceiveEvent> { ev ->
        // Only handle game messages. It is unlikely that any server will use a player for the chat game.
        if (ev.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@sequenceHandler
        }

        if (ev.message.contains("Du hast deine zuletzt genutzte Warteschlange für das Kit")) {
            notification("AutoPlay", "Queue started", NotificationEvent.Severity.INFO)
        } else if (ev.message.contains("Du bist bereits in einer Warteschlange")) {
            notification("AutoPlay", "Already in queue", NotificationEvent.Severity.ERROR)
        } else if (ev.message.contains("Das Match kann beginnen")) {
            notification("AutoPlay", "Match started", NotificationEvent.Severity.INFO)
            inMatch = true
        } else if (ev.message.contains("und das Match gewonnen")) {
            notification("AutoPlay", "Match won", NotificationEvent.Severity.INFO)
            inMatch = false

            sync()
            waitSeconds(2)
            network.sendChatMessage(winMessage)
        } else if (ev.message.contains("Du wurdest von") && ev.message.contains("getötet")) {
            notification("AutoPlay", "Match lost", NotificationEvent.Severity.INFO)
            inMatch = false

            sync()
            waitSeconds(2)
            network.sendChatMessage(loseMessage)
        }
    }

    override fun disable() {
        super.disable()

        inMatch = false
    }

    private suspend fun Sequence.handleLobbySituation() {
        inMatch = false

        val duelsEntity = world.entities.filterIsInstance<ArmorStandEntity>().find {
            it.boxedDistanceTo(player) < 5 && it.displayName?.string?.contains("Duels") == true
        }?.let { armorStand ->
            world.entities.filterIsInstance<PlayerEntity>().find {
                it.boxedDistanceTo(player) < 5 && it.pos == armorStand.pos.subtract(0.0, 2.0, 0.0)
            }
        }

        if (duelsEntity == null) {
            notification("AutoPlay", "Could not find Duels NPC", NotificationEvent.Severity.ERROR)
        } else {
            // I mean, we do not need any rotation for the lobby, right?
            interaction.interactEntity(player, duelsEntity, Hand.MAIN_HAND)
            notification("AutoPlay", "Interacted with Duels NPC", NotificationEvent.Severity.INFO)
        }

        sync()
        waitSeconds(5)
    }

    private suspend fun Sequence.handleDuelsSituation() {
        // Check if player inventory has a head
        if (!inMatch) {
            if (controlKillAura) {
                ModuleKillAura.enabled = false
            }

            val headSlot = Slots.Hotbar.findSlot(Items.PLAYER_HEAD)?.hotbarSlot ?: return

            if (headSlot != player.inventory.selectedSlot) {
                SilentHotbar.selectSlotSilently(this, headSlot, 20)
            }

            waitTicks(5)

            // Use head
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, player.yaw, player.pitch)
            }
            waitTicks(20)
        } else if (!ModuleKillAura.running && controlKillAura) {
            ModuleKillAura.enabled = true
        }
    }

}
