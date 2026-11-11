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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.account.CrackedAccount
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext
import java.util.Properties

/**
 * A dedicated server that further clients of this game can join as offline accounts.
 */
fun ClientGameTestContext.createMultiClientServer(): TestDedicatedServerContext =
    worldBuilder().createServer(Properties().apply {
        setProperty("online-mode", "false")
        setProperty("white-list", "false")
        setProperty("max-players", "8")
    })

val TestDedicatedServerContext.address: String
    get() = "localhost:${computeOnServer<Int, RuntimeException> { it.port }}"

/**
 * Opens a client playing as the offline account [name] and lets it join [address].
 */
fun ClientGameTestContext.openClient(name: String, address: String): Client = fromClient {
    val account = CrackedAccount(name, false).also { it.refresh() }
    ClientManager.open(AccountManager.authenticate(account)).also { ClientManager.connect(it, address) }
}

