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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.services.auth.OAuthClient.startAuth
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.joinToText
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object CommandClientAccountSubcommand {
    fun accountCommand() = CommandBuilder.begin("account")
        .hub()
        .subcommand(loginSubcommand())
        .subcommand(logoutSubcommand())
        .subcommand(infoSubcommand())
        .build()

    private fun infoSubcommand() = CommandBuilder.begin("info")
        .suspendHandler {
            if (ClientAccountManager.clientAccount == EMPTY_ACCOUNT) {
                chat(regular("You are not logged in."))
                return@suspendHandler
            }

            chat(regular("Getting user information..."))
            try {
                val account = ClientAccountManager.clientAccount
                account.updateInfo()
                account.userInformation?.let { info ->
                    info.nickname?.let { nickname -> chat(regular("Nickname: "), variable(nickname)) }
                    chat(regular("Email: "), variable(info.email))
                    chat(regular("User ID: "), variable(info.userId))
                    chat(regular("Groups: "), info.groups.map(::variable).joinToText(", ".asText()))
                    chat(regular("Premium: "), variable(if (info.premium) "Yes" else "No"))
                }
            } catch (e: HttpException) {
                chat(markAsError("Failed to get user information: ${e.content}"))
            }
        }.build()

    private fun logoutSubcommand() = CommandBuilder.begin("logout")
        .suspendHandler {
            if (ClientAccountManager.clientAccount == EMPTY_ACCOUNT) {
                chat(regular("You are not logged in."))
                return@suspendHandler
            }

            chat(regular("Logging out..."))
            withContext(Dispatchers.IO) {
                ClientAccountManager.clientAccount = EMPTY_ACCOUNT
                ConfigSystem.store(ClientAccountManager)
                EventManager.callEvent(UserLoggedOutEvent)
                chat(regular("Successfully logged out."))
            }
        }.build()

    private fun loginSubcommand() = CommandBuilder.begin("login")
        .suspendHandler {
            if (ClientAccountManager.clientAccount != EMPTY_ACCOUNT) {
                chat(regular("You are already logged in."))
                return@suspendHandler
            }

            chat(regular("Starting OAuth authorization process..."))
            val account = startAuth { browseUrl(it) }
            ClientAccountManager.clientAccount = account
            ConfigSystem.store(ClientAccountManager)
            EventManager.callEvent(UserLoggedInEvent)
            chat(regular("Successfully authorized client."))
        }.build()
}
