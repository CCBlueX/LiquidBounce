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
import com.mojang.authlib.Environment
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.thealtening.api.TheAltening
import com.thealtening.api.TheAlteningException
import net.ccbluex.liquidbounce.config.gson.util.int
import net.ccbluex.liquidbounce.config.gson.util.string
import java.net.Proxy

const val ALTENING_AUTH = "http://authserver.thealtening.com"
const val ALTENING_SESSION = "http://sessionserver.thealtening.com"

val alteningEnvironment = Environment(
    ALTENING_SESSION,
    "https://api.minecraftservices.com",
    "https://api.minecraftservices.com",
    "PROD",
)

/**
 * An account from the TheAltening account generator.
 *
 * TheAltening still speaks the legacy Yggdrasil protocol, so the [accountToken] is exchanged for an
 * access token through [YggdrasilUserAuthentication] rather than through Microsoft.
 */
class AlteningAccount(var accountToken: String) : MinecraftAccount(AccountService.THEALTENING) {

    /**
     * Used for JSON deserialize.
     */
    @Suppress("unused")
    constructor() : this("")

    var accessToken = ""
        private set

    var hypixelLevel: Int = 0
        private set

    var hypixelRank: String = ""
        private set

    override val authenticationService: YggdrasilAuthenticationService by lazy {
        YggdrasilAuthenticationService(Proxy.NO_PROXY, alteningEnvironment)
    }

    override fun refresh() {
        val session = YggdrasilUserAuthentication(ALTENING_AUTH).authenticate(accountToken, "LiquidBounce")

        accessToken = session.accessToken
        username = session.profile.name
        profile = session.profile
    }

    override fun acquireAccessToken() = accessToken

    override fun toRawJson(json: JsonObject) = json.run {
        writeProfile()
        addProperty("token", accessToken)
        addProperty("hypixelLevel", hypixelLevel)
        addProperty("hypixelRank", hypixelRank)
    }

    override fun fromRawJson(json: JsonObject) = json.run {
        readProfile()
        accessToken = string("token") ?: throw IllegalArgumentException("'$this' has no access token")
        hypixelLevel = int("hypixelLevel") ?: 0
        hypixelRank = string("hypixelRank").orEmpty()
    }

    companion object {

        fun fromToken(accountToken: String) = AlteningAccount(accountToken).apply { refresh() }

        @Throws(TheAlteningException::class)
        fun generateAccount(apiToken: String): AlteningAccount {
            val alteningAccount = TheAltening.newBasicRetriever(apiToken).account
            return fromToken(alteningAccount.token)
        }

    }

}
