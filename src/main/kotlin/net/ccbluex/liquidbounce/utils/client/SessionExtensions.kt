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

package net.ccbluex.liquidbounce.utils.client

import com.mojang.authlib.Agent
import com.mojang.authlib.Environment
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService
import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.client.util.ProfileKeys
import net.minecraft.client.util.Session
import java.net.Proxy
import java.util.*

/**
 * Login to a Minecraft account
 *
 * @param username Username of the account
 * @param password Password of the account
 * @param environment Environment of the account
 * @return Pair of the session and the session service
 *
 * @throws AuthenticationUnavailableException If the authentication service is unavailable
 * @throws AuthenticationException If the account data is invalid
 * @throws Exception If an unknown issue occurs
 */
private fun MinecraftSessionService.login(username: String, password: String = "", environment: Environment) :
    Triple<Session, MinecraftSessionService?, ProfileKeys> {
    // If the password is blank, we assume that the account is cracked
    if (password.isBlank()) {
        val session = Session(
            username,
            MojangApi.getUUID(username),
            "-",
            Optional.empty(),
            Optional.empty(),
            Session.AccountType.LEGACY
        )
        val sessionService = YggdrasilAuthenticationService(
            Proxy.NO_PROXY, "", YggdrasilEnvironment.PROD.environment
        ).createMinecraftSessionService()

        return Triple(session, sessionService, ProfileKeys.MISSING)
    }

    // Create a new authentication service
    // We could use mc.sessionService, but we want to use our own environment.
    val authenticationService = YggdrasilAuthenticationService(Proxy.NO_PROXY, "", environment)

    // Create a new user authentication
    val userAuthentication = authenticationService.createUserAuthentication(Agent.MINECRAFT)
    userAuthentication.setUsername(username)
    userAuthentication.setPassword(password)

    // Logging in
    userAuthentication.logIn()

    // Create a new session
    val session = Session(
        userAuthentication.selectedProfile.name,
        userAuthentication.selectedProfile.id.toString(),
        userAuthentication.authenticatedToken,
        Optional.empty(),
        Optional.empty(),
        Session.AccountType.MOJANG
    )
    val sessionService = authenticationService.createMinecraftSessionService()

    var profileKeys = ProfileKeys.MISSING
    runCatching {
        // todo: fix support for altening (Failed to create profile keys for ... due to Status: 401)
        val userAuthenticationService = YggdrasilUserApiService(session.accessToken,
            mc.authenticationService.proxy, environment)
        profileKeys = ProfileKeys.create(userAuthenticationService, session, mc.runDirectory.toPath())
    }.onFailure {
        logger.error("Failed to create profile keys for ${session.username} due to ${it.message}")
    }

    return Triple(session, sessionService, profileKeys)
}

fun MinecraftSessionService.loginMojang(email: String, password: String) =
    login(email, password, YggdrasilEnvironment.PROD.environment)

fun MinecraftSessionService.loginAltening(account: String) =
    login(account, LiquidBounce.CLIENT_NAME, GenEnvironments.THE_ALTENING)

fun MinecraftSessionService.loginCracked(username: String) =
    login(username, "", YggdrasilEnvironment.PROD.environment)

/**
 * Account Generator environments
 */
enum class GenEnvironments(
    private val authHost: String,
    private val accountsHost: String,
    private val sessionHost: String,
    private val servicesHost: String
) : Environment {

    THE_ALTENING(
        "http://authserver.thealtening.com",
        "https://api.mojang.com",
        "http://sessionserver.thealtening.com",
        "https://api.minecraftservices.com"
    );

    override fun getAuthHost() = authHost
    override fun getAccountsHost() = accountsHost
    override fun getSessionHost() = sessionHost
    override fun getServicesHost() = servicesHost
    override fun getName() = name

    override fun asString() = StringJoiner(", ", "", "")
        .add("authHost='$authHost'")
        .add("accountsHost='$accountsHost'")
        .add("sessionHost='$sessionHost'")
        .add("servicesHost='$servicesHost'")
        .add("name='" + getName() + "'")
        .toString()

}
