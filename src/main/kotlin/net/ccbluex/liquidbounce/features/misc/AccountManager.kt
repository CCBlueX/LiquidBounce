/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.authlib.account.AlteningAccount
import net.ccbluex.liquidbounce.authlib.account.CrackedAccount
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.authlib.yggdrasil.clientIdentifier
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.AltManagerUpdateEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.script.ScriptApi
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.session.ProfileKeys
import net.minecraft.client.session.Session
import java.net.Proxy
import java.util.*

object AccountManager : Configurable("Accounts"), Listenable {

    val accounts by value(name, mutableListOf<MinecraftAccount>(), listType = ListValueType.Account)

    var initialSession: SessionData? = null

    val sessionHandler = handler<SessionEvent> {
        if (initialSession == null) {
            initialSession = SessionData(mc.session, mc.sessionService, mc.profileKeys)
        }
    }

    init {
        ConfigSystem.root(this)
    }

    @ScriptApi
    @JvmName("loginAccountAsync")
    fun loginAccountAsync(id: Int) = GlobalScope.launch {
        loginAccount(id)
    }

    @ScriptApi
    @JvmName("loginAccount")
    fun loginAccount(id: Int) = runCatching {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        loginDirectAccount(account)
    }.onFailure {
        logger.error("Failed to login into account", it)
        EventManager.callEvent(AltManagerUpdateEvent(false, it.message ?: "Unknown error"))
    }.getOrThrow()

    @ScriptApi
    @JvmName("loginDirectAccount")
    fun loginDirectAccount(account: MinecraftAccount) = runCatching {
        val (compatSession, service) = account.login()
        val session = Session(
            compatSession.username, compatSession.uuid, compatSession.token,
            Optional.empty(),
            Optional.of(clientIdentifier),
            Session.AccountType.byName(compatSession.type)
        )

        var profileKeys = ProfileKeys.MISSING
        runCatching {
            // In this case the environment doesn't matter, as it is only used for the profile key
            val environment = YggdrasilEnvironment.PROD.environment
            val userAuthenticationService = YggdrasilUserApiService(session.accessToken, Proxy.NO_PROXY, environment)
            profileKeys = ProfileKeys.create(userAuthenticationService, session, mc.runDirectory.toPath())
        }.onFailure {
            logger.error("Failed to create profile keys for ${session.username} due to ${it.message}")
        }

        mc.session = session
        mc.sessionService = service.createMinecraftSessionService()
        mc.profileKeys = profileKeys

        EventManager.callEvent(SessionEvent())
        EventManager.callEvent(AltManagerUpdateEvent(true, "Logged in as ${account.profile?.username}"))
    }.onFailure {
        logger.error("Failed to login into account", it)
        EventManager.callEvent(AltManagerUpdateEvent(false, it.message ?: "Unknown error"))
    }.getOrThrow()

    /**
     * Cracked account. This can only be used to join cracked servers and not premium servers.
     */
    @ScriptApi
    @JvmName("newCrackedAccount")
    fun newCrackedAccount(username: String) {
        if (username.isEmpty()) {
            error("Username is empty!")
        }

        if (username.length > 16) {
            error("Username is too long!")
        }

        // Check if account already exists
        if (accounts.any { it.profile?.username.equals(username, true) }) {
            error("Account already exists!")
        }

        // Create new cracked account
        accounts += CrackedAccount(username).also { it.refresh() }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)

        EventManager.callEvent(AltManagerUpdateEvent(true, "Added new account: $username"))
    }

    @OptIn(DelicateCoroutinesApi::class)
    @ScriptApi
    @JvmName("loginCrackedAccountAsync")
    fun loginCrackedAccountAsync(username: String) {
        if (username.isEmpty()) {
            error("Username is empty!")
        }

        if (username.length > 16) {
            error("Username is too long!")
        }

        val account = CrackedAccount(username).also { it.refresh() }
        GlobalScope.launch {
            loginDirectAccount(account)
        }
    }

    /**
     * Cache microsoft login server
     */
    private var activeUrl: String? = null

    @ScriptApi
    @JvmName("newMicrosoftAccount")
    fun newMicrosoftAccount(url: (String) -> Unit) {
        // Prevents you from starting multiple login attempts
        val activeUrl = activeUrl
        if (activeUrl != null) {
            url(activeUrl)
            return
        }

        runCatching {
            newMicrosoftAccount(url = {
                this.activeUrl = it

                url(it)
            }, success = { account ->
                EventManager.callEvent(AltManagerUpdateEvent(true,
                    "Added new account: ${account.profile?.username}"))
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
    private fun newMicrosoftAccount(url: (String) -> Unit, success: (account: MicrosoftAccount) -> Unit,
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
                logger.info("Logged in as new account ${account.profile?.username}")

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

    @ScriptApi
    @JvmName("newAlteningAccount")
    fun newAlteningAccount(accountToken: String) = runCatching {
        accounts += AlteningAccount.fromToken(accountToken)

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)
    }.onFailure {
        logger.error("Failed to login into altening account (for add-process)", it)
        EventManager.callEvent(AltManagerUpdateEvent(false, it.message ?: "Unknown error"))
    }

    fun generateAlteningAccountAsync(apiToken: String) = GlobalScope.launch {
        generateAlteningAccount(apiToken)
    }

    @ScriptApi
    @JvmName("generateAlteningAccount")
    fun generateAlteningAccount(apiToken: String) = runCatching {
        if (apiToken.isEmpty()) {
            error("Altening API Token is empty!")
        }

        val account = AlteningAccount.generateAccount(apiToken).also { accounts += it }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)

        account
    }.onFailure {
        logger.error("Failed to generate altening account", it)
        EventManager.callEvent(AltManagerUpdateEvent(false, it.message ?: "Unknown error"))
    }.onSuccess {

        EventManager.callEvent(AltManagerUpdateEvent(true, "Added new account: ${it.profile?.username}"))
    }

    @ScriptApi
    @JvmName("restoreInitial")
    fun restoreInitial() {
        val initialSession = initialSession!!

        mc.session = initialSession.session
        mc.sessionService = initialSession.sessionService
        mc.profileKeys = initialSession.profileKeys
    }

    data class SessionData(val session: Session, val sessionService: MinecraftSessionService?,
                           val profileKeys: ProfileKeys)

}
