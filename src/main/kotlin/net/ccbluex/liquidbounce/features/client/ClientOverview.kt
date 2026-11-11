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

import net.ccbluex.liquidbounce.api.core.ApiConfig
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.ClientsUpdateEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
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
 * Describes what the clients are doing, which is all the user sees of the ones in the background.
 */
object ClientOverview : EventListener {

    private var announcedAt = 0L
    private var announcedSeveral = false

    /**
     * The last line a server wrote into the chat of the client.
     */
    private var lastMessage: Component? by clientState { null }

    /**
     * Has to be asked on the game thread.
     */
    fun snapshots(): List<ClientSnapshot> = ClientManager.clients.map(::snapshot)

    fun snapshot(client: Client): ClientSnapshot = ClientContext.within(client) { describe(client) }

    /**
     * Tells the theme what the clients are doing, and once more when only one is left.
     */
    internal fun announce() {
        val several = ClientManager.clients.size > 1
        if (!several && !announcedSeveral) {
            return
        }

        val now = System.nanoTime()
        if (several && now - announcedAt < ANNOUNCE_INTERVAL_NANOS) {
            return
        }

        announcedAt = now
        announcedSeveral = several
        EventManager.callEvent(ClientsUpdateEvent(snapshots()))
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
     * Reads the game of the loaded client, which has to be [client].
     */
    private fun describe(client: Client): ClientSnapshot {
        val player = mc.player?.takeIf { mc.level != null }
        val screen = mc.gui.screen()
        val user = client.user

        return ClientSnapshot(
            id = client.id,
            username = user.name,
            avatar = ApiConfig.formatAvatarUrl(user.profileId, user.name),
            active = client.isActive,
            status = when {
                player != null -> ClientSnapshot.Status.IN_GAME
                screen is DisconnectedScreen -> ClientSnapshot.Status.DISCONNECTED
                screen is ConnectScreen || mc.pendingConnection != null -> ClientSnapshot.Status.CONNECTING
                else -> ClientSnapshot.Status.MENU
            },
            server = mc.currentServer?.ip ?: ServerObserver.serverInfo?.ip,
            activity = player?.let(::activityOf),
            health = player?.health,
            maxHealth = player?.maxHealth,
            food = player?.foodData?.foodLevel,
            position = player?.blockPosition(),
            heldItem = player?.mainHandItem?.takeUnless { it.isEmpty },
            modules = ModuleManager
                .filter { it.perClient && it.category != ModuleCategories.RENDER && it.enabled && !it.hidden }
                .map { it.name },
            lastMessage = lastMessage,
        )
    }

    private fun activityOf(player: LocalPlayer) = when {
        player.isDeadOrDying -> ClientSnapshot.Activity.DEAD
        CombatManager.isInCombat -> ClientSnapshot.Activity.FIGHTING
        mc.gameMode?.isDestroying == true -> ClientSnapshot.Activity.MINING
        player.isUsingItem -> ClientSnapshot.Activity.USING_ITEM
        mc.gui.screen() is AbstractContainerScreen<*> -> ClientSnapshot.Activity.IN_CONTAINER
        player.isSleeping -> ClientSnapshot.Activity.SLEEPING
        player.isPassenger -> ClientSnapshot.Activity.RIDING
        player.isFallFlying -> ClientSnapshot.Activity.GLIDING
        player.isSwimming -> ClientSnapshot.Activity.SWIMMING
        player.deltaMovement.horizontalDistanceSqr() > MOVING_SPEED_SQR -> ClientSnapshot.Activity.MOVING
        else -> ClientSnapshot.Activity.IDLE
    }

}
