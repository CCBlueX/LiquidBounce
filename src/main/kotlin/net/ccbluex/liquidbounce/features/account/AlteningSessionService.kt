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
import java.net.InetAddress
import java.net.Proxy
import java.net.URI
import java.util.UUID

/**
 * TheAltening exposes Yggdrasil session endpoints, not an Authlib 10 discovery document.
 * Keep Authlib's texture decoding and signature checks, but route session requests explicitly.
 *
 * Request/response handling follows Mojang's [MinecraftServicesSessionService] in
 * [Authlib 10.0.77](https://libraries.minecraft.net/com/mojang/authlib/10.0.77/authlib-10.0.77.jar):
 * `joinServer`, `hasJoinedServer`, and `fetchProfileUncached`.
 * The fixed endpoint layout follows `com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService`
 * in [Authlib 9.0.75](https://libraries.minecraft.net/com/mojang/authlib/9.0.75/authlib-9.0.75.jar),
 * adapted to TheAltening's session host instead of Authlib 10 service discovery.
 */
internal class AlteningSessionService(
    discoveryService: MinecraftServicesDiscoveryService,
    sessionServer: URI = URI.create(ALTENING_SESSION),
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
        val arguments = mutableMapOf<String, Any>("username" to user, "serverId" to serverId)
        address?.let { arguments["ip"] = it.hostAddress }
        val url = HttpDiscoveryService.concatenateURL(verifyUrl, HttpDiscoveryService.buildQuery(arguments))

        return try {
            val response = client.get(url, HasJoinedMinecraftServerResponse::class.java) ?: return null
            val id = response.id() ?: return null
            ProfileResult(
                GameProfile(id, user, response.properties() ?: PropertyMap.EMPTY),
                response.profileActions().mapNotNull { it.type() }.toSet(),
            )
        } catch (exception: MinecraftClientException) {
            val cause = exception.toAuthenticationException()
            if (cause is AuthenticationUnavailableException) throw cause
            null
        }
    }

    override fun fetchProfile(profileId: UUID, requireSecure: Boolean): ProfileResult? {
        val id = profileId.toString().replace("-", "")
        val url = URI.create("$profileUrl$id?unsigned=${!requireSecure}").toURL()

        return try {
            val response = client.get(url, MinecraftProfilePropertiesResponse::class.java) ?: return null
            ProfileResult(response.profile(), response.profileActions().mapNotNull { it.type() }.toSet())
        } catch (_: MinecraftClientException) {
            null
        }
    }

}
