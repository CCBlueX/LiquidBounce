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


package net.ccbluex.liquidbounce.features.chat.party

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.packet.C2SPartyStatePacket
import net.ccbluex.liquidbounce.features.chat.packet.PartyPosition
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.armorItems
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.inventory.EnderChestInventoryTracker
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.level.GameType

private const val POSITION_INTERVAL = 250L
private const val STATUS_INTERVAL = 250L
private const val STATUS_REFRESH = 10_000L
private const val INVENTORY_INTERVAL = 1000L
private const val INVENTORY_REFRESH = 30_000L

// The server drops party states above 16 KiB
private const val INVENTORY_BUDGET = 12 * 1024

private val POSITION_RELATIONS = setOf("nearby", "world", "instance")

object PartyStateReporter : EventListener {

    private var membership = emptySet<String>()

    private var positionSentAt = 0L
    private var statusCheckedAt = 0L
    private var statusSentAt = 0L
    private var lastStatus: String? = null
    private var inventoryCheckedAt = 0L
    private var inventorySentAt = 0L
    private var lastInventory: String? = null

    private val client
        get() = GlobalSettingsClientChat.chatClient

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player
        val others = PartyManager.others
        if (player == null || others.isEmpty() || !client.isLoggedIn || !client.isModern) {
            membership = emptySet()
            return@handler
        }

        // members who joined have not seen anything yet
        val members = others.mapTo(hashSetOf()) { it.user.id }
        if (members != membership) {
            membership = members
            lastStatus = null
            lastInventory = null
        }

        val now = System.currentTimeMillis()
        val sharesWorld = others.any { it.relation in POSITION_RELATIONS }
        val position = if (sharesWorld && now - positionSentAt >= POSITION_INTERVAL) {
            positionSentAt = now
            positionOf(player)
        } else {
            null
        }

        val packet = C2SPartyStatePacket(position, nextStatus(player, now), nextInventory(player, now))
        if (packet.position != null || packet.status != null || packet.inventory != null) {
            client.sendPacket(packet)
        }
    }

    private fun positionOf(player: LocalPlayer) = PartyPosition(
        player.x, player.y, player.z, player.yRot, player.xRot,
        player.level().dimension().identifier().toString(),
    )

    private fun nextStatus(player: LocalPlayer, now: Long): JsonObject? {
        if (now - statusCheckedAt < STATUS_INTERVAL) {
            return null
        }
        statusCheckedAt = now

        val status = statusOf(player)
        val serialized = status.toString()
        if (serialized == lastStatus && now - statusSentAt < STATUS_REFRESH) {
            return null
        }

        lastStatus = serialized
        statusSentAt = now
        return status
    }

    private fun nextInventory(player: LocalPlayer, now: Long): JsonObject? {
        if (now - inventoryCheckedAt < INVENTORY_INTERVAL) {
            return null
        }
        inventoryCheckedAt = now

        val inventory = inventoryOf(player, withEnderChest = true, withNames = true).takeIf { it.fits() }
            ?: inventoryOf(player, withEnderChest = false, withNames = true).takeIf { it.fits() }
            ?: inventoryOf(player, withEnderChest = false, withNames = false)
        val serialized = inventory.toString()
        if (serialized == lastInventory && now - inventorySentAt < INVENTORY_REFRESH) {
            return null
        }

        lastInventory = serialized
        inventorySentAt = now
        return inventory
    }

    private fun JsonObject.fits() = toString().length <= INVENTORY_BUDGET

    private fun statusOf(player: LocalPlayer) = JsonObject().apply {
        addProperty("health", player.getActualHealth())
        addProperty("max_health", player.maxHealth)
        addProperty("absorption", player.absorptionAmount)
        addProperty("food", player.foodData.foodLevel)
        addProperty("saturation", player.foodData.saturationLevel)
        addProperty("armor", player.armorValue)
        addProperty("xp_level", player.experienceLevel)
        addProperty("game_mode", (player.gameMode() ?: GameType.DEFAULT_MODE).serializedName)
        addProperty("ping", player.ping)
        addProperty("dead", player.isDeadOrDying)
        add("effects", JsonArray().apply {
            for (effect in player.activeEffects) {
                add(JsonObject().apply {
                    addProperty("id", effect.effect.registeredName)
                    addProperty("amplifier", effect.amplifier)
                    addProperty("duration", effect.duration)
                })
            }
        })
        add("main_hand", PartyItems.encode(player.mainHandItem))
        add("off_hand", PartyItems.encode(player.offhandItem))
        add("armor_items", PartyItems.encode(player.armorItems.asList()))
    }

    private fun inventoryOf(player: LocalPlayer, withEnderChest: Boolean, withNames: Boolean) = JsonObject().apply {
        add("main", PartyItems.encode(player.inventory.nonEquipmentItems, withNames))
        add("armor", PartyItems.encode(player.armorItems.asList(), withNames))
        add("offhand", PartyItems.encode(player.offhandItem, withNames))
        if (withEnderChest && EnderChestInventoryTracker.stacks.isNotEmpty()) {
            add("ender_chest", PartyItems.encode(EnderChestInventoryTracker.stacks, withNames))
        }
    }

    override fun parent() = GlobalSettingsClientChat

}
