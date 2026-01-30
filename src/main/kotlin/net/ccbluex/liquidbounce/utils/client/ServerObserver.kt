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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.fastutil.objectRBTreeSetOf
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.waitMatchesWithTimeout
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePlugins
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiCheatDetect
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import net.minecraft.network.protocol.login.ClientboundHelloPacket
import net.minecraft.resources.Identifier
import java.net.InetAddress
import java.util.TreeSet
import kotlin.random.Random
import kotlin.time.Duration

object ServerObserver : EventListener {

    @Suppress("SpellCheckingInspection")
    private val knownAntiCheats = setOf(
        "nocheatplus",
        "grimac",
        "aac",
        "intave",
        "horizon",
        "vulcan",
        "Vulcan",
        "spartan",
        "kauri",
        "anticheatreloaded",
        "matrix",
        "themis",
        "negativity"
    )

    var serverInfo: ServerData? = null
        private set
    var serverAddress: ServerAddress? = null
        private set
    var serverId: String? = null
        private set
    var serverType: ServerType? = null
        private set
    val payloadChannels: TreeSet<Identifier> = TreeSet<Identifier>()

    val transactions = mutableListOf<Int>()
    var isCapturingTransactions = false

    // defines how many packets are recorded to get the average
    private const val AVERAGE_OF = 15

    // stores last intervals between WorldTimeUpdateS2CPackets
    private val intervals = ArrayDeque<Double>(AVERAGE_OF + 1)
    private val chronometer = Chronometer()
    private var wasDisconnected = true

    var tps = Double.NaN
        private set
    var serverVersion: String? = null
        private set
    var hostingInformation: IpInfoApi.IpData? = null
        private set

    var plugins: Set<String>? = null
        private set

    val formattedPluginList: List<Component>?
        get() = plugins?.map { pluginName ->
            pluginName.asPlainText(
                if (knownAntiCheats.contains(pluginName)) {
                    ChatFormatting.GREEN
                } else {
                    ChatFormatting.RED
                }
            )
        }

    @Suppress("unused")
    private val handleServerConnect = handler<ServerConnectEvent> { event ->
        this.serverInfo = event.serverInfo
        this.serverAddress = event.address
    }

    /**
     * Reconnects to the last server. This is safe to call from every thread since it records a render call and
     * therefore runs in the Minecraft thread
     */
    fun reconnect() {
        val serverInfo = serverInfo ?: error("no known last server")
        val serverAddress = ServerAddress.parseString(serverInfo.ip)

        mc.execute {
            ConnectScreen.startConnecting(
                JoinMultiplayerScreen(TitleScreen()),
                mc,
                serverAddress,
                serverInfo,
                false,
                null
            )
        }
    }

    /**
     * Requests completions for all given commands.
     * This is an exploit for servers that block the `/plugins` command.
     *
     * Plugins will add themselves to the command suggestions list with a prefix like `/pluginname:command`.
     * This can be used to get a list of plugins on the server.
     *
     * @see [ServerboundCommandSuggestionPacket]
     * @see [ModulePlugins]
     */
    suspend fun captureCommandSuggestions(timeout: Duration): Boolean {
        this.plugins = null
        val completionId = Random.nextInt(0, 32767)
        network.send(ServerboundCommandSuggestionPacket(completionId, "/"))
        /**
         * Server sends a command suggestions packet with a list of commands.
         * These commands are usually prefixed with the plugin name and a colon.
         */
        val packet = waitMatchesWithTimeout<PacketEvent>(timeout, priority = FIRST_PRIORITY) {
            it.packet is ClientboundCommandSuggestionsPacket && it.packet.id == completionId
        }?.packet ?: return false

        packet as ClientboundCommandSuggestionsPacket

        this.plugins = packet.toSuggestions().list.mapNotNullTo(objectRBTreeSetOf()) { cmd ->
            val command = cmd.text.split(":")

            if (command.size > 1) {
                command[0].replace("/", "")
            } else {
                null
            }
        }
        return !this.plugins.isNullOrEmpty()
    }

    suspend fun requestHostingInformation() {
        val hostAddress: String = this.serverAddress?.host ?: return
        val ipAddress: InetAddress = try {
            // If it's already an IP address, it will parse it directly
            // If it's a domain name, it will resolve it to an IP address
            InetAddress.getByName(hostAddress)
        } catch (e: Exception) {
            logger.error("Failed to resolve host address: $hostAddress", e)
            return
        }

        hostingInformation = runCatching {
            IpInfoApi.someoneElse(ipAddress.hostAddress)
        }.getOrNull()
    }

    @Suppress("unused")
    private val packetObserver = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            /**
             * The world time update packet should be sent once every second.
             * This allows us to calculate the TPS (ticks per second) of the server.
             */
            is ClientboundSetTimePacket -> {
                if (wasDisconnected && intervals.isEmpty()) {
                    wasDisconnected = false
                    chronometer.reset()
                    return@handler
                }

                val currentTime = System.currentTimeMillis()
                val elapsed = chronometer.elapsedUntil(currentTime).toDouble()
                chronometer.reset(currentTime)

                intervals.addLast(elapsed)
                while (intervals.size > AVERAGE_OF) {
                    intervals.removeFirst()
                }

                val averageInterval = intervals.average()
                mc.execute {
                    tps = if (averageInterval > 0 && !averageInterval.isNaN()) {
                        (20.0 / (averageInterval / 1000.0)).coerceIn(0.0..20.0)
                    } else {
                        Double.NaN
                    }
                }
            }

            /**
             * Server version detection reading the version from the server resource pack which
             * is not being spoofed by anything at the moment. Most realiable way to detect the version
             * of the server even when it spoofs the brand.
             *
             * @author nekosarekawaii
             */
            is ClientboundSelectKnownPacks -> {
                for (knownPack in packet.knownPacks()) {
                    if (knownPack.isVanilla && knownPack.id() == "core") { // Works for 1.20.5+ servers
                        this.serverVersion = knownPack.version()
                        break
                    }
                }
            }

            /**
             * Server sents a hello packet with the server id and public key,
             * as well as if the server is cracked or not.
             */
            is ClientboundHelloPacket -> {
                // The Server ID is not often present and likely reserved for official servers.
                if (packet.serverId.isNotEmpty()) {
                    this.serverId = packet.serverId
                }
                this.serverType = if (packet.shouldAuthenticate()) {
                    ServerType.PREMIUM
                } else {
                    ServerType.CRACKED
                }
            }

            /**
             * Watches for the payload channels that are being used by the server.
             */
            is ClientboundCustomPayloadPacket -> {
                val payload = packet.payload
                payloadChannels.add(payload.type().id)
            }

            is ClientboundPingPacket -> if (isCapturingTransactions) {
                transactions.add(packet.id)
                if (transactions.size >= 5) {
                    ModuleAntiCheatDetect.completed()
                    isCapturingTransactions = false
                }
            }

            is ClientboundLoginPacket -> {
                transactions.clear()
                isCapturingTransactions = true
            }

        }

    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        this.wasDisconnected = true
        this.intervals.clear()
        this.tps = Double.NaN
        // Do NOT set to NULL because we need to keep the server address for reconnecting
        // this.serverInfo = null
        this.serverVersion = null
        this.plugins = null
        this.serverAddress = null
        this.hostingInformation = null
        this.serverId = null
        this.serverType = null
        this.payloadChannels.clear()
        this.transactions.clear()
    }

    /**
     * Reference: https://github.com/CCBlueX/LiquidBounce/blob/legacy/src/main/java/net/ccbluex/liquidbounce/features/module/modules/misc/AnticheatDetector.kt
     * @author RtxOP
     */
    fun guessAntiCheat(address: String?): String? {
        if (transactions.size < 5) {
            return null
        }

        val diffs = transactions.windowed(2) { it[1] - it[0] }
        val first = transactions.first()

        return when {
            address?.endsWith("hypixel.net", true) == true -> "Watchdog"

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
            }

            transactions.take(2).let { it[0] == it[1] }
                && transactions.drop(2).windowed(2).all { it[1] - it[0] == 1 }
                -> "Verus"

            diffs.take(2).let { it[0] >= 100 && it[1] == -1 }
                && diffs.drop(2).all { it == -1 }
                -> "Polar"

            transactions.first() < -3000 && transactions.any { it == 0 }
                -> "Intave"

            transactions.take(3) == listOf(-30767, -30766, -25767)
                && transactions.drop(3).windowed(2).all { it[1] - it[0] == 1 }
                -> "Old Vulcan"

            else -> "Unknown"
        }
    }

    enum class ServerType(override val tag: String) : Tagged {

        /**
         * Allows only premium players to join.
         */
        PREMIUM("Premium"),

        /**
         * Allows premium and cracked players to join.
         */
        CRACKED("Cracked");

    }

}
