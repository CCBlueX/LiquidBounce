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
package net.ccbluex.liquidbounce.features.client

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.cancelEventListenerScopes
import net.ccbluex.liquidbounce.event.events.ClientSwitchEvent
import net.ccbluex.liquidbounce.features.account.SessionBundle
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Keeps the clients of the client. [main] is the one the game starts with and can never be closed.
 */
object ClientManager {

    val main = Client(Client.MAIN_ID)

    /**
     * The client the window shows and the user plays.
     */
    @Volatile
    var active: Client = main
        private set

    private val openClients = CopyOnWriteArrayList(listOf(main))

    val clients: List<Client>
        get() = openClients

    private var nextId = Client.MAIN_ID + 1

    @Volatile
    private var requestedSwitch: Client? = null
    private val closing = ConcurrentLinkedQueue<Client>()

    fun byId(id: Int): Client? = openClients.find { it.id == id }

    /**
     * Opens a client that plays as [account]. It starts in the background and without a connection.
     */
    fun open(account: SessionBundle?): Client {
        check(ClientContext.isGameThread) { "Clients are opened on the game thread" }

        val client = Client(nextId++)
        client.account = account
        ModuleManager.copyTo(client)
        openClients += client

        logger.info("Opened client ${client.id} for ${client.user.name}")
        return client
    }

    fun connect(client: Client, address: String) {
        check(ClientContext.isGameThread) { "Clients connect on the game thread" }
        require(ServerAddress.isValidAddress(address)) { "Invalid server address: $address" }

        ClientContext.within(client) {
            ConnectScreen.startConnecting(
                TitleScreen(),
                mc,
                ServerAddress.parseString(address),
                ServerData(address, address, ServerData.Type.OTHER),
                false,
                null
            )
        }
    }

    /**
     * Hands the window to [client] at the start of the next frame.
     */
    fun activate(client: Client) {
        require(client in openClients) { "$client is closed" }
        requestedSwitch = client
    }

    /**
     * Disconnects [client] and forgets it at the start of the next frame.
     */
    fun close(client: Client) {
        require(!client.isMain) { "The main client cannot be closed" }
        closing += client
    }

    /**
     * Called once per frame, before the game ticks.
     */
    @JvmStatic
    fun runFrame() {
        while (true) {
            leave(closing.poll() ?: break)
        }

        requestedSwitch?.let { client ->
            requestedSwitch = null
            if (client in openClients) {
                show(client)
            }
        }

        for (client in openClients) {
            if (client.isActive) {
                continue
            }

            ClientContext.within(client) {
                repeat(client.dueTicks()) {
                    BackgroundGame.tick()
                }
            }
        }

        ClientOverview.announce()
    }

    /**
     * Leaves only the main client behind, so that Minecraft saves and closes its world as usual.
     */
    @JvmStatic
    fun shutdown() {
        for (client in openClients) {
            if (!client.isMain) {
                leave(client)
            }
        }
    }

    private fun leave(client: Client) {
        if (!openClients.remove(client)) {
            return
        }
        if (client.isActive) {
            show(main)
        }

        ClientContext.within(client) {
            mc.level?.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE)
            mc.disconnectWithProgressScreen(false)
            cancelEventListenerScopes()
        }
        EventManager.unregisterClient(client)
        logger.info("Closed client ${client.id}")
    }

    private fun show(client: Client) {
        val previous = active
        if (previous === client) {
            return
        }

        // Keys the user still holds would otherwise stay held in the client that leaves the window.
        KeyMapping.releaseAll()
        previous.restartClock()

        ClientContext.rest(client)
        active = client
        present()

        logger.info("Switched from client ${previous.id} to client ${client.id}")
        EventManager.callEvent(ClientSwitchEvent(client.id))
    }

    /**
     * Puts the loaded client on the window: its level into the renderer and its screen through
     * the regular path, which the screen never took while it was in the background.
     */
    private fun present() {
        val level = mc.level

        mc.soundManager.stop()
        mc.gameRenderer.resetData()
        mc.levelExtractor.setLevel(level)
        mc.particleEngine.setLevel(level)
        mc.gameRenderer.setLevel(level)
        mc.gameRenderer.checkEntityPostEffect(mc.cameraEntity)
        mc.gui.hud.clearTitles()
        mc.gui.hud.resetTitleTimes()

        val screen = mc.gui.screen
        mc.gui.screen = null
        mc.gui.setScreen(screen)
    }

}
