package net.ccbluex.liquidbounce.features.module.modules.player.autoplay.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.autoplay.ModuleAutoPlay.modes
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
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
        // Check if player inventory has a head
        if (!inMatch) {
            if (inQueue) {
                return@repeatable
            }

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
            waitSeconds(2)
            network.sendChatCommand(winMessage)
        } else if (ev.message.contains("und das Match verloren")) {
            notification("AutoPlay", "Match lost", NotificationEvent.Severity.INFO)
            inMatch = false
            inQueue = false
            waitSeconds(2)
            network.sendChatCommand(loseMessage)
        }
    }

}
