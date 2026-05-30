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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.Items
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

    private val heldItemMessages by boolean("HeldItemMessages", false)
    private val heldItemMessageFormat by text("HeldItemMessageFormat", $$"%1$s holds %2$s x%3$s in %4$s")
    private val heldItems by items("HeldItems", itemSortedSetOf(Items.END_CRYSTAL, Items.ENCHANTED_GOLDEN_APPLE))

    private val useNotification by boolean("UseNotification", false)

    private val uuidNameCache = Object2ObjectOpenHashMap<UUID, String>()
    private val uuidGameModeCache = Object2ObjectOpenHashMap<UUID, GameType>()
    private val itemConsumptionCache = Object2ObjectOpenHashMap<UUID, ItemConsumptionState>()
    private val heldItemCache = Object2ObjectOpenHashMap<UUID, HeldItemState>()
    private val observedPlayers = ObjectOpenHashSet<UUID>()
    private val popCounter = Object2IntOpenHashMap<UUID>()

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
        heldItemCache.clear()
        observedPlayers.clear()
        popCounter.clear()
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

                    if (profileName != null && profileName.length >= 2) {
                        if (leaveMessages) {
                            sendNotifierMessage(leaveMessageFormat.format(profileName))
                        }
                    }
                }
            }

            is ClientboundEntityEventPacket -> mc.execute {
                if (packet.eventId.toInt() == 35) {
                    val entity = packet.getEntity(world) as? Player ?: return@execute
                    if (entity === mc.player || FriendManager.isFriend(entity.name.string)) return@execute

                    popCounter.addTo(entity.uuid, 1)

                    sendNotifierMessage("${entity.name.string} pop totem ${popCounter.getInt(entity.uuid)} times")
                }
            }

            is ClientboundDisconnectPacket, is ClientboundLoginPacket -> mc.execute(popCounter::clear)
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        observedPlayers.clear()
        for (player in world.players()) {
            if (player !is RemotePlayer || ModuleAntiBot.isBot(player)) {
                continue
            }

            observedPlayers.add(player.uuid)

            if (itemConsumptionMessages) {
                handleItemConsumption(player)
            }

            if (heldItemMessages) {
                handleHeldItems(player)
            }
        }

        if (itemConsumptionMessages) {
            itemConsumptionCache.keys.retainAll(observedPlayers)
        } else {
            itemConsumptionCache.clear()
        }

        if (heldItemMessages) {
            heldItemCache.keys.retainAll(observedPlayers)
        } else {
            heldItemCache.clear()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        itemConsumptionCache.clear()
        heldItemCache.clear()
        observedPlayers.clear()
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

    private fun handleHeldItems(player: RemotePlayer) {
        val currentMainHand = player.mainHandItem.takeIf { it.isTrackedHeldItem() }
        val currentOffHand = player.offhandItem.takeIf { it.isTrackedHeldItem() }

        val currentState = HeldItemState(currentMainHand, currentOffHand)
        val previousState = heldItemCache[player.uuid]

        if (currentState.isEmpty) {
            heldItemCache.remove(player.uuid)
            return
        }

        if (previousState != null) {
            if (currentMainHand != null && !currentMainHand.isSameHeldItem(previousState.mainHand)) {
                sendHeldItemMessage(player, currentMainHand, InteractionHand.MAIN_HAND)
            }
            if (currentOffHand != null && !currentOffHand.isSameHeldItem(previousState.offHand)) {
                sendHeldItemMessage(player, currentOffHand, InteractionHand.OFF_HAND)
            }
        } else {
            if (currentMainHand != null) {
                sendHeldItemMessage(player, currentMainHand, InteractionHand.MAIN_HAND)
            }
            if (currentOffHand != null) {
                sendHeldItemMessage(player, currentOffHand, InteractionHand.OFF_HAND)
            }
        }

        heldItemCache[player.uuid] = currentState
    }

    private fun ItemStack.isTrackedConsumable(): Boolean {
        if (!isConsumable) {
            return false
        }

        val animation = useAnimation
        return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK
    }

    private fun ItemStack.isTrackedHeldItem(): Boolean {
        return !isEmpty && item in heldItems
    }

    private fun ItemStack.isSameHeldItem(other: ItemStack?): Boolean {
        return other != null && ItemStack.isSameItemSameComponents(this, other) && count == other.count
    }

    private fun sendHeldItemMessage(player: RemotePlayer, itemStack: ItemStack, hand: InteractionHand) {
        sendNotifierMessage(
            heldItemMessageFormat.format(
                player.gameProfile.name,
                itemStack.hoverName.string,
                itemStack.count,
                hand
            )
        )
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

    private data class HeldItemState(
        val mainHand: ItemStack?,
        val offHand: ItemStack?,
    ) {
        val isEmpty: Boolean
            get() = mainHand == null && offHand == null
    }

}
