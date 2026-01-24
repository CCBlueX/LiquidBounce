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
package net.ccbluex.liquidbounce.features.module.modules.render

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldEntityRemoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import java.time.Instant
import java.util.UUID

/**
 * Log off spot
 *
 * Creates a fake player entity when a player logs off.
 */
object ModuleLogoffSpot : ClientModule("LogoffSpot", ModuleCategories.RENDER) {

    @JvmRecord
    private data class LoggedOffPlayer(
        val time: Instant,
        val entity: Entity,
    )

    private val lastSeenPlayers = Object2ObjectOpenHashMap<UUID, LoggedOffPlayer>()

    @Suppress("unused")
    private val entityRemoveHandler = handler<WorldEntityRemoveEvent> { event ->
        val entity = event.entity
        if (entity !is Player || isLogoffEntity(entity.id)) {
            return@handler
        }

        // Note: I thought we could keep [entity], but I was not able to keep it from being removed
        // from the world. So, we have to create a new entity and copy the position and rotation.
        val clone = RemotePlayer(world, entity.gameProfile)
        clone.yHeadRot = entity.yHeadRot
        clone.copyPosition(entity)
        clone.setUUID(UUID.randomUUID())
        clone.inventory.replaceWith(entity.inventory)
        clone.health = entity.getActualHealth()
        world.addEntity(clone)
        lastSeenPlayers[entity.uuid] = LoggedOffPlayer(Instant.now(), clone)

        val blockPos = entity.blockPosition()
        chat(regular(message("disappeared", entity.scoreboardName, blockPos.x, blockPos.y, blockPos.z)))
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        lastSeenPlayers.entries.removeIf { (id, loggedOffPlayer) ->
            val playerEntity = loggedOffPlayer.entity
            val blockPos = playerEntity.blockPosition()

            if (!world.isLoaded(blockPos)) {
                chat(regular(message("unloaded", playerEntity.scoreboardName)))
                world.removeEntity(playerEntity.id, Entity.RemovalReason.UNLOADED_TO_CHUNK)
                true
            } else if (world.getPlayerByUUID(id) != null) {
                chat(regular(message("reappeared", playerEntity.scoreboardName)))
                world.removeEntity(playerEntity.id, Entity.RemovalReason.UNLOADED_WITH_PLAYER)
                true
            } else {
                false
            }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        lastSeenPlayers.clear()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundInteractPacket) {
            if (isLogoffEntity(packet.entityId)) {
                event.cancelEvent()
            }
        }
    }

    override fun onDisabled() {
        for (loggedOffPlayer in lastSeenPlayers.values) {
            val playerEntity = loggedOffPlayer.entity
            // Use [mc.world] instead of [world] to prevent NPE when the module is disabled
            // outside the game
            mc.level?.removeEntity(playerEntity.id, Entity.RemovalReason.UNLOADED_TO_CHUNK)
        }

        lastSeenPlayers.clear()
        super.onDisabled()
    }

    fun isLogoffEntity(state: LivingEntityRenderState): Boolean {
        return isLogoffEntity((state as EntityRenderStateAddition).`liquid_bounce$getEntity`()?.id ?: return false)
    }

    fun isLogoffEntity(entityId: Int) = this.running
        && lastSeenPlayers.any { (_, logOffPlayer) -> logOffPlayer.entity.id == entityId }

}
