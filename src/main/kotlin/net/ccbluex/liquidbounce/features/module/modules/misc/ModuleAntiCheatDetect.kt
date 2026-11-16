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

import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.AntiCheat
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.transactionId
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.network.protocol.common.ClientboundPingPacket
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Module Anti Cheat Detect
 *
 * Attempts to detect the anti-cheat used by the server.
 */
object ModuleAntiCheatDetect : ClientModule("AntiCheatDetect", ModuleCategories.MISC) {

    private const val TRANSACTION_WINDOW = 128
    private const val GRIM_PARAMETERS = 10
    private const val GRIM_STREAK = 5
    private const val GRIM_CONFIRMATIONS = 3
    private const val MIN_ENTRIES = 5
    private const val EVALUATION_INTERVAL = 1000L
    private const val EVALUATION_CONFIRMATIONS = 3

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val automatic by boolean("Automatic", true)
    private val chatMessage by boolean("ChatMessage", true)
    private val notification by boolean("Notification", true)

    private val pingParameters = ArrayDeque<Int>()
    private var grimCount = 0

    private val entries = ArrayDeque<Entry>()
    private var lastEval = 0L
    private var lastCandidate: String? = null
    private var confirmCount = 0
    private var detected = false

    private val reported = mutableSetOf<String>()

    private data class Entry(val time: Long, val param: Int)

    private data class Detection(val name: String, val rate: Int?)

    init {
        doNotIncludeAlways()
    }

    override fun onEnabled() {
        reset()
        alertAboutAntiCheat()
        super.onEnabled()
    }

    override fun onDisabled() {
        reset()
    }

    /**
     * Called by [ServerObserver] when enough transactions have been received.
     */
    fun completed() {
        if (enabled && automatic) {
            alertAboutAntiCheat()
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        val packet = event.packet as? ClientboundPingPacket ?: return@handler
        val param = transactionId(packet.id)

        if (!automatic) {
            if (entries.isNotEmpty() || pingParameters.isNotEmpty()) {
                clearTransactionScan()
            }
            if (chatMessage) {
                val stamp = LocalTime.now().format(timeFormat)
                chat(regular(message("transaction", variable(param.toString()), variable(stamp))))
            }
            return@handler
        }

        if (detected) {
            return@handler
        }

        val now = System.currentTimeMillis()
        entries.addLast(Entry(now, param))
        if (entries.size > TRANSACTION_WINDOW) {
            entries.removeFirst()
        }

        checkGrim(param)
        evaluate(now)
    }

    @Suppress("unused")
    private val connectHandler = handler<ServerConnectEvent> { reset() }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> { reset() }

    private fun checkGrim(param: Int) {
        pingParameters.addLast(param)
        if (pingParameters.size > GRIM_PARAMETERS) {
            pingParameters.removeFirst()
        }
        if (pingParameters.size < GRIM_STREAK) {
            return
        }

        var consecutive = 0
        for (i in pingParameters.size - 1 downTo 1) {
            val current = pingParameters[i]
            if (current == pingParameters[i - 1] - 1 && current < 0) {
                consecutive++
            } else {
                break
            }
        }

        if (consecutive >= GRIM_STREAK) {
            grimCount++
            if (grimCount >= GRIM_CONFIRMATIONS) {
                report(Detection(AntiCheat.GRIM.tag, null))
            }
        } else {
            grimCount = 0
        }
    }

    private fun evaluate(now: Long) {
        if (now - lastEval < EVALUATION_INTERVAL || entries.size < MIN_ENTRIES) {
            return
        }
        lastEval = now

        var up = 0
        var rate = 0
        var previous: Entry? = null

        for (entry in entries) {
            if (now - entry.time <= EVALUATION_INTERVAL) {
                rate++
            }
            if (previous != null) {
                up = if (entry.param == previous.param + 1) up + 1 else 0
            }
            previous = entry
        }

        val candidate = when {
            up >= GRIM_STREAK -> {
                val negative = previous != null && previous.param < 0
                if (negative && rate >= 12) {
                    Detection(AntiCheat.VULCAN.tag, rate)
                } else {
                    Detection(AntiCheat.TRUTHFUL.tag, rate)
                }
            }

            rate >= 15 -> Detection(AntiCheat.MATRIX.tag, rate)
            rate >= 2 -> Detection(AntiCheat.THEMIS.tag, rate)
            else -> null
        }

        val candidateName = candidate?.name
        if (candidateName != null && candidateName == lastCandidate) {
            confirmCount++
        } else {
            lastCandidate = candidateName
            confirmCount = if (candidateName == null) 0 else 1
        }

        if (candidate != null && confirmCount >= EVALUATION_CONFIRMATIONS) {
            report(candidate)
        }
    }

    private fun report(detection: Detection) {
        detected = true
        if (!reported.add(detection.name)) {
            return
        }

        val text = detection.rate?.let {
            message("heuristicDetected", variable(detection.name), variable("$it/s"))
        } ?: message("heuristicDetectedWithoutRate", variable(detection.name))

        if (chatMessage) {
            chat(regular(text))
        }
        if (notification) {
            notification(name, text, NotificationEvent.Severity.INFO)
        }
    }

    private fun alertAboutAntiCheat() {
        val guess = ServerObserver.guessAntiCheat(mc.currentServer?.ip) ?: return
        if (!reported.add(guess.antiCheat.tag)) {
            return
        }

        val text = message("detected", variable(guess.antiCheat.tag), variable("${guess.certainty}%"))
        if (chatMessage) {
            chat(regular(text))
        }
        if (notification) {
            notification(name, text, NotificationEvent.Severity.INFO)
        }
    }

    private fun reset() {
        clearTransactionScan()
        reported.clear()
    }

    private fun clearTransactionScan() {
        entries.clear()
        pingParameters.clear()
        grimCount = 0
        lastEval = 0
        lastCandidate = null
        confirmCount = 0
        detected = false
    }

}
