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
package net.ccbluex.liquidbounce.features

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.authlib.account.AlteningAccount
import net.ccbluex.liquidbounce.authlib.account.CrackedAccount
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import org.apache.commons.lang3.RandomStringUtils

object Reconnect : Listenable {

    private var lastServer: ServerInfo? = null

    val handleServerConnect = handler<ServerConnectEvent> {
        lastServer = ServerInfo(it.serverName, it.serverAddress, ServerInfo.ServerType.OTHER)
    }

    /**
     * Reconnects to the last server. This is safe to call from every thread since it records a render call and
     * therefore runs in the Minecraft thread
     */
    fun reconnectNow() {
        val serverInfo = lastServer ?: error("no known last server")
        val serverAddress = ServerAddress.parse(serverInfo.address)

        RenderSystem.recordRenderCall {
            ConnectScreen.connect(
                MultiplayerScreen(TitleScreen()),
                mc,
                serverAddress,
                serverInfo,
                false,
                null
            )
        }
    }

    /**
     * Reconnects to the last server with a random account.
     *
     * This is safe to call from every thread since it records a render call and
     * therefore runs in the Minecraft thread
     */
    fun reconnectWithRandomPremiumAccount() {
        val account = AccountManager.accounts.filter {
            (it is MicrosoftAccount || it is AlteningAccount) && !it.favorite
        }.randomOrNull()
            ?: error("There are no accounts available")
        AccountManager.loginDirectAccount(account)

        reconnectNow()
    }

    /**
     * Reconnects to the last server with a random cracked username.
     *
     * This is safe to call from every thread since it records a render call and
     * therefore runs in the Minecraft thread
     */
    fun reconnectWithRandomCracked() {
        // Random 7-16 character alphabetic username
        // todo: make realistic usernames
        val username = RandomStringUtils.randomAlphanumeric(7, 16)

        val account = CrackedAccount(username).also { it.refresh() }
        AccountManager.loginDirectAccount(account)

        reconnectNow()
    }

}
