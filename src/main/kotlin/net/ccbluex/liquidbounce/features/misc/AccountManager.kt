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
import net.ccbluex.liquidbounce.authlib.account.*
import net.ccbluex.liquidbounce.authlib.yggdrasil.clientIdentifier
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.AccountManagerAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerLoginResultEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.event.handler
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

    fun loginAccount(id: Int) = runCatching {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        loginDirectAccount(account)
    }.onFailure {
        logger.error("Failed to login into account", it)
        EventManager.callEvent(AccountManagerLoginResultEvent(error = it.message ?: "Unknown error"))
    }.getOrThrow()

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

        EventManager.callEvent(SessionEvent(session))
        EventManager.callEvent(AccountManagerLoginResultEvent(username = account.profile?.username))
    }.onFailure {
        logger.error("Failed to login into account", it)
        EventManager.callEvent(AccountManagerLoginResultEvent(error = it.message ?: "Unknown error"))
    }.getOrThrow()

    private val USERNAME_REGEX = Regex("[a-zA-z0-9_]{1,16}")

    /**
     * Cracked account. This can only be used to join cracked servers and not premium servers.
     */
    fun newCrackedAccount(username: String, online: Boolean = false) {
        if (username.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is empty!"))
            return
        }

        if (username.length > 16) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is too long!"))
            return
        }

        if (!USERNAME_REGEX.matches(username)) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username contains invalid characters!"))
            return
        }

        // Check if account already exists
        if (accounts.any { it.profile?.username.equals(username, true) }) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
            return
        }

        // Create new cracked account
        accounts += CrackedAccount(username, online).also { it.refresh() }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)

        EventManager.callEvent(AccountManagerAdditionResultEvent(username = username))
    }

    fun loginCrackedAccount(username: String, online: Boolean = false) {
        if (username.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is empty!"))
            return
        }

        if (username.length > 16) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is too long!"))
            return
        }

        val account = CrackedAccount(username, online).also { it.refresh() }
        loginDirectAccount(account)
    }

    fun loginSessionAccount(token: String) {
        val account = SessionAccount(token).also { it.refresh() }
        loginDirectAccount(account)
    }

    fun loginEasyMCAccount(token: String) {
        val account = EasyMCAccount.fromToken(token).also { it.refresh() }
        loginDirectAccount(account)
    }

    /**
     * Cache microsoft login server
     */
    private var activeUrl: String? = null

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
                val profile = account.profile
                if (profile == null) {
                    logger.error("Failed to get profile")
                    EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                    return@newMicrosoftAccount
                }

                EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
                this.activeUrl = null
            }, error = { errorString ->
                logger.error("Failed to create new account: $errorString")

                EventManager.callEvent(AccountManagerAdditionResultEvent(error = errorString))
                this.activeUrl = null
            })
        }.onFailure {
            logger.error("Failed to create new account", it)

            EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
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

                val existingAccount = accounts.find {
                    it.type == account.type && it.profile?.username == account.profile?.username
                }

                if (existingAccount != null) {
                    // Replace existing account
                    accounts[accounts.indexOf(existingAccount)] = account
                } else {
                    // Add account to list of accounts
                    accounts += account
                }

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

    fun newAlteningAccount(accountToken: String) = runCatching {
        accounts += AlteningAccount.fromToken(accountToken).apply {
            val profile = this.profile

            if (profile == null) {
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                return@runCatching
            }

            EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
        }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)
    }.onFailure {
        logger.error("Failed to login into altening account (for add-process)", it)
        EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
    }

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
        EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
    }.onSuccess {
        val profile = it.profile

        if (profile == null) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
            return@onSuccess
        }

        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
    }

    fun newEasyMCAccount(accountToken: String) = runCatching {
        accounts += EasyMCAccount.fromToken(accountToken).apply {
            val profile = this.profile

            if (profile == null) {
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                return@runCatching
            }

            EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
        }

        // Store configurable
        ConfigSystem.storeConfigurable(this@AccountManager)
    }.onFailure {
        logger.error("Failed to login into EasyMC account (for add-process)", it)
        EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
    }

    fun restoreInitial() {
        val initialSession = initialSession!!

        mc.session = initialSession.session
        mc.sessionService = initialSession.sessionService
        mc.profileKeys = initialSession.profileKeys

        EventManager.callEvent(SessionEvent(mc.session))
        EventManager.callEvent(AccountManagerLoginResultEvent(username = mc.session.username))
    }

    fun favoriteAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        account.favorite()
        ConfigSystem.storeConfigurable(this@AccountManager)
    }

    fun unfavoriteAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        account.unfavorite()
        ConfigSystem.storeConfigurable(this@AccountManager)
    }

    fun swapAccounts(index1: Int, index2: Int) {
        val account1 = accounts.getOrNull(index1) ?: error("Account not found!")
        val account2 = accounts.getOrNull(index2) ?: error("Account not found!")
        accounts[index1] = account2
        accounts[index2] = account1
        ConfigSystem.storeConfigurable(this@AccountManager)
    }

    fun orderAccounts(order: List<Int>) {
        order.map { index -> accounts[index] }
            .forEachIndexed { index, serverInfo ->
                accounts[index] = serverInfo
            }

        ConfigSystem.storeConfigurable(this@AccountManager)
    }

    fun removeAccount(id: Int): MinecraftAccount {
        return accounts.removeAt(id).apply { ConfigSystem.storeConfigurable(this@AccountManager) }
    }

    fun newSessionAccount(token: String) {
        if (token.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Token is empty!"))
            return
        }

        // Create new cracked account
        accounts += SessionAccount(token).also { it.refresh() }.apply {
            val profile = this.profile

            if (profile == null) {
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                return
            }

            // Check if account already exists
            if (accounts.any { it.profile?.username.equals(profile.username, true) }) {
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
                return
            }

            // Store configurable
            ConfigSystem.storeConfigurable(this@AccountManager)

            EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
        }
    }

    data class SessionData(val session: Session, val sessionService: MinecraftSessionService?,
                           val profileKeys: ProfileKeys)

}
