/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.misc

import com.google.gson.JsonObject
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService
import com.thealtening.api.TheAltening
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.compat.Session
import me.liuli.elixir.utils.int
import me.liuli.elixir.utils.set
import me.liuli.elixir.utils.string
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.event.AltManagerUpdateEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.script.RequiredByScript
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.client.util.ProfileKeys
import java.net.Proxy
import java.util.*

object AccountManager : Configurable("Accounts") {

    val accounts by value(name, mutableListOf<MinecraftAccount>(), listType = ListValueType.Account)

    // Account Generator
    var alteningApiToken by value("TheAlteningApiToken", "")

    init {
        ConfigSystem.root(this)
    }

    @RequiredByScript
    fun loginAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: return

        // TODO: Implement directly into Elixir
        val (session, sessionService, profileKeys) = when (account) {
            is CrackedAccount -> mc.sessionService.loginCracked(account.session.username)

            is MicrosoftAccount -> {
                val session = net.minecraft.client.util.Session(
                    account.name,
                    account.session.uuid,
                    account.session.token,
                    Optional.empty(),
                    Optional.empty(),
                    net.minecraft.client.util.Session.AccountType.MSA
                )
                val sessionService = YggdrasilAuthenticationService(
                    Proxy.NO_PROXY, "", YggdrasilEnvironment.PROD.environment
                ).createMinecraftSessionService()

                var profileKeys = ProfileKeys.MISSING
                runCatching {
                    val userAuthenticationService = YggdrasilUserApiService(session.accessToken,
                        mc.authenticationService.proxy, YggdrasilEnvironment.PROD.environment)
                    profileKeys = ProfileKeys.create(userAuthenticationService, session, mc.runDirectory.toPath())
                }.onFailure {
                    logger.error("Failed to create profile keys for ${session.username} due to ${it.message}")
                }

                Triple(session, sessionService, profileKeys)
            }

            is AlteningAccount -> {
                val (session, sessionService, keys) = mc.sessionService.loginAltening(account.token)

                // Update account info
                account.name = session.username
                account.uuid = session.uuid

                Triple(session, sessionService, keys)
            }
            else -> error("Unknown account type: ${account::class.simpleName}")
        }

        mc.session = session
        mc.sessionService = sessionService
        mc.profileKeys = profileKeys
    }

    /**
     * Cracked account. This can only be used to join cracked servers and not premium servers.
     */
    fun newCrackedAccount(username: String) {
        // Get UUID of username from Mojang API
        val uuid = runCatching {
            MojangApi.getUUID(username)
        }.getOrNull() ?: UUID.randomUUID()

        // Create new cracked account
        accounts += CrackedAccount().also { account ->
            account.name = username
        }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)
    }

    /**
     * Cache microsoft login server
     */
    private var activeUrl: String? = null

    @RequiredByScript
    fun newMicrosoftAccount() {
        // Prevents you from starting multiple login attempts
        val activeUrl = activeUrl
        if (activeUrl != null) {
            browseUrl(activeUrl)
            return
        }

        runCatching {
            newMicrosoftAccount(url = {
                this.activeUrl = it

                browseUrl(it)
            }, success = { account ->
                EventManager.callEvent(AltManagerUpdateEvent(true, "Added new account: ${account.name}"))
                this.activeUrl = null
            }, error = { errorString ->
                EventManager.callEvent(AltManagerUpdateEvent(false, errorString))
                this.activeUrl = null
            })
        }.onFailure {
            logger.error("Failed to create new account", it)
            EventManager.callEvent(AltManagerUpdateEvent(false, it.message ?: "Unknown error"))
            this.activeUrl = null
        }
    }

    /**
     * Create a new Microsoft Account using the OAuth2 flow which opens a browser window to authenticate the user
     */
    fun newMicrosoftAccount(url: (String) -> Unit, success: (account: MicrosoftAccount) -> Unit,
                            error: (error: String) -> Unit) {
        MicrosoftAccount.buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler {

            /**
             * Called when the user has cancelled the authentication process or the thread has been interrupted
             */
            override fun authError(error: String) {
                // Oh no, something went wrong. Callback with error.
                logger.error("Failed to login: $error")
                error(error)
            }

            /**
             * Called when the user has completed authentication
             */
            override fun authResult(account: MicrosoftAccount) {
                // Yay, it worked! Callback with account.
                logger.info("Logged in as new account ${account.name}")

                // Add account to list of accounts
                accounts += account

                runCatching {
                    success(account)
                }.onFailure {
                    logger.error("Internal error", it)
                }

                // Store configurable
                ConfigSystem.storeConfigurable(this@AccountManager)
            }

            /**
             * Called when the server has prepared the user for authentication
             */
            override fun openUrl(url: String) {
                url(url)
            }

        })
    }

    fun newAlteningAccount(accountToken: String) {
        // Check if altening token is valid
        val (session) = mc.sessionService.loginAltening(accountToken)

        accounts += AlteningAccount().also { account ->
            account.token = accountToken
            account.name = session.username
            account.uuid = session.uuid
        }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)
    }

    fun generateNewAlteningAccount(apiToken: String = this.alteningApiToken) {
        if (apiToken.isEmpty()) {
            error("Altening API Token is empty!")
        }

        val alteningAccount = TheAltening.newBasicRetriever(apiToken).account

        accounts += AlteningAccount().also { account ->
            account.name = alteningAccount.username
            account.token = alteningAccount.token

            account.hypixelRank = alteningAccount.info.hypixelRank ?: ""
            account.hypixelLevel = alteningAccount.info.hypixelLevel
        }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)
    }

}

/**
 * Altening Account
 */
class AlteningAccount : MinecraftAccount("Altening") {

    override var name = ""

    var token = ""
    var uuid = ""

    var hypixelLevel: Int = 0
    var hypixelRank: String = ""

    /**
     * get the session of the account
     */
    override val session: Session
        get() = Session(name, uuid, "", "mojang")

    /**
     * load the account data from json
     * @param json contains the account data
     */
    override fun fromRawJson(json: JsonObject) {
        name = json.string("name")!!
        token = json.string("token")!!
        hypixelLevel = json.int("hypixelLevel")!!
        hypixelRank = json.string("hypixelRank")!!
    }

    /**
     * save the account data to json
     * @param json needs to write data in
     */
    override fun toRawJson(json: JsonObject) {
        json["name"] = name
        json["token"] = token
        json["hypixelLevel"] = hypixelLevel
        json["hypixelRank"] = hypixelRank
    }

    /**
     * login with this account info
     * @throws me.liuli.elixir.exception.LoginException if login failed
     */
    override fun update() {
        // Login into account
        val (session, _) = mc.sessionService.loginAltening(token)

        // Update account info
        name = session.username
        uuid = session.uuid
    }

}
