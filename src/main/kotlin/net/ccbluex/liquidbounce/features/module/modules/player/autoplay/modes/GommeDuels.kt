package net.ccbluex.liquidbounce.features.module.modules.player.autoplay.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.autoplay.ModuleAutoPlay.modes
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket
import net.minecraft.util.Hand

object GommeDuels : Choice("GommeDuels") {

    private var inMatch = false
    private var inQueue = false

    private var forceKillAura by boolean("ForceKillAura", true)
    private var winMessage by text("WinMessage", "gg, nice try")
    private var loseMessage by text("LoseMessage", "gg, bist wohl besser als ich!")

    override val parent: ChoiceConfigurable
        get() = modes

    val repeatable = repeatable {
        val inGameHud = mc.inGameHud ?: return@repeatable
        val playerListHeader = inGameHud.playerListHud.header

        if (playerListHeader == null) {
            inMatch = false
            inQueue = false
            return@repeatable
        }

        val headerText = playerListHeader.outputString()

        if (!headerText.contains("GommeHD.net")) {
            inMatch = false
            inQueue = false
            return@repeatable
        }

        if (!headerText.contains("Duels")) {
            inMatch = false
            inQueue = false

            val duelsEntity = mc.world?.entities?.find {
                it is PlayerEntity && it.nameForScoreboard.contains("§\u0007§\t§\u0005§\u0003§\u0004§\u0005")
                    && it.boxedDistanceTo(player) < 5
            }

            if (duelsEntity == null) {
                notification("AutoPlay", "Could not find Duels NPC", NotificationEvent.Severity.ERROR)
                sync()
                waitSeconds(5)
                return@repeatable
            }

            // I mean, we do not need any rotation for the lobby, right?
            interaction.interactEntity(player, duelsEntity, Hand.MAIN_HAND)
            notification("AutoPlay", "Interacted with Duels NPC", NotificationEvent.Severity.INFO)
            sync()
            waitSeconds(5)
            return@repeatable
        }

        // Check if player inventory has a head
        if (!inMatch) {
            ModuleKillAura.enabled = false

            val headSlot = findHotbarSlot(Items.PLAYER_HEAD) ?: return@repeatable

            if (headSlot != player.inventory.selectedSlot) {
                SilentHotbar.selectSlotSilently(this, headSlot, 20)
            }

            // Use head
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence)
            }

            inQueue = true
            waitTicks(20)
        } else if (!ModuleKillAura.enabled && forceKillAura) {
            ModuleKillAura.enabled = true
        }
    }

    val chatReceiveEvent = sequenceHandler<ChatReceiveEvent> { ev ->
        // Only handle game messages. It is unlikely that any server will use a player for the chat game.
        if (ev.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@sequenceHandler
        }

        if (ev.message.contains("Du hast deine zuletzt genutzte Warteschlange für das Kit")) {
            notification("AutoPlay", "Queue started", NotificationEvent.Severity.INFO)
            inQueue = true
        } else if (ev.message.contains("Du bist bereits in einer Warteschlange")) {
            notification("AutoPlay", "Already in queue", NotificationEvent.Severity.ERROR)
            inQueue = true
        } else if (ev.message.contains("Das Match kann beginnen")) {
            notification("AutoPlay", "Match started", NotificationEvent.Severity.INFO)
            inMatch = true
            inQueue = false
        } else if (ev.message.contains("Deine Match-Statistiken")) {
            notification("AutoPlay", "Match ended", NotificationEvent.Severity.INFO)
            inMatch = false
        } else if (ev.message.contains("und das Match gewonnen")) {
            notification("AutoPlay", "Match won", NotificationEvent.Severity.INFO)
            inMatch = false
            inQueue = false

            sync()
            waitSeconds(2)
            network.sendChatMessage(winMessage)
        } else if (ev.message.contains("und das Match verloren")) {
            notification("AutoPlay", "Match lost", NotificationEvent.Severity.INFO)
            inMatch = false
            inQueue = false

            sync()
            waitSeconds(2)
            network.sendChatMessage(loseMessage)
        }
    }

    override fun disable() {
        super.disable()
        inMatch = false
        inQueue = false
    }

}
