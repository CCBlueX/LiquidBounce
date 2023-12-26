package net.ccbluex.liquidbounce.features.module.modules.player.autoplay.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.autoplay.ModuleAutoPlay.modes
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket
import net.minecraft.util.Hand

object GommeDuels : Choice("GommeDuels") {

    private var inMatch = false

    private var forceKillAura by boolean("ForceKillAura", true)
    private var winMessage by text("WinMessage", "GG, nice try")
    private var loseMessage by text("LoseMessage", "GG, bist wohl besser als ich!")

    override val parent: ChoiceConfigurable
        get() = modes

    override fun enable() {
        chat(regular("Please set your server language to German (DE) to use AutoPlay"))
        chat(regular("AutoPlay will automatically queue for Duels and re-join if you get disconnected"))
        super.enable()
    }

    val repeatable = repeatable {
        val inGameHud = mc.inGameHud ?: return@repeatable
        val playerListHeader = inGameHud.playerListHud.header

        if (playerListHeader == null) {
            inMatch = false
            return@repeatable
        }

        // Check if we are on GommeHD.net
        val headerText = playerListHeader.outputString()
        if (!headerText.contains("GommeHD.net")) {
            inMatch = false

            notification("AutoPlay", "Not on GommeHD.net", NotificationEvent.Severity.ERROR)
            waitTicks(20)
            return@repeatable
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

    private suspend fun Sequence<*>.handleLobbySituation() {
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

    private suspend fun Sequence<*>.handleDuelsSituation() {
        // Check if player inventory has a head
        if (!inMatch) {
            ModuleKillAura.enabled = false

            val headSlot = findHotbarSlot(Items.PLAYER_HEAD) ?: return

            if (headSlot != player.inventory.selectedSlot) {
                SilentHotbar.selectSlotSilently(this, headSlot, 20)
            }

            waitTicks(5)

            // Use head
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence)
            }
            waitTicks(20)
        } else if (!ModuleKillAura.enabled && forceKillAura) {
            ModuleKillAura.enabled = true
        }
    }

}
