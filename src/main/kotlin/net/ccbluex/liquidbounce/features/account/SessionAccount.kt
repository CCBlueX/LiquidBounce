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

import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.gson.util.string
import net.raphimc.minecraftauth.java.model.MinecraftToken
import net.raphimc.minecraftauth.java.request.MinecraftProfileRequest

/**
 * A premium account represented by nothing but a Minecraft access token.
 *
 * The token cannot be refreshed - it is used as-is until it expires, at which point the account has
 * to be re-added.
 */
class SessionAccount(private var session: String) : MinecraftAccount(AccountService.SESSION) {

    /**
     * Used for JSON deserialize.
     */
    @Suppress("unused")
    constructor() : this("")

    override fun refresh() {
        // The token is opaque to us, so it is handed to MinecraftAuth as an already valid one.
        val token = MinecraftToken(Long.MAX_VALUE, "Bearer", session)
        val minecraftProfile = minecraftAuthHttpClient.executeAndHandle(MinecraftProfileRequest(token))

        username = minecraftProfile.name
        profile = GameProfile(minecraftProfile.id, minecraftProfile.name)
    }

    override fun acquireAccessToken() = session

    override fun toRawJson(json: JsonObject) = json.run {
        writeProfile()
        addProperty("accessToken", session)
    }

    override fun fromRawJson(json: JsonObject) = json.run {
        readProfile()
        session = string("accessToken") ?: throw IllegalArgumentException("'$this' has no access token")
    }

    companion object {
        fun fromToken(token: String) = SessionAccount(token).apply { refresh() }
    }

}
