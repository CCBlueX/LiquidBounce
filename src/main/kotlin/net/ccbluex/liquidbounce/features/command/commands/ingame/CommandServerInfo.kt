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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.enumsParameter
import net.ccbluex.liquidbounce.features.command.builder.parseEnumsFromParameter
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.ServerObserver.captureCommandSuggestions
import net.ccbluex.liquidbounce.utils.client.ServerObserver.formattedPluginList
import net.ccbluex.liquidbounce.utils.client.ServerObserver.guessAntiCheat
import net.ccbluex.liquidbounce.utils.client.ServerObserver.hostingInformation
import net.ccbluex.liquidbounce.utils.client.ServerObserver.payloadChannels
import net.ccbluex.liquidbounce.utils.client.ServerObserver.plugins
import net.ccbluex.liquidbounce.utils.client.ServerObserver.requestHostingInformation
import net.ccbluex.liquidbounce.utils.client.ServerObserver.serverAddress
import net.ccbluex.liquidbounce.utils.client.ServerObserver.serverId
import net.ccbluex.liquidbounce.utils.client.ServerObserver.serverType
import net.ccbluex.liquidbounce.utils.client.ServerObserver.serverVersion
import net.ccbluex.liquidbounce.utils.client.ServerObserver.tps
import net.ccbluex.liquidbounce.utils.client.ServerObserver.transactions
import net.minecraft.text.HoverEvent

/**
 * Displays the current server information, including TPS (ticks per second) and detected server version.
 */
object CommandServerInfo : CommandFactory, EventListener {

    @Suppress("CognitiveComplexMethod")
    override fun createCommand(): Command {
        return CommandBuilder
            .begin("serverinfo")
            .requiresIngame()
            .parameter(
                enumsParameter<DetectionType>("detect")
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val detectionTypes = parseEnumsFromParameter<DetectionType>(args.getOrNull(0) as? String)

                if (detectionTypes.isNotEmpty()) {
                    Sequence(this) {
                        chat(regular(command.result("detecting")))

                        if (DetectionType.PLUGINS in detectionTypes) {
                            captureCommandSuggestions()
                            // Timeout after 5 seconds.
                            waitConditional(20 * 5) { plugins != null }

                            if (plugins == null) {
                                chat(markAsError(command.result("pluginsDetectionTimeout")))
                            }
                        }

                        if (DetectionType.HOSTING in detectionTypes) {
                            requestHostingInformation()
                        }

                        printInformation(command, detectionTypes)
                    }
                } else {
                    printInformation(command)
                }
            }
            .build()
    }

    private fun printInformation(command: Command, detections: List<DetectionType> = emptyList()) {
        val notAvailableError = markAsError("N/A") // N/A should be a common understanding

        val serverInfo = network.serverInfo
        val resolvedServerAddress = serverAddress?.toString()
        val tps = tps
        val ping = network.getPlayerListEntry(player.uuid)?.latency ?: 0

        val advertisedVersion = "${serverInfo?.version?.convertToString()} (${serverInfo?.protocolVersion})"
        val detectedServerVersion = serverVersion ?: "<= 1.20.4"

        chat(warning(command.result("header")))
        chat(regular(command.result("address", serverInfo?.address?.let(::variable) ?: notAvailableError)))
        chat(regular(command.result("resolvedAddress", resolvedServerAddress?.let(::variable) ?: notAvailableError)))
        chat(regular(command.result("serverId", serverId?.let(::variable) ?: notAvailableError)))
        chat(regular(command.result("serverType", serverType?.let{ variable(it.choiceName) } ?: notAvailableError)))
        chat(regular(command.result("brand", network.brand?.let(::variable) ?: notAvailableError)))
        chat(regular(command.result("advertisedVersion", variable(advertisedVersion))))
        chat(regular(command.result("detectedVersion", variable(detectedServerVersion).styled {
            it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, command.result("detectedVersion.description", variable(detectedServerVersion))))
        })))
        chat(regular(command.result("tps", variable(
            if (tps.isNaN()) command.result("nan").string else tps.roundToDecimalPlaces(2).toString()
        ))))
        chat(regular(command.result("ping", variable(ping.toString()))))

        chat(regular(command.result("channels", payloadChannels.map { id ->
            variable(id.toString())
        }.joinToText(regular(", ")))))

        chat(regular(command.result("transactions", transactions.map { variable(it.toString()) }.joinToText(regular(", ")))))
        chat(regular(command.result("transactionDifferences", transactions.windowed(2) { it[1] - it[0] }.map { variable(it.toString()) }.joinToText(regular(", ")))))
        chat(regular(command.result("guessedAntiCheat", guessAntiCheat(serverInfo?.address ?: "")?.let(::variable)?.styled { style ->
            style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, command.result("guessedAntiCheat.description")))
        } ?: notAvailableError)))

        val ipData = hostingInformation
        if (ipData != null) {
            // Hosting Information
            chat(regular(command.result("hostingIp", ipData.ip?.let(::variable) ?: notAvailableError)))
            chat(regular(command.result("hostingHostname", ipData.hostname?.let(::variable) ?: notAvailableError)))
            chat(regular(command.result("hostingOrganization", ipData.org?.let(::variable) ?: notAvailableError)))
            chat(regular(command.result("hostingCountry", ipData.country?.let(::variable) ?: notAvailableError)))
            chat(regular(command.result("hostingCity", ipData.city?.let(::variable) ?: notAvailableError)))
            chat(regular(command.result("hostingRegion", ipData.region?.let(::variable) ?: notAvailableError)))
        }

        val plugins = plugins
        if (plugins != null) {
            // Plugin information
            val pluginCount = plugins.size
            val pluginList = formattedPluginList?.joinToText(regular(", ")) ?: notAvailableError
            chat(regular(command.result("plugins", variable(pluginCount.toString()), pluginList)))
        }

        if (detections.isEmpty()) {
            val detectionList = DetectionType.entries.map { detectionType ->
                variable(detectionType.choiceName)
            }.joinToText(regular(", "))
            chat(warning(command.result("detectParameter", detectionList)))
        }
    }

    private enum class DetectionType(override val choiceName: String) : NamedChoice {
        PLUGINS("Plugins"),
        HOSTING("Hosting");
    }

}

