package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket

object PlayerDeadEventListener : EventListener {

    private var wasAliveLastTick = true
    private var localPlayerDeathCounter = 0

    val deathCount: Int
        get() = localPlayerDeathCounter

    init {
        tickHandler {
            mc.player?.let { player ->
                updateDeathCount(player)
            }
        }
    }

    private fun updateDeathCount(player: PlayerEntity) {
        if (player != mc.player) return

        val isNowDead = player.isRemoved || !player.isAlive
        if (wasAliveLastTick && isNowDead) {
            localPlayerDeathCounter++
        }
        wasAliveLastTick = !isNowDead
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet !is GameMessageS2CPacket) return@handler

        val message = packet.content.string
        val playerName = mc.player?.name?.string ?: return@handler

        val combinedPattern = Regex(
            "you died! want to play again\\? click here!" +
                "|\\b${Regex.escape(playerName)}\\b was killed by .+" +
                "|\\b${Regex.escape(playerName)}\\b fell into the void\\." +
                "|\\b${Regex.escape(playerName)}\\b died\\." +
                "|\\b${Regex.escape(playerName)}\\b died in close combat to .+",
            RegexOption.IGNORE_CASE
        )

        if (combinedPattern.containsMatchIn(message)) {
            localPlayerDeathCounter++
        }
    }

}
