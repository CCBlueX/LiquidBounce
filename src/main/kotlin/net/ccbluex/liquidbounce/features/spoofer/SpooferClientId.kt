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
package net.ccbluex.liquidbounce.features.spoofer

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.spoofer.clientid.payload.ClientIdModCheckPayload
import net.ccbluex.liquidbounce.features.spoofer.clientid.payload.ClientIdModListPayload
import net.ccbluex.liquidbounce.features.spoofer.clientid.payload.ClientIdPackListPayload
import net.ccbluex.liquidbounce.features.spoofer.clientid.payload.ClientIdVersionPayload
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.protocol.game.ClientboundLoginPacket

/**
 * Emulates Novinity's ClientID client-side mod.
 *
 * ClientID sends four UTF-8 custom payloads on play join:
 * - clientid:modcheck -> profile UUID
 * - clientid:modlist -> comma separated mod IDs
 * - clientid:packlist -> comma separated enabled resource pack names
 * - clientid:clientversion -> ClientID version
 */
object SpooferClientId : ToggleableValueGroup(name = "ClientIDSpoofer", enabled = false) {

    private const val DEFAULT_VERSION = "1.1.3"
    private const val CLIENT_ID_MOD_ID = "clientid"

    private val spoofedMods = listOf(
        "minecraft",
        "java",
        "fabricloader",
        "fabric-api",
        CLIENT_ID_MOD_ID
    )

    private var sentInitialPayloads = false
    private var payloadTypesRegistered = false

    override fun onToggled(state: Boolean): Boolean {
        if (state && FabricLoader.getInstance().isModLoaded(CLIENT_ID_MOD_ID)) {
            chat("ClientIDSpoofer is disabled because the ClientID mod is already installed.")
            return false
        }

        if (state && !ensurePayloadTypesRegistered()) {
            chat("ClientIDSpoofer cannot register ClientID payload channels.")
            return false
        }

        val acceptedState = super.onToggled(state)

        if (acceptedState) {
            mc.execute(::sendInitialPayloads)
        } else {
            sentInitialPayloads = false
        }

        return acceptedState
    }

    @Suppress("unused")
    private val loginHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING || event.packet !is ClientboundLoginPacket) {
            return@handler
        }

        mc.execute(::sendInitialPayloads)
    }

    @Suppress("unused")
    private val resourcePackReloadHandler = handler<ResourceReloadEvent> {
        if (sentInitialPayloads) {
            sendPackList()
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        sentInitialPayloads = false
    }

    private fun sendInitialPayloads() {
        if (mc.connection == null) {
            return
        }

        if (!running || sentInitialPayloads) {
            return
        }

        if (!ensurePayloadTypesRegistered()) {
            return
        }

        sentInitialPayloads = true
        ClientPlayNetworking.send(ClientIdModCheckPayload(mc.user.profileId.toString()))
        ClientPlayNetworking.send(ClientIdModListPayload(modList()))
        ClientPlayNetworking.send(ClientIdPackListPayload(packList()))
        ClientPlayNetworking.send(ClientIdVersionPayload(DEFAULT_VERSION))
    }

    private fun sendPackList() {
        if (!running) {
            return
        }

        if (mc.connection != null && ensurePayloadTypesRegistered()) {
            ClientPlayNetworking.send(ClientIdPackListPayload(packList()))
        }
    }

    private fun modList() = spoofedMods.normalizedEntries().joinToString(",")

    private fun packList() = mc.resourcePackRepository.selectedPacks.mapTo(mutableListOf()) { pack ->
        pack.title.string
    }.normalizedEntries().ifEmpty { listOf("Default") }.joinToString(",")

    private fun Iterable<String>.normalizedEntries() =
        flatMap { entry -> entry.split(',') }
            .map { entry -> entry.trim() }
            .filter { entry -> entry.isNotEmpty() }
            .distinct()

    private fun ensurePayloadTypesRegistered(): Boolean {
        if (payloadTypesRegistered) {
            return true
        }

        // Register lazily so the ClientID channels are not exposed while the spoofer is disabled.
        return runCatching {
            PayloadTypeRegistry.serverboundPlay().register(ClientIdModCheckPayload.ID, ClientIdModCheckPayload.CODEC)
            PayloadTypeRegistry.serverboundPlay().register(ClientIdModListPayload.ID, ClientIdModListPayload.CODEC)
            PayloadTypeRegistry.serverboundPlay().register(ClientIdPackListPayload.ID, ClientIdPackListPayload.CODEC)
            PayloadTypeRegistry.serverboundPlay().register(ClientIdVersionPayload.ID, ClientIdVersionPayload.CODEC)
            payloadTypesRegistered = true
        }.onFailure { error ->
            logger.debug("ClientID payload types could not be registered.", error)
        }.isSuccess
    }

}
