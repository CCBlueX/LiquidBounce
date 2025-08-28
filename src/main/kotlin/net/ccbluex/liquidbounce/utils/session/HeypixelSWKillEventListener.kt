package net.ccbluex.liquidbounce.utils.session

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.HeypixelSWKillEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket

object HeypixelSWKillEventListener : EventListener {
    init {
        HeypixelSWKillEventListener
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet !is GameMessageS2CPacket) return@handler

        val message = packet.content.string

        val patterns = listOf(
            Regex("(.+?) 被 (.+?) 击败(.*)"),
            Regex("(.+?) 被炸成了粉尘, 最终还是被 (.+?) 击败(.*)"),
            Regex("(.+?) 消逝了, 最终还是被 (.+?) 击败(.+?)"),
            Regex("(.+?) 被架在了烧烤架上, 熟透了, 最终还是被 (.+?) 击败(.*)"),
            Regex("(.+?) 跑得很快, 但是他还是摔了一跤, 最终被 (.+?) 击败(.*)"),
            Regex("(.+?) 被 (.+?) 用弓箭射穿了(.*?)"),
            Regex("(.+?) 被重压地无法呼吸, 最终还是被 (.+?) 击败(.*)")
        )

        for (pattern in patterns) {
            val match = pattern.find(message) ?: continue
            val victim = match.groupValues[1].trim()
            val killer = match.groupValues[2].trim()
            EventManager.callEvent(HeypixelSWKillEvent(victim, killer))
            return@handler
        }
    }

}
