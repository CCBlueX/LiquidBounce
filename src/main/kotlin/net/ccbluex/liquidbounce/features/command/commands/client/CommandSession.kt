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
package net.ccbluex.liquidbounce.features.command.commands.client

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.account.CrackedAccount
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.SessionArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.session.SessionContext
import net.ccbluex.liquidbounce.features.session.SessionManager
import net.ccbluex.liquidbounce.features.session.SessionOverview
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.client.multiplayer.resolver.ServerAddress

/**
 * Session Command
 *
 * Opens further sessions next to the one the game started with, each playing as its own account,
 * and hands the window from one to another.
 */
object CommandSession : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("session") {
            literal("list") {
                exec {
                    list()
                    1
                }
            }
            literal("open") {
                argument("username", ClientStringArgumentType.word(), accountNames()) { username ->
                    optional("address", ClientStringArgumentType.word(), default = null) { address ->
                        execSuspend { ctx ->
                            open(ctx.get(username), ctx.get(address))
                        }
                    }
                }
            }
            literal("switch") {
                argument("session", SessionArgumentType) { session ->
                    exec { ctx ->
                        val target = ctx.get(session)
                        SessionManager.activate(target)
                        chat(regular(t("switch.success", variable(target.user.name))))
                        1
                    }
                }
            }
            literal("connect") {
                argument("session", SessionArgumentType) { session ->
                    argument("address", ClientStringArgumentType.word()) { address ->
                        exec { ctx ->
                            val target = ctx.get(session)
                            SessionManager.connect(target, validAddress(ctx.get(address)))
                            chat(regular(t("connect.success", variable(target.user.name), variable(ctx.get(address)))))
                            1
                        }
                    }
                }
            }
            literal("close") {
                argument("session", SessionArgumentType) { session ->
                    exec { ctx ->
                        val target = ctx.get(session)
                        if (target.isMain) {
                            throw CommandException(t("close.main"))
                        }

                        SessionManager.close(target)
                        chat(regular(t("close.success", variable(target.user.name))))
                        1
                    }
                }
            }
        }
    }

    private fun CmdI18n.list() {
        for (session in SessionManager.sessions) {
            val snapshot = SessionOverview.snapshot(session)
            chat(
                regular(if (session.isActive) "> " else "- "),
                variable(snapshot.username),
                regular(" (${session.id}) "),
                regular(t("list.${snapshot.status.name.lowercase()}", variable(snapshot.server ?: "")))
            )
        }
    }

    /**
     * [username] is an account of the alt manager, or the name of an offline account if there is none.
     */
    private suspend fun CmdI18n.open(username: String, address: String?) {
        address?.let { validAddress(it) }

        val account = AccountManager.accounts.firstOrNull { it.username.equals(username, true) }
            ?: CrackedAccount(username).also { it.refresh() }
        val login = withContext(Dispatchers.IO) { AccountManager.authenticate(account) }

        val session = SessionManager.open(login)
        (address ?: SessionContext.current.connection?.serverData?.ip)?.let { SessionManager.connect(session, it) }
        chat(regular(t("open.success", variable(session.user.name))))
    }

    private fun CmdI18n.validAddress(address: String): String {
        if (!ServerAddress.isValidAddress(address)) {
            throw CommandException(t("invalidAddress", variable(address)))
        }
        return address
    }

    private fun accountNames() = suggestions<ClientCommandSource> { AccountManager.accounts.map { it.username } }

}
