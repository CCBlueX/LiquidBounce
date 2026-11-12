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

import com.mojang.brigadier.suggestion.Suggestions
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.waitMatchesWithTimeout
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePlugins
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAntiCheatDetect
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
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
import kotlin.time.Duration.Companion.seconds

object ServerObserver : EventListener {

    var serverInfo: ServerData? by clientState { null }
        private set
    var serverAddress: ServerAddress? by clientState { null }
        private set
    var serverId: String? by clientState { null }
        private set
    var serverType: ServerType? by clientState { null }
        private set
    val payloadChannels: TreeSet<Identifier> by clientState { TreeSet<Identifier>() }

    val transactions by clientState { mutableListOf<Int>() }
    var isCapturingTransactions by clientState { false }
    private const val TRANSACTIONS = 5

    private val keepAlives by clientState { mutableListOf<Long>() }
    private const val KEEP_ALIVES = 5

    // defines how many packets are recorded to get the average
    private const val AVERAGE_OF = 15

    // stores last intervals between WorldTimeUpdateS2CPackets
    private val intervals by clientState { ArrayDeque<Double>(AVERAGE_OF + 1) }
    private val chronometer by clientState { Chronometer() }
    private var wasDisconnected by clientState { true }

    var tps by clientState { Double.NaN }
        private set

    /**
     * How fast the server ticks against the client: [tps] over the tick rate the client runs at. 1 until there is a
     * measurement.
     */
    val tickSpeed: Double
        get() {
            val level = currentClient.level
            return if (tps.isNaN() || level == null) 1.0 else tps / level.tickRateManager().tickrate()
        }

    var serverVersion: String? by clientState { null }
        private set
    var hostingInformation: IpInfoApi.IpData? by clientState { null }
        private set

    var plugins: Set<String>? by clientState { null }
        private set

    val formattedPluginList: List<Component>?
        get() = plugins?.map { pluginName ->
            pluginName.asPlainText(
                if (AntiCheat.ofPlugin(pluginName) != null) {
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
     * Bukkit's `/version <plugin>` suggests the names of all plugins, also those without commands. `bukkit:version`
     * gets past servers that only block `version`.
     */
    private val versionCommands = arrayOf("version", "bukkit:version")

    /**
     * Paper sends nothing back when there is no suggestion, so the `/version` requests give up early.
     */
    private val versionAnswerTimeout = 1.seconds

    /**
     * Collects the server's plugins for servers that block `/plugins`: the names `/version` suggests, and the
     * prefixes of the server's commands, like `/pluginname:command`.
     *
     * @see [ServerboundCommandSuggestionPacket]
     * @see [ModulePlugins]
     */
    suspend fun captureCommandSuggestions(timeout: Duration): Boolean {
        this.plugins = null

        // Plugin names keep their spelling, so they go in before the lowercase command prefixes
        val plugins = TreeSet(String.CASE_INSENSITIVE_ORDER)
        versionCommands.firstNotNullOfOrNull { command ->
            requestSuggestions("/$command ", versionAnswerTimeout)?.list?.takeIf { it.isNotEmpty() }
        }?.mapTo(plugins) { it.text }

        plugins += commandPrefixes(serverCommands()).ifEmpty {
            // ViaVersion sends no commands of a pre-1.13 server, it only suggests them on request
            commandPrefixes(requestSuggestions("/", timeout)?.list?.map { it.text }.orEmpty())
        }

        this.plugins = plugins
        return plugins.isNotEmpty()
    }

    /**
     * The commands the server sent, without the client-side commands Fabric adds to them.
     */
    private fun serverCommands(): List<String> {
        val clientCommands = ClientCommands.getActiveDispatcher()?.root?.children.orEmpty().mapTo(HashSet()) { it.name }
        return network.commands.root.children.map { it.name }.filter { it !in clientCommands }
    }

    private fun commandPrefixes(commands: List<String>) =
        commands.mapNotNull { it.substringBefore(':', "").removePrefix("/").ifEmpty { null } }

    /**
     * One request at a time, as ViaVersion tags every answer of a pre-1.13 server with the id of the latest request.
     */
    private suspend fun requestSuggestions(input: String, timeout: Duration): Suggestions? {
        val id = Random.nextInt(0, Short.MAX_VALUE.toInt())
        network.send(ServerboundCommandSuggestionPacket(id, input))
        val event = waitMatchesWithTimeout<PacketEvent>(timeout, priority = FIRST_PRIORITY) {
            (it.packet as? ClientboundCommandSuggestionsPacket)?.id == id
        }
        return (event?.packet as? ClientboundCommandSuggestionsPacket)?.toSuggestions()
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
                        (20.0 / (averageInterval / 1000.0)).coerceIn(0.0, 20.0)
                    } else {
                        Double.NaN
                    }

                    debugParameter("TPS") { tps }
                    debugParameter("Tick Speed") { tickSpeed }
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
                transactions.add(transactionId(packet.id))
                if (transactions.size >= TRANSACTIONS) {
                    ModuleAntiCheatDetect.completed()
                    isCapturingTransactions = false
                }
            }

            is ClientboundKeepAlivePacket -> if (keepAlives.size < KEEP_ALIVES) {
                keepAlives.add(packet.id)
            }

            is ClientboundLoginPacket -> {
                transactions.clear()
                keepAlives.clear()
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
        this.keepAlives.clear()
    }

    /**
     * The most certain [AntiCheat] for the current connection.
     */
    fun guessAntiCheat(address: String?): AntiCheatGuess? = AntiCheat.guess(
        address,
        // Channels the server registered through minecraft:register
        if (mc.connection == null) emptySet() else ClientPlayNetworking.getSendable(),
        transactions.takeIf { it.size >= TRANSACTIONS },
        keepAlives,
    )

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
