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

        val deathRegex = Regex("you died! want to play again\\? click here!", RegexOption.IGNORE_CASE)
        if (deathRegex.containsMatchIn(message)) {
            localPlayerDeathCounter++
        }

        mc.player?.let { player ->
            val name = player.name.string

            val killedByRegex = Regex("\\b${Regex.escape(name)}\\b was killed by .+", RegexOption.IGNORE_CASE)
            val voidRegex = Regex("\\b${Regex.escape(name)}\\b fell into the void\\.", RegexOption.IGNORE_CASE)
            val diedRegex = Regex("\\b${Regex.escape(name)}\\b died\\.", RegexOption.IGNORE_CASE)

            if (killedByRegex.containsMatchIn(message) ||
                voidRegex.containsMatchIn(message) ||
                diedRegex.containsMatchIn(message)
            ) {
                localPlayerDeathCounter++
            }
        }
    }


}
