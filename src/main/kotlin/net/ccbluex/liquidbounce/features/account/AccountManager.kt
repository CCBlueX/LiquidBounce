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
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerLoginResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerRemovalResultEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.screen.impl.MicrosoftLoginScreen
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.with
import net.minecraft.client.multiplayer.ProfileKeyPairManager
import java.net.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

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
        logger.info("Start logging in with username '${account.username}'")
        val (session, service) = account.login()

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
        EventManager.callEvent(AccountManagerLoginResultEvent(username = account.username))
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
        if (accounts.any { it.username.equals(username, true) }) {
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
     * Caches the current device code login URL, so re-triggering the flow while a login is already in
     * progress just re-shows the same URL/code instead of requesting a new one from Microsoft.
     */
    private var activeDeviceCodeUrl: String? = null

    /**
     * Whether a Microsoft WebView or Credentials sign-in is currently in progress. The device code flow
     * uses [activeDeviceCodeUrl] instead, since re-triggering it should reuse the existing code rather than
     * being rejected outright.
     */
    private val microsoftLoginInProgress = AtomicBoolean(false)

    /**
     * Create a new Microsoft account using the device code flow: the user is given a short code and a URL
     * to open on any device to complete sign-in. [url] receives the direct verification URL (which has the
     * code pre-filled) as soon as it is available. This blocks the calling thread only until that URL is
     * known; the account itself is created asynchronously once the user completes the sign-in elsewhere,
     * surfaced via [AccountManagerAdditionResultEvent].
     */
    fun newMicrosoftAccountViaDeviceCode(url: (String) -> Unit) {
        // Prevents you from starting multiple login attempts
        val existingUrl = activeDeviceCodeUrl
        if (existingUrl != null) {
            url(existingUrl)
            return
        }

        val urlReady = CountDownLatch(1)

        thread(name = "microsoft-account-device-code", isDaemon = true) {
            runCatching {
                MicrosoftAccount.buildFromDeviceCode(onDeviceCode = { code ->
                    activeDeviceCodeUrl = code.directVerificationUri
                    url(code.directVerificationUri)
                    urlReady.countDown()
                })
            }.onSuccess { account ->
                activeDeviceCodeUrl = null
                handleNewMicrosoftAccount(account)
            }.onFailure {
                activeDeviceCodeUrl = null
                logger.error("Failed to create new account", it)
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
            }

            // In case buildFromDeviceCode failed before ever reaching the onDeviceCode callback
            urlReady.countDown()
        }

        urlReady.await()
    }

    /**
     * Create a new Microsoft account by signing in on the Microsoft login page, shown in the client's own
     * browser. Runs asynchronously; the result is surfaced via [AccountManagerAdditionResultEvent].
     */
    fun newMicrosoftAccountViaWebView() {
        if (!microsoftLoginInProgress.compareAndSet(false, true)) {
            EventManager.callEvent(
                AccountManagerAdditionResultEvent(error = "A Microsoft sign-in is already in progress!")
            )
            return
        }

        if (!BrowserBackendManager.isInitialized) {
            microsoftLoginInProgress.set(false)
            EventManager.callEvent(
                AccountManagerAdditionResultEvent(error = "The browser is not available, use another sign-in method")
            )
            return
        }

        thread(name = "microsoft-account-webview", isDaemon = true) {
            runCatching {
                MicrosoftAccount.buildFromWebView(
                    onOpen = { service ->
                        val url = service.authenticationUrl.toString()
                        mc.execute {
                            mc.gui.setScreen(MicrosoftLoginScreen(url, service, mc.gui.screen()))
                        }
                    },
                    onClose = {
                        mc.execute { (mc.gui.screen() as? MicrosoftLoginScreen)?.onClose() }
                    },
                )
            }.onSuccess {
                handleNewMicrosoftAccount(it)
            }.onFailure {
                logger.error("Failed to create new account", it)
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
            }

            microsoftLoginInProgress.set(false)
        }
    }

    /**
     * Create a new Microsoft account by signing in directly with an email and password. Does not support
     * accounts with two-factor authentication enabled. Runs asynchronously; the result is surfaced via
     * [AccountManagerAdditionResultEvent].
     */
    fun newMicrosoftAccountViaCredentials(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Email and password are required!"))
            return
        }

        if (!microsoftLoginInProgress.compareAndSet(false, true)) {
            EventManager.callEvent(
                AccountManagerAdditionResultEvent(error = "A Microsoft sign-in is already in progress!")
            )
            return
        }

        thread(name = "microsoft-account-credentials", isDaemon = true) {
            runCatching {
                MicrosoftAccount.buildFromCredentials(email, password)
            }.onSuccess {
                handleNewMicrosoftAccount(it)
            }.onFailure {
                logger.error("Failed to create new account", it)
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
            }

            microsoftLoginInProgress.set(false)
        }
    }

    /**
     * Adds or replaces [account] in [accounts] and notifies the frontend. Shared by all Microsoft sign-in
     * flows.
     */
    private fun handleNewMicrosoftAccount(account: MicrosoftAccount) {
        val profile = account.profile
        if (profile == null) {
            logger.error("Failed to get profile")
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
            return
        }

        logger.info("Logged in as new account ${account.username}")

        val existingAccount = accounts.find {
            it.service == account.service && it.username == account.username
        }

        if (existingAccount != null) {
            // Replace existing account
            accounts[accounts.indexOf(existingAccount)] = account
        } else {
            // Add account to list of accounts
            accounts += account
        }

        // Store configurable
        ConfigSystem.store(this@AccountManager)

        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.name))
    }

    fun newAlteningAccount(accountToken: String) = runCatching {
        accounts += AlteningAccount.fromToken(accountToken).apply {
            val profile = this.profile

            if (profile == null) {
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                return@runCatching
            }

            EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.name))
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

        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.name))
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
        EventManager.callEvent(AccountManagerRemovalResultEvent(account.username))
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
        if (accounts.any { it.username.equals(account.username, true) }) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
            return
        }

        // Store configurable
        accounts += account
        ConfigSystem.store(this@AccountManager)
        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.name))
    }

}
