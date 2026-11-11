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

import net.ccbluex.liquidbounce.api.core.ApiConfig
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.SessionsUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.ServerObserver
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component

private const val ANNOUNCE_INTERVAL_NANOS = 250_000_000L
private const val MOVING_SPEED_SQR = 1.0E-4

/**
 * Describes what the sessions are doing, which is all the user sees of the ones in the background.
 */
object SessionOverview : EventListener {

    private var announcedAt = 0L
    private var announcedSeveral = false

    /**
     * The last line a server wrote into the chat of the session.
     */
    private var lastMessage: Component? by sessionState { null }

    /**
     * Has to be asked on the game thread.
     */
    fun snapshots(): List<SessionSnapshot> = SessionManager.sessions.map(::snapshot)

    fun snapshot(session: ClientSession): SessionSnapshot = SessionContext.within(session) { describe(session) }

    /**
     * Tells the theme what the sessions are doing, and once more when only one is left.
     */
    internal fun announce() {
        val several = SessionManager.sessions.size > 1
        if (!several && !announcedSeveral) {
            return
        }

        val now = System.nanoTime()
        if (several && now - announcedAt < ANNOUNCE_INTERVAL_NANOS) {
            return
        }

        announcedAt = now
        announcedSeveral = several
        EventManager.callEvent(SessionsUpdateEvent(snapshots()))
    }

    @Suppress("unused")
    private val chatHandler = handler<ChatReceiveEvent>(priority = READ_FINAL_STATE) { event ->
        if (!event.isCancelled) {
            lastMessage = event.applyChatDecoration.apply(event.textData)
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        lastMessage = null
    }

    /**
     * Reads the game of the loaded session, which has to be [session].
     */
    private fun describe(session: ClientSession): SessionSnapshot {
        val player = mc.player?.takeIf { mc.level != null }
        val screen = mc.gui.screen()
        val user = session.user

        return SessionSnapshot(
            id = session.id,
            username = user.name,
            avatar = ApiConfig.formatAvatarUrl(user.profileId, user.name),
            active = session.isActive,
            status = when {
                player != null -> SessionSnapshot.Status.IN_GAME
                screen is DisconnectedScreen -> SessionSnapshot.Status.DISCONNECTED
                screen is ConnectScreen || mc.pendingConnection != null -> SessionSnapshot.Status.CONNECTING
                else -> SessionSnapshot.Status.MENU
            },
            server = mc.currentServer?.ip ?: ServerObserver.serverInfo?.ip,
            activity = player?.let(::activityOf),
            health = player?.health,
            maxHealth = player?.maxHealth,
            food = player?.foodData?.foodLevel,
            position = player?.blockPosition(),
            heldItem = player?.mainHandItem?.takeUnless { it.isEmpty },
            modules = ModuleManager
                .filter { it.perSession && it.category != ModuleCategories.RENDER && it.enabled && !it.hidden }
                .map { it.name },
            lastMessage = lastMessage,
        )
    }

    private fun activityOf(player: LocalPlayer) = when {
        player.isDeadOrDying -> SessionSnapshot.Activity.DEAD
        CombatManager.isInCombat -> SessionSnapshot.Activity.FIGHTING
        mc.gameMode?.isDestroying == true -> SessionSnapshot.Activity.MINING
        player.isUsingItem -> SessionSnapshot.Activity.USING_ITEM
        mc.gui.screen() is AbstractContainerScreen<*> -> SessionSnapshot.Activity.IN_CONTAINER
        player.isSleeping -> SessionSnapshot.Activity.SLEEPING
        player.isPassenger -> SessionSnapshot.Activity.RIDING
        player.isFallFlying -> SessionSnapshot.Activity.GLIDING
        player.isSwimming -> SessionSnapshot.Activity.SWIMMING
        player.deltaMovement.horizontalDistanceSqr() > MOVING_SPEED_SQR -> SessionSnapshot.Activity.MOVING
        else -> SessionSnapshot.Activity.IDLE
    }

}
