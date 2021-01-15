/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils.extensions

import com.mojang.authlib.Agent
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.thealtening.auth.TheAlteningAuthentication
import com.thealtening.auth.service.AlteningServiceType
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.utils.ProfileUtils
import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.util.Session
import java.net.Proxy

private val service = TheAlteningAuthentication.mojang()

private fun MinecraftSessionService.login(username: String, password: String = ""): LoginResult {
    if (password.isBlank())
        return loginCracked(username)

    val userAuthentication = YggdrasilAuthenticationService(Proxy.NO_PROXY, "")
        .createUserAuthentication(Agent.MINECRAFT)

    userAuthentication.setUsername(username)
    userAuthentication.setPassword(password)

    return try {
        userAuthentication.logIn()
        mc.session = Session(userAuthentication.selectedProfile.name, userAuthentication.selectedProfile.id.toString(),
            userAuthentication.authenticatedToken, "mojang")
        EventManager.callEvent(SessionEvent())
        LoginResult.LOGGED_IN
    } catch (exception: AuthenticationUnavailableException) {
        LoginResult.UNAVAILABLE_SERVICE
    } catch (exception: AuthenticationException) {
        val message = exception.message ?: return LoginResult.UNKNOWN_ISSUE

        when {
            message.contains("invalid username or password.", ignoreCase = true) -> LoginResult.INVALID_ACCOUNT_DATA
            message.contains("account migrated", ignoreCase = true) -> LoginResult.MIGRATED
            else -> LoginResult.UNAVAILABLE_SERVICE
        }
    }
}

fun MinecraftSessionService.loginMojang(username: String, password: String): LoginResult {
    val oldService = service()
    service.updateService(AlteningServiceType.MOJANG)
    return login(username, password)
        .also { if (it != LoginResult.LOGGED_IN) service.updateService(oldService) }
}

fun MinecraftSessionService.loginAltening(account: String): LoginResult {
    val oldService = service()
    service.updateService(AlteningServiceType.THEALTENING)
    return login(account, LiquidBounce.CLIENT_NAME)
        .also { if (it != LoginResult.LOGGED_IN) service.updateService(oldService) }
}

fun MinecraftSessionService.loginCracked(username: String): LoginResult {
    mc.session = Session(username, ProfileUtils.getUUID(username), "-", "legacy")
    EventManager.callEvent(SessionEvent())
    return LoginResult.LOGGED_IN
}

fun MinecraftSessionService.service() = service.service

enum class LoginResult(val readable: String) {
    LOGGED_IN("Successfully logged in"),
    UNAVAILABLE_SERVICE("Authentication service unavailable"),
    INVALID_ACCOUNT_DATA("Invalid username or password"),
    MIGRATED("Account migrated"),
    UNKNOWN_ISSUE("Unknown issue")
}
