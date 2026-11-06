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

package net.ccbluex.liquidbounce.features.account

import com.mojang.authlib.GameProfile
import com.mojang.authlib.HttpDiscoveryService
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.exceptions.MinecraftClientException
import com.mojang.authlib.minecraft.client.MinecraftClient
import com.mojang.authlib.properties.PropertyMap
import com.mojang.authlib.services.MinecraftServicesDiscoveryService
import com.mojang.authlib.services.MinecraftServicesSessionService
import com.mojang.authlib.services.ProfileResult
import com.mojang.authlib.services.request.JoinMinecraftServerRequest
import com.mojang.authlib.services.response.HasJoinedMinecraftServerResponse
import com.mojang.authlib.services.response.MinecraftProfilePropertiesResponse
import com.mojang.authlib.services.response.ProfileAction
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.toUndashedString
import java.net.InetAddress
import java.net.Proxy
import java.net.URI
import java.util.UUID

/**
 * TheAltening exposes Yggdrasil session endpoints, not an Authlib 10 discovery document.
 * Keep Authlib's texture decoding and signature checks, but route session requests explicitly.
 *
 * Request/response handling follows [MinecraftServicesSessionService] as of the Authlib version this
 * module resolves (10.0.77): [MinecraftServicesSessionService.joinServer],
 * [MinecraftServicesSessionService.hasJoinedServer] and its `fetchProfileUncached`.
 * The fixed endpoint layout follows `com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService`,
 * which resolved the same Yggdrasil paths against a session host before Authlib 10 introduced
 * service discovery.
 */
internal class AlteningSessionService(
    discoveryService: MinecraftServicesDiscoveryService,
    sessionServer: URI = URI(ALTENING_SESSION),
) : MinecraftServicesSessionService(discoveryService.servicesKeySet, Proxy.NO_PROXY, discoveryService) {

    private val client = MinecraftClient.unauthenticated(Proxy.NO_PROXY)
    private val joinUrl = sessionServer.resolve("/session/minecraft/join").toURL()
    private val verifyUrl = sessionServer.resolve("/session/minecraft/hasJoined").toURL()
    private val profileUrl = sessionServer.resolve("/session/minecraft/profile/")

    override fun joinServer(profileId: UUID, authenticationToken: String, serverId: String) {
        try {
            client.post(joinUrl, JoinMinecraftServerRequest(authenticationToken, profileId, serverId), Void::class.java)
        } catch (exception: MinecraftClientException) {
            throw exception.toAuthenticationException()
        }
    }

    // Match Authlib: rejected sessions return null; only service unavailability propagates.
    @Suppress("SwallowedException")
    override fun hasJoinedServer(user: String, serverId: String, address: InetAddress?): ProfileResult? {
        val arguments = buildMap<String, Any>(3) {
            this["username"] = user
            this["serverId"] = serverId
            address?.let { this["ip"] = it.hostAddress }
        }
        val url = HttpDiscoveryService.concatenateURL(verifyUrl, HttpDiscoveryService.buildQuery(arguments))

        return try {
            val response = client.get(url, HasJoinedMinecraftServerResponse::class.java) ?: return null
            val id = response.id() ?: return null
            ProfileResult(
                GameProfile(id, user, response.properties ?: PropertyMap.EMPTY),
                extractProfileActionTypes(response.profileActions),
            )
        } catch (exception: MinecraftClientException) {
            val cause = exception.toAuthenticationException()
            if (cause is AuthenticationUnavailableException) throw cause
            null
        }
    }

    /**
     * Insecure profiles are not served from the 6 hour cache [MinecraftServicesSessionService] keeps,
     * because that cache and its `fetchProfileUncached` are private - every lookup reaches TheAltening
     * over HTTP instead.
     *
     * TODO: mirror the cache here if TheAltening's profile endpoint turns into a rate limit.
     */
    override fun fetchProfile(profileId: UUID, requireSecure: Boolean): ProfileResult? {
        val id = profileId.toUndashedString()
        val url = URI("$profileUrl$id?unsigned=${!requireSecure}").toURL()

        return try {
            val response = client.get(url, MinecraftProfilePropertiesResponse::class.java) ?: return null
            ProfileResult(response.profile(), extractProfileActionTypes(response.profileActions))
        } catch (exception: MinecraftClientException) {
            logger.warn("Couldn't look up profile properties for $profileId", exception)
            null
        }
    }

    /**
     * @see [MinecraftServicesSessionService.extractProfileActionTypes]
     */
    private fun extractProfileActionTypes(response: Set<ProfileAction>) =
        response.mapNotNullTo(enumSetOf()) { it.type }

}
