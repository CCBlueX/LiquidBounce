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

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService
import net.ccbluex.liquidbounce.authlib.account.AlteningAccount
import net.ccbluex.liquidbounce.authlib.account.CrackedAccount
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.authlib.account.SessionAccount
import net.ccbluex.liquidbounce.authlib.yggdrasil.clientIdentifier
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerLoginResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerRemovalResultEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.with
import net.minecraft.client.multiplayer.ProfileKeyPairManager
import java.net.Proxy
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
object AccountManager : Config("Accounts"), EventListener {

    val accounts by list(name, mutableListOf<MinecraftAccount>(), ValueType.ACCOUNT)

    private var initialSession: SessionBundle

    private val loggingIn = AtomicBoolean(false)

    init {
        ConfigSystem.root(this)

        try {
            initialSession = SessionBundle(mc.user, mc.services.sessionService, mc.profileKeyPairManager)
            logger.info("Initial session saved: ${mc.user.name} (${mc.user.profileId})")
        } catch (e: Exception) {
            logger.error("Failed to save initial session", e)
            initialSession = SessionBundle(mc.user, null, ProfileKeyPairManager.EMPTY_KEY_MANAGER)
        }
    }

    fun loginAccount(id: Int) {
        if (!loggingIn.compareAndSet(false, true)) {
            EventManager.callEvent(AccountManagerLoginResultEvent(error = "Logging in already started!"))
            return
        }

        val account = accounts.getOrNull(id) ?: run {
            EventManager.callEvent(AccountManagerLoginResultEvent(error = "Account not found!"))
            return
        }
        loginDirectAccount(account)
        loggingIn.set(false)
    }

    fun loginDirectAccount(account: MinecraftAccount) = try {
        logger.info("Start logging in with username '${account.profile?.username}'")
        val (compatSession, service) = account.login()
        val session = SessionWithService(
            compatSession.username, compatSession.uuid, compatSession.token,
            Optional.empty(),
            Optional.of(clientIdentifier),
            AccountService.getService(account)
        )

        val profileKeys = runCatching {
            // In this case the environment doesn't matter, as it is only used for the profile key
            val environment = YggdrasilEnvironment.PROD.environment
            val userAuthenticationService = YggdrasilUserApiService(session.accessToken, Proxy.NO_PROXY, environment)
            ProfileKeyPairManager.create(userAuthenticationService, session, mc.gameDirectory.toPath())
        }.onFailure {
            logger.error("Failed to create profile keys for ${session.name} due to ${it.message}")
        }.getOrDefault(ProfileKeyPairManager.EMPTY_KEY_MANAGER)

        mc.user = session
        mc.services = mc.services.with(
            service.createMinecraftSessionService(),
            service.servicesKeySet,
            service.createProfileRepository(),
        )
        mc.profileKeyPairManager = profileKeys

        EventManager.callEvent(SessionEvent(session))
        EventManager.callEvent(AccountManagerLoginResultEvent(username = account.profile?.username))
    } catch (e: Exception) {
        logger.error("Failed to login into account", e)
        EventManager.callEvent(AccountManagerLoginResultEvent(error = e.message ?: "Unknown error"))
    }

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

        // Check if account already exists
        if (accounts.any { it.profile?.username.equals(username, true) }) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
            return
        }

        // Create new cracked account
        accounts += CrackedAccount(username, online).also { it.refresh() }

        // Store configurable
        ConfigSystem.store(this@AccountManager)

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
        val account = if (token.startsWith("M.")) {
            MicrosoftAccount.buildFromRefreshToken(token)
        } else {
            SessionAccount(token).apply {
                refresh()
            }
        }

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
                ConfigSystem.store(this@AccountManager)
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
        ConfigSystem.store(this@AccountManager)
    }.onFailure {
        logger.error("Failed to login into altening account (for add-process)", it)
        EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
    }

    fun generateAlteningAccount(apiToken: String) = runCatching {
        if (apiToken.isEmpty()) {
            error("Altening API Token is empty!")
        }

        val account = AlteningAccount.generateAccount(apiToken)
        accounts += account

        // Store configurable
        ConfigSystem.store(this@AccountManager)

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

    fun restoreInitial() {
        val initialSession = initialSession
        mc.user = initialSession.session
        mc.services = mc.services.with(
            initialSession.sessionService ?: mc.services.sessionService
        )
        mc.profileKeyPairManager = initialSession.profileKeys

        EventManager.callEvent(SessionEvent(mc.user))
        EventManager.callEvent(AccountManagerLoginResultEvent(username = mc.user.name))
    }

    fun favoriteAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        account.favorite()
        ConfigSystem.store(this@AccountManager)
    }

    fun unfavoriteAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        account.unfavorite()
        ConfigSystem.store(this@AccountManager)
    }

    fun swapAccounts(index1: Int, index2: Int) {
        val account1 = accounts.getOrNull(index1) ?: error("Account not found!")
        val account2 = accounts.getOrNull(index2) ?: error("Account not found!")
        accounts[index1] = account2
        accounts[index2] = account1
        ConfigSystem.store(this@AccountManager)
    }

    fun orderAccounts(order: List<Int>) {
        order.map { index -> accounts[index] }
            .forEachIndexed { index, serverInfo ->
                accounts[index] = serverInfo
            }

        ConfigSystem.store(this@AccountManager)
    }

    fun removeAccount(id: Int): MinecraftAccount {
        val account = accounts.removeAt(id).apply { ConfigSystem.store(this@AccountManager) }
        EventManager.callEvent(AccountManagerRemovalResultEvent(account.profile?.username))
        return account
    }

    fun newSessionAccount(token: String) {
        if (token.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Token is empty!"))
            return
        }

        val account: MinecraftAccount = try {
            if (token.startsWith("M.")) {
                MicrosoftAccount.buildFromRefreshToken(token)
            } else {
                // Create a new cracked account
                SessionAccount(token).apply {
                    refresh()
                }
            }
        } catch (exception: Exception) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = exception.message ?: "Unknown error"))
            return
        }

        val profile = account.profile

        if (profile == null) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
            return
        }

        // Check if an account already exists
        if (accounts.any { it.profile?.username.equals(profile.username, true) }) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
            return
        }

        // Store configurable
        accounts += account
        ConfigSystem.store(this@AccountManager)
        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
    }

}
