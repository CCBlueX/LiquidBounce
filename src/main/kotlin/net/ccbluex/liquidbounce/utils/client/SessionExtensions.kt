/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.SessionEvent
import net.minecraft.client.util.Session
import java.net.Proxy
import java.util.*

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

private fun MinecraftSessionService.login(username: String, password: String = "", environment: Environment): LoginResult {
    if (password.isBlank()) {
        return loginCracked(username)
    }

    val authenticationService = YggdrasilAuthenticationService(Proxy.NO_PROXY, "", environment)
    val userAuthentication = authenticationService.createUserAuthentication(Agent.MINECRAFT)
    userAuthentication.setUsername(username)
    userAuthentication.setPassword(password)

    return try {
        userAuthentication.logIn()
        mc.session = Session(
            userAuthentication.selectedProfile.name,
            userAuthentication.selectedProfile.id.toString(),
            userAuthentication.authenticatedToken,
            Optional.empty(),
            Optional.empty(),
            Session.AccountType.MOJANG
        )
        mc.sessionService = authenticationService.createMinecraftSessionService()
        EventManager.callEvent(SessionEvent())
        LoginResult.LOGGED_IN
    } catch (exception: AuthenticationUnavailableException) {
        LoginResult.UNAVAILABLE_SERVICE
    } catch (exception: AuthenticationException) {
        val message = exception.message ?: return LoginResult.UNKNOWN_ISSUE

        when {
            message.contains("The provided token is invalid", ignoreCase = true) -> LoginResult.INVALID_GENERATOR_TOKEN
            message.contains("invalid username or password.", ignoreCase = true) -> LoginResult.INVALID_ACCOUNT_DATA
            message.contains("account migrated", ignoreCase = true) -> LoginResult.MIGRATED
            else -> LoginResult.UNAVAILABLE_SERVICE
        }
    }
}

fun MinecraftSessionService.loginMojang(email: String, password: String) =
    login(email, password, YggdrasilEnvironment.PROD.environment)

fun MinecraftSessionService.loginAltening(account: String) =
    login(account, LiquidBounce.CLIENT_NAME, GenEnvironments.THE_ALTENING)

fun MinecraftSessionService.loginCracked(username: String): LoginResult {
    mc.session = Session(username, MojangApi.getUUID(username), "-", Optional.empty(), Optional.empty(),
        Session.AccountType.LEGACY)
    EventManager.callEvent(SessionEvent())
    return LoginResult.LOGGED_IN
}

enum class LoginResult(val readable: String) {
    LOGGED_IN("Successfully logged in"),
    UNAVAILABLE_SERVICE("Authentication service unavailable"),
    INVALID_ACCOUNT_DATA("Invalid username or password"),
    MIGRATED("Account migrated"),
    UNKNOWN_ISSUE("Unknown issue"),
    INVALID_GENERATOR_TOKEN("Invalid account token")
}
