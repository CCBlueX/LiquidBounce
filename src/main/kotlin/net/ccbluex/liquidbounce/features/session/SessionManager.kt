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
package net.ccbluex.liquidbounce.features.session

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.cancelEventListenerScopes
import net.ccbluex.liquidbounce.event.events.SessionSwitchEvent
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
 * Keeps the sessions of the client. [main] is the one the game starts with and can never be closed.
 */
object SessionManager {

    val main = ClientSession(ClientSession.MAIN_ID)

    /**
     * The session the window shows and the user plays.
     */
    @Volatile
    var active: ClientSession = main
        private set

    private val openSessions = CopyOnWriteArrayList(listOf(main))

    val sessions: List<ClientSession>
        get() = openSessions

    private var nextId = ClientSession.MAIN_ID + 1

    @Volatile
    private var requestedSwitch: ClientSession? = null
    private val closing = ConcurrentLinkedQueue<ClientSession>()

    fun byId(id: Int): ClientSession? = openSessions.find { it.id == id }

    /**
     * Opens a session that plays as [account]. It starts in the background and without a connection.
     */
    fun open(account: SessionBundle?): ClientSession {
        check(SessionContext.isGameThread) { "Sessions are opened on the game thread" }

        val session = ClientSession(nextId++)
        session.account = account
        ModuleManager.copyTo(session)
        openSessions += session

        logger.info("Opened session ${session.id} for ${session.user.name}")
        return session
    }

    fun connect(session: ClientSession, address: String) {
        check(SessionContext.isGameThread) { "Sessions connect on the game thread" }
        require(ServerAddress.isValidAddress(address)) { "Invalid server address: $address" }

        SessionContext.within(session) {
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
     * Hands the window to [session] at the start of the next frame.
     */
    fun activate(session: ClientSession) {
        require(session in openSessions) { "$session is closed" }
        requestedSwitch = session
    }

    /**
     * Disconnects [session] and forgets it at the start of the next frame.
     */
    fun close(session: ClientSession) {
        require(!session.isMain) { "The main session cannot be closed" }
        closing += session
    }

    /**
     * Called once per frame, before the game ticks.
     */
    @JvmStatic
    fun runFrame() {
        while (true) {
            leave(closing.poll() ?: break)
        }

        requestedSwitch?.let { session ->
            requestedSwitch = null
            if (session in openSessions) {
                show(session)
            }
        }

        for (session in openSessions) {
            if (session.isActive) {
                continue
            }

            SessionContext.within(session) {
                repeat(session.dueTicks()) {
                    BackgroundGame.tick()
                }
            }
        }

        SessionOverview.announce()
    }

    /**
     * Leaves only the main session behind, so that Minecraft saves and closes its world as usual.
     */
    @JvmStatic
    fun shutdown() {
        for (session in openSessions) {
            if (!session.isMain) {
                leave(session)
            }
        }
    }

    private fun leave(session: ClientSession) {
        if (!openSessions.remove(session)) {
            return
        }
        if (session.isActive) {
            show(main)
        }

        SessionContext.within(session) {
            mc.level?.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE)
            mc.disconnectWithProgressScreen(false)
            cancelEventListenerScopes()
        }
        EventManager.unregisterSession(session)
        logger.info("Closed session ${session.id}")
    }

    private fun show(session: ClientSession) {
        val previous = active
        if (previous === session) {
            return
        }

        // Keys the user still holds would otherwise stay held in the session that leaves the window.
        KeyMapping.releaseAll()
        previous.restartClock()

        SessionContext.rest(session)
        active = session
        present()

        logger.info("Switched from session ${previous.id} to session ${session.id}")
        EventManager.callEvent(SessionSwitchEvent(session.id))
    }

    /**
     * Puts the loaded session on the window: its level into the renderer and its screen through
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
