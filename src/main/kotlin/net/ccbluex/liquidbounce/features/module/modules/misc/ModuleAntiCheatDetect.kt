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
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket

/**
 * Module Anti Cheat Detect
 *
 * Attempts to detect the anti-cheat used by the server.
 *
 * Reference: https://github.com/CCBlueX/LiquidBounce/blob/legacy/src/main/java/net/ccbluex/liquidbounce/features/module/modules/misc/AnticheatDetector.kt
 * @author RtxOP
 */
object ModuleAntiCheatDetect : ClientModule("AntiCheatDetect", Category.MISC) {

    private val debug by boolean("Debug", true)
    private val actionNumbers = mutableListOf<Int>()
    private var isDetecting = false

    override val running: Boolean
        get() = enabled

    init {
        doNotIncludeAlways()
    }

    override fun enable() {
        chat(warning(message("description")))
    }

    override fun disable() {
        reset()
    }
    private fun reset() {
        actionNumbers.clear()
        isDetecting = false
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val packet = event.packet

        when (packet) {
            is CommonPingS2CPacket -> if (isDetecting) {
                handleTransaction(packet.parameter)
            }

            is GameJoinS2CPacket -> reset().also {
                isDetecting = true
            }
        }
    }

    private fun handleTransaction(action: Int) {
        actionNumbers.add(action).also { if (debug) chat("ID: $action") }
        if (actionNumbers.size >= 5) analyzeActionNumbers()
    }

    private fun analyzeActionNumbers() {
        val diffs = actionNumbers.windowed(2) { it[1] - it[0] }
        val first = actionNumbers.first()

        when {
            mc.currentServerEntry?.address?.lowercase().equals("hypixel.net", true) -> notify("Watchdog")

            diffs.all { it == diffs.first() } -> when (diffs.first()) {
                1 -> when (first) {
                    in -23772..-23762 -> "Vulcan"
                    in 95..105, in -20005..-19995 -> "Matrix"
                    in -32773..-32762 -> "Grizzly"
                    else -> "Verus"
                }
                -1 -> when {
                    first in -8287..-8280 -> "Errata"
                    first < -3000 -> "Intave"
                    first in -5..0 -> "Grim"
                    first in -3000..-2995 -> "Karhu"
                    else -> "Polar"
                }
                else -> null
            }?.let { antiCheat -> notify(antiCheat) }

            actionNumbers.take(2).let { it[0] == it[1] }
                && actionNumbers.drop(2).windowed(2).all { it[1] - it[0] == 1 }
                -> notify("Verus")

            diffs.take(2).let { it[0] >= 100 && it[1] == -1 }
                && diffs.drop(2).all { it == -1 }
                -> notify("Polar")

            actionNumbers.first() < -3000 && actionNumbers.any { it == 0 }
                -> notify("Intave")

            actionNumbers.take(3) == listOf(-30767, -30766, -25767)
                && actionNumbers.drop(3).windowed(2).all { it[1] - it[0] == 1 }
                -> notify("Old Vulcan")

            else -> notify("Unknown").also {
                chat(regular(message("actionNumbers", actionNumbers.joinToString())))
                chat(regular(message("differences", actionNumbers.windowed(2) { it[1] - it[0] }.joinToString())))
            }
        }

        reset()
    }

    private fun notify(message: String) {
        chat(regular(message("detected", message)))
    }

}
