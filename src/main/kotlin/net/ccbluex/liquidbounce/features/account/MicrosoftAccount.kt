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
import net.ccbluex.liquidbounce.config.gson.util.obj
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.data.MsaConstants
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig
import net.raphimc.minecraftauth.msa.model.MsaCredentials
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import net.raphimc.minecraftauth.msa.service.impl.ExternalBrowserMsaAuthService
import java.net.Proxy
import java.util.function.Consumer

/**
 * A premium account authenticated through a Microsoft account.
 *
 * Authentication is delegated to [MinecraftAuth](https://github.com/CCBlueX/minecraft-auth-java),
 * which implements the full Microsoft -> Xbox Live -> XSTS -> Minecraft token exchange. Use one of the
 * `buildFrom*` factory functions to sign in; all of them block the calling thread until the sign-in
 * completes, so none of them may be called on the render thread.
 */
class MicrosoftAccount internal constructor(
    private var authManager: JavaAuthManager?
) : MinecraftAccount(AccountService.MICROSOFT) {

    /**
     * Used for JSON deserialize.
     */
    @Suppress("unused")
    constructor() : this(null)

    private fun requireAuthManager() =
        checkNotNull(authManager) { "Microsoft account has not been signed in" }

    /**
     * Refreshes the Microsoft/Xbox/Minecraft token chain and updates the account's [profile].
     */
    override fun refresh() {
        val manager = requireAuthManager()

        manager.minecraftToken.refresh()
        val minecraftProfile = manager.minecraftProfile.refresh()

        username = minecraftProfile.name
        profile = GameProfile(minecraftProfile.id, minecraftProfile.name)
    }

    override fun login(): Pair<SessionWithService, YggdrasilAuthenticationService> {
        val manager = requireAuthManager()

        if (profile == null) {
            refresh()
        }

        val token = manager.minecraftToken.getUpToDate()
        val service = YggdrasilAuthenticationService(Proxy.NO_PROXY, YggdrasilEnvironment.PROD.environment)

        return sessionOf(token.token) to service
    }

    /**
     * Saves the account data. The full [JavaAuthManager] state (MSA refresh token and all cached
     * Xbox/Minecraft tokens) is embedded so that the session can be restored without requiring the
     * user to sign in again.
     */
    override fun toRawJson(json: JsonObject) = json.run {
        writeProfile()
        add("authManager", JavaAuthManager.toJson(requireAuthManager()))
    }

    override fun fromRawJson(json: JsonObject) = json.run {
        readProfile()

        val authManagerJson = obj("authManager") ?: throw IllegalArgumentException(
            "This Microsoft account was saved by an older version and can no longer be restored. " +
                "Please sign in again."
        )
        authManager = JavaAuthManager.fromJson(minecraftAuthHttpClient, authManagerJson)
    }

    companion object {

        /**
         * The official Minecraft (Java Edition) launcher application, using the client ID and scope
         * constants provided by the MinecraftAuth library.
         */
        val JAVA_APPLICATION_CONFIG: MsaApplicationConfig =
            MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH)

        private const val DEFAULT_TIMEOUT_MS = 300_000

        /**
         * Signs in through a browser the caller supplies. This is the preferred sign-in method, as it
         * supports 2FA, passkeys and every other login method Microsoft offers, without any custom
         * setup.
         *
         * [onOpen] is called on the calling thread and has to display
         * [ExternalBrowserMsaAuthService.getAuthenticationUrl] and report the URLs it navigates to back
         * to the service; [onClose] is called once the sign-in has finished, failed or timed out.
         *
         * @param timeoutMs How long to wait for the user to complete the login before giving up.
         */
        fun buildFromWebView(
            onOpen: (ExternalBrowserMsaAuthService) -> Unit,
            onClose: (ExternalBrowserMsaAuthService) -> Unit,
            applicationConfig: MsaApplicationConfig = JAVA_APPLICATION_CONFIG,
            timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        ): MicrosoftAccount = build(applicationConfig) {
            it.login { httpClient, config ->
                ExternalBrowserMsaAuthService(httpClient, config, onOpen, onClose, timeoutMs)
            }
        }

        /**
         * Signs in with the Microsoft device code flow. [onDeviceCode] is invoked once with the code
         * the user needs to enter at the returned verification URL; this call blocks until the user
         * completes the sign-in, the device code expires, or [timeoutMs] elapses.
         */
        fun buildFromDeviceCode(
            onDeviceCode: Consumer<MsaDeviceCode>,
            applicationConfig: MsaApplicationConfig = JAVA_APPLICATION_CONFIG,
            timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        ): MicrosoftAccount = build(applicationConfig) {
            it.login(
                { httpClient, config, callback -> DeviceCodeMsaAuthService(httpClient, config, callback, timeoutMs) },
                onDeviceCode,
            )
        }

        /**
         * Signs in directly with a Microsoft email and password. Does not support accounts with
         * two-factor authentication enabled; use [buildFromWebView] or [buildFromDeviceCode] instead
         * in that case.
         */
        fun buildFromCredentials(
            email: String,
            password: String,
            applicationConfig: MsaApplicationConfig = JAVA_APPLICATION_CONFIG,
        ): MicrosoftAccount = build(applicationConfig) {
            it.login(
                { httpClient, config, credentials -> CredentialsMsaAuthService(httpClient, config, credentials) },
                MsaCredentials(email, password),
            )
        }

        /**
         * Restores a session from a previously obtained Microsoft OAuth refresh token. The
         * [applicationConfig] must match the one the refresh token was originally issued for.
         */
        fun buildFromRefreshToken(
            refreshToken: String,
            applicationConfig: MsaApplicationConfig = JAVA_APPLICATION_CONFIG,
        ): MicrosoftAccount = build(applicationConfig) { it.login(refreshToken) }

        private fun build(
            applicationConfig: MsaApplicationConfig,
            login: (JavaAuthManager.Builder) -> JavaAuthManager,
        ): MicrosoftAccount {
            val builder = JavaAuthManager.create(minecraftAuthHttpClient).msaApplicationConfig(applicationConfig)
            return MicrosoftAccount(login(builder)).apply { refresh() }
        }

    }

}
