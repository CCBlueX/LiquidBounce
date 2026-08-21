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
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment
import com.mojang.util.UndashedUuid
import net.ccbluex.liquidbounce.config.gson.util.boolean
import net.ccbluex.liquidbounce.config.gson.util.obj
import net.ccbluex.liquidbounce.config.gson.util.string
import java.net.Proxy
import java.util.Optional
import java.util.UUID

/**
 * Client token of every session we create. Randomised per launch, which is what the protocol expects
 * from a client that does not persist one.
 */
val clientIdentifier: String = UUID.randomUUID().toString()

/**
 * Constructing this fetches the environment's `/publickeys`, so it is shared by every account that
 * authenticates against Mojang rather than built per login.
 */
private val mojangAuthentication by lazy {
    YggdrasilAuthenticationService(Proxy.NO_PROXY, YggdrasilEnvironment.PROD.environment)
}

/**
 * An account the client can log into.
 *
 * Subclasses own their credentials and how those are refreshed. Everything shared between them - the
 * resolved profile, the favourite flag, ban tracking and JSON persistence - lives here.
 *
 * This was previously the `mc-authlib` library. The Microsoft/Xbox Live/XSTS/Minecraft token chain
 * behind [MicrosoftAccount] is implemented by
 * [MinecraftAuth](https://github.com/CCBlueX/minecraft-auth-java).
 */
@Suppress("TooManyFunctions")
sealed class MinecraftAccount(val service: AccountService) {

    /**
     * Known as soon as the account exists, unlike [profile], which is only resolved once [refresh] has
     * succeeded at least once.
     */
    var username: String = ""
        protected set

    var profile: GameProfile? = null
        protected set

    var favorite: Boolean = false

    val bans = hashMapOf<String, Ban>()

    protected open val authenticationService: YggdrasilAuthenticationService
        get() = mojangAuthentication

    abstract fun refresh()

    protected abstract fun acquireAccessToken(): String

    protected abstract fun toRawJson(json: JsonObject)

    protected abstract fun fromRawJson(json: JsonObject)

    /**
     * Authenticates the account and returns the session to hand to the game, along with the
     * authentication service that resolves other players' profiles while it is active.
     */
    fun login(): Pair<SessionWithService, YggdrasilAuthenticationService> {
        if (profile == null) {
            refresh()
        }

        val profile = checkNotNull(profile) { "Account '$username' has not been refreshed" }
        val session = SessionWithService(
            profile.name, profile.id, acquireAccessToken(),
            Optional.empty(),
            Optional.of(clientIdentifier),
            service,
        )

        return session to authenticationService
    }

    fun toJson(): JsonObject = JsonObject().apply {
        toRawJson(this)
        addProperty("type", service.serialName)
        addProperty("favorite", favorite)
        add("bans", JsonObject().apply {
            bans.forEach { (serverName, ban) -> add(serverName, ban.toJson()) }
        })
    }

    protected fun JsonObject.writeProfile() {
        addProperty("name", username)
        profile?.let { addProperty("uuid", it.id.toString()) }
    }

    /**
     * Accounts saved before their first successful refresh have no UUID, in which case [profile] stays
     * `null` until the next [refresh].
     */
    protected fun JsonObject.readProfile() {
        username = string("name") ?: throw IllegalArgumentException("'$this' has no account name")
        profile = string("uuid")?.let { GameProfile(UndashedUuid.fromStringLenient(it), username) }
    }

    /**
     * Tracking bans is up to the caller - this only stores and expires them.
     */
    fun trackBan(ban: Ban) {
        bans[ban.serverName] = ban
    }

    fun untrackBan(serverName: String) {
        bans.remove(serverName)
    }

    fun isBanned(serverName: String) = listActiveBans().any { it.serverName == serverName }

    fun listActiveBans(): List<Ban> {
        bans.values.removeIf { !it.isPermanent && it.bannedUntil < System.currentTimeMillis() }
        return bans.values.toList()
    }

    companion object {

        /**
         * @throws IllegalArgumentException if [json] is not a valid account
         */
        @JvmStatic
        fun fromJson(json: JsonObject): MinecraftAccount {
            val serialName = json.string("type")
                ?: throw IllegalArgumentException("'$json' is not a valid MinecraftAccount")

            val account = when (AccountService.bySerialName(serialName)) {
                AccountService.MICROSOFT -> MicrosoftAccount()
                AccountService.SESSION -> SessionAccount()
                AccountService.THEALTENING -> AlteningAccount()
                AccountService.CRACKED -> CrackedAccount()
                null -> throw IllegalArgumentException("Unknown account type '$serialName'")
            }

            account.fromRawJson(json)

            json.obj("bans")?.entrySet()?.forEach { (serverName, ban) ->
                account.bans[serverName] = Ban.fromJson(ban.asJsonObject)
            }

            account.favorite = json.boolean("favorite") == true

            return account
        }

    }

}
