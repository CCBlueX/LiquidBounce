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

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mojang.authlib.GameProfile
import com.mojang.util.UndashedUuid
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.makeRequestBody
import net.ccbluex.liquidbounce.config.gson.util.readJson
import okhttp3.Request

data class YggdrasilSession(val profile: GameProfile, val accessToken: String)

/**
 * A Yggdrasil `/authenticate` client, used for alt services such as TheAltening. Mojang itself is not
 * authenticated against through here - MinecraftAuth does that.
 *
 * Documentation: https://wiki.vg/Authentication
 */
class YggdrasilUserAuthentication(val baseUrl: String) {

    private enum class Agent(
        @SerializedName("name")
        val agentName: String,
        val version: Int
    ) {
        MINECRAFT("Minecraft", 1)
    }

    private class AuthenticationRequest(
        val agent: Agent,
        val username: String,
        val password: String,
        val clientToken: String = clientIdentifier,
        val requestUser: Boolean = true
    )

    private class AuthenticationResponse(
        val accessToken: String,
        val clientToken: String,
        val availableProfiles: Array<Profile>,
        /**
         * Absent for a valid account without a Minecraft license - the authentication still succeeds.
         */
        val selectedProfile: Profile?
    ) {
        class Profile(
            val id: String,
            val name: String
        )
    }

    fun authenticate(username: String, password: String): YggdrasilSession {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }

        val request = Request.Builder()
            .url("$baseUrl/authenticate")
            .post(GSON.makeRequestBody(AuthenticationRequest(Agent.MINECRAFT, username, password)))
            .build()

        val response = HttpClient.client.newCall(request).execute().use {
            it.body.charStream().readJson<AuthenticationResponse>(GSON)
        }

        check(response.clientToken == clientIdentifier) { "Client identifier mismatch" }

        val selectedProfile = response.selectedProfile
        check(selectedProfile != null && response.availableProfiles.isNotEmpty()) {
            "Minecraft account not purchased"
        }

        val profile = GameProfile(UndashedUuid.fromStringLenient(selectedProfile.id), selectedProfile.name)
        return YggdrasilSession(profile, response.accessToken)
    }

    companion object {
        /**
         * Plain Gson: these are protocol types with no client-side adapters.
         */
        private val GSON = Gson()
    }

}
