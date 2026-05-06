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
package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.level.GameType
import java.util.UUID

/**
 * Notifier module
 *
 * Notifies you about all kinds of events.
 */
object ModuleNotifier : ClientModule("Notifier", ModuleCategories.MISC) {

    init {
        doNotIncludeAlways()
    }

    private val joinMessages by boolean("JoinMessages", true)
    private val joinMessageFormat by text("JoinMessageFormat", "%s joined")

    private val leaveMessages by boolean("LeaveMessages", true)
    private val leaveMessageFormat by text("LeaveMessageFormat", "%s left")

    private val gameModeMessages by boolean("GameModeMessages", false)
    private val gameModeMessageFormat by text("GameModeMessageFormat", "%s changed their game mode to %s")

    private val itemConsumptionMessages by boolean("ItemConsumptionMessages", true)
    private val itemConsumptionMessageFormat by text("ItemConsumptionMessageFormat", $$"%1$s used %2$s")

    private val useNotification by boolean("UseNotification", false)

    private val uuidNameCache = Object2ObjectOpenHashMap<UUID, String>()
    private val uuidGameModeCache = Object2ObjectOpenHashMap<UUID, GameType>()
    private val itemConsumptionCache = Object2ObjectOpenHashMap<UUID, ItemConsumptionState>()
    private val observedItemConsumers = ObjectOpenHashSet<UUID>()

    override fun onEnabled() {
        for (entry in network.onlinePlayers) {
            uuidNameCache[entry.profile.id] = entry.profile.name
            uuidGameModeCache[entry.profile.id] = entry.gameMode
        }
    }

    override fun onDisabled() {
        uuidNameCache.clear()
        uuidGameModeCache.clear()
        itemConsumptionCache.clear()
        observedItemConsumers.clear()
    }

    val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundPlayerInfoUpdatePacket -> mc.execute {
                val actions = packet.actions()
                val entries = packet.entries()

                if (ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER in actions) {
                    for (entry in entries) {
                        handlePlayerAdd(entry)
                    }
                }

                if (ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE in actions) {
                    val isInitializing = ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER in actions

                    for (entry in entries) {
                        handleGameModeUpdate(entry, isInitializing)
                    }
                }
            }

            is ClientboundPlayerInfoRemovePacket -> mc.execute {
                for (uuid in packet.profileIds) {
                    val profileName = uuidNameCache.remove(uuid)
                    uuidGameModeCache.remove(uuid)

                    if (profileName != null && profileName.length > 2) {
                        if (leaveMessages) {
                            sendNotifierMessage(leaveMessageFormat.format(profileName))
                        }
                    }
                }
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        if (!itemConsumptionMessages) {
            itemConsumptionCache.clear()
            observedItemConsumers.clear()
            return@handler
        }

        observedItemConsumers.clear()
        for (player in world.players()) {
            if (player !is RemotePlayer || ModuleAntiBot.isBot(player)) {
                continue
            }

            observedItemConsumers.add(player.uuid)
            handleItemConsumption(player)
        }

        itemConsumptionCache.keys.retainAll(observedItemConsumers)
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        itemConsumptionCache.clear()
        observedItemConsumers.clear()
    }

    private fun handlePlayerAdd(entry: ClientboundPlayerInfoUpdatePacket.Entry) {
        val profile = entry.profile ?: return
        val profileName = profile.name

        if (profileName == null || profileName.length <= 2) {
            return
        }

        uuidNameCache[profile.id] = profileName

        if (joinMessages) {
            sendNotifierMessage(joinMessageFormat.format(profileName))
        }
    }

    private fun handleGameModeUpdate(entry: ClientboundPlayerInfoUpdatePacket.Entry, isInitializing: Boolean) {
        val previousGameMode = uuidGameModeCache.put(entry.profileId, entry.gameMode)

        if (isInitializing || previousGameMode == null || previousGameMode == entry.gameMode || !gameModeMessages) {
            return
        }

        val profileName = uuidNameCache[entry.profileId] ?: return
        sendNotifierMessage(gameModeMessageFormat.format(profileName, entry.gameMode))
    }

    private fun handleItemConsumption(player: RemotePlayer) {
        if (!player.isUsingItem) {
            val state = itemConsumptionCache.remove(player.uuid)

            if (state != null && state.isComplete) {
                sendNotifierMessage(
                    itemConsumptionMessageFormat.format(
                        player.gameProfile.name,
                        state.itemStack.hoverName.string
                    )
                )
            }

            return
        }

        val state = itemConsumptionCache[player.uuid]
        val useItem = player.useItem
        if (!useItem.isTrackedConsumable()) {
            itemConsumptionCache.remove(player.uuid)
            return
        }

        if (state == null || !ItemStack.isSameItemSameComponents(state.itemStack, useItem)) {
            itemConsumptionCache[player.uuid] = ItemConsumptionState(
                useItem.copy(),
                useItem.getUseDuration(player),
                player.ticksUsingItem
            )
            return
        }

        state.lastTicksUsingItem = maxOf(state.lastTicksUsingItem, player.ticksUsingItem)
    }

    private fun ItemStack.isTrackedConsumable(): Boolean {
        if (!isConsumable) {
            return false
        }

        val animation = useAnimation
        return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK
    }

    private fun sendNotifierMessage(message: String) {
        if (useNotification) {
            notification(this.name, message, NotificationEvent.Severity.INFO)
        } else {
            chat(regular(message))
        }
    }

    private data class ItemConsumptionState(
        val itemStack: ItemStack,
        val useDuration: Int,
        var lastTicksUsingItem: Int,
    ) {
        val isComplete: Boolean
            get() = useDuration > 0 && lastTicksUsingItem >= useDuration - 1
    }

}
