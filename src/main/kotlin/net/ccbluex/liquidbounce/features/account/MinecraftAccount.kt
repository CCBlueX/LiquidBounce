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
import com.mojang.util.UndashedUuid
import net.ccbluex.liquidbounce.config.gson.util.boolean
import net.ccbluex.liquidbounce.config.gson.util.obj
import net.ccbluex.liquidbounce.config.gson.util.string
import java.util.Optional

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
     * Name of the account. Known as soon as the account exists, unlike [profile], which is only
     * resolved once [refresh] has succeeded at least once.
     */
    var username: String = ""
        protected set

    /**
     * Resolved game profile, or `null` while the account has never been refreshed.
     */
    var profile: GameProfile? = null
        protected set

    /**
     * Whether the account is marked as favorite.
     */
    var favorite: Boolean = false
        private set

    var bans: MutableMap<String, Ban> = hashMapOf()
        internal set

    /**
     * Re-authenticates the account and updates [username] and [profile].
     */
    abstract fun refresh()

    /**
     * Authenticates the account and returns the session to hand to the game, along with the
     * authentication service that resolves other players' profiles while it is active.
     */
    abstract fun login(): Pair<SessionWithService, YggdrasilAuthenticationService>

    protected abstract fun toRawJson(json: JsonObject)

    protected abstract fun fromRawJson(json: JsonObject)

    fun toJson(): JsonObject = JsonObject().apply {
        toRawJson(this)
        addProperty("type", service.serialName)
        addProperty("favorite", favorite)
        add("bans", JsonObject().apply {
            bans.forEach { (serverName, ban) -> add(serverName, ban.toJson()) }
        })
    }

    /**
     * Builds the game session for this account. The account must have been refreshed before.
     */
    protected fun sessionOf(accessToken: String): SessionWithService {
        val profile = checkNotNull(profile) { "Account '$username' has not been refreshed" }

        return SessionWithService(
            profile.name, profile.id, accessToken,
            Optional.empty(),
            Optional.of(clientIdentifier),
            service,
        )
    }

    /**
     * Writes the account name and, when it has been resolved, the profile UUID.
     */
    protected fun JsonObject.writeProfile() {
        addProperty("name", username)
        profile?.let { addProperty("uuid", it.id.toString()) }
    }

    /**
     * Counterpart of [writeProfile]. Accounts saved before their first successful refresh have no
     * UUID, in which case [profile] stays `null` until the next [refresh].
     */
    protected fun JsonObject.readProfile() {
        username = string("name") ?: throw IllegalArgumentException("'$this' has no account name")
        profile = string("uuid")?.let { GameProfile(UndashedUuid.fromStringLenient(it), username) }
    }

    /**
     * Marks the account as a favorite.
     */
    fun favorite() = apply {
        favorite = true
    }

    /**
     * Marks the account as not a favorite.
     */
    fun unfavorite() = apply {
        favorite = false
    }

    /**
     * Tracks a ban, which should be called when the player is banned. The logic for this needs to be
     * implemented by the client itself.
     */
    fun trackBan(ban: Ban) {
        bans[ban.serverName] = ban
    }

    /**
     * Untracks a ban, which should be called when the player is able to join the server again.
     */
    fun untrackBan(serverName: String) {
        bans.remove(serverName)
    }

    /**
     * Checks if the player is banned on the specified server.
     */
    fun isBanned(serverName: String) = listActiveBans().any { it.serverName == serverName }

    /**
     * Returns a list of all active bans, dropping the ones that have expired.
     */
    fun listActiveBans(): List<Ban> {
        bans.values.removeIf { !it.isPermanent && it.bannedUntil < System.currentTimeMillis() }
        return bans.values.toList()
    }

    companion object {

        /**
         * Restores an account from its JSON representation.
         *
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

            if (json.boolean("favorite") == true) {
                account.favorite()
            }

            return account
        }

    }

}
