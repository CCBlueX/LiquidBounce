/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldEntityRemoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import java.util.*

/**
 * Log off spot
 *
 * Creates a fake player entity when a player logs off.
 */
object ModuleLogoffSpot : ClientModule("LogoffSpot", Category.RENDER) {

    private data class LoggedOffPlayer(
        val time: Instant,
        val entity: Entity
    )

    private val lastSeenPlayers = mutableMapOf<UUID, LoggedOffPlayer>()

    @Suppress("unused")
    private val entityRemoveHandler = handler<WorldEntityRemoveEvent> { event ->
        val entity = event.entity
        if (entity !is PlayerEntity || isLogoffEntity(entity)) {
            return@handler
        }

        // Note: I thought we could keep [entity], but I was not able to keep it from being removed
        // from the world. So, we have to create a new entity and copy the position and rotation.
        val clone = OtherClientPlayerEntity(world, entity.gameProfile)
        clone.headYaw = entity.headYaw
        clone.copyPositionAndRotation(entity)
        clone.uuid = UUID.randomUUID()
        clone.inventory.clone(entity.inventory)
        clone.health = entity.getActualHealth()
        world.addEntity(clone)
        lastSeenPlayers[entity.uuid] = LoggedOffPlayer(Clock.System.now(), clone)

        val blockPos = entity.pos.toBlockPos()
        chat(regular(message("disappeared", entity.nameForScoreboard, blockPos.x, blockPos.y, blockPos.z)))
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        lastSeenPlayers.entries.removeIf { (id, loggedOffPlayer) ->
            val playerEntity = loggedOffPlayer.entity
            val blockPos = playerEntity.pos.toBlockPos()

            if (!world.isPosLoaded(blockPos)) {
                chat(regular(message("unloaded", playerEntity.nameForScoreboard)))
                world.removeEntity(playerEntity.id, Entity.RemovalReason.UNLOADED_TO_CHUNK)
                true
            } else if (world.getPlayerByUuid(id) != null) {
                chat(regular(message("reappeared", playerEntity.nameForScoreboard)))
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
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val entity = event.entity

        if (isLogoffEntity(entity)) {
            event.cancelEvent()
        }
    }

    override fun onDisabled() {
        for (loggedOffPlayer in lastSeenPlayers.values) {
            val playerEntity = loggedOffPlayer.entity
            // Use [mc.world] instead of [world] to prevent NPE when the module is disabled
            // outside the game
            mc.world?.removeEntity(playerEntity.id, Entity.RemovalReason.UNLOADED_TO_CHUNK)
        }

        lastSeenPlayers.clear()
        super.onDisabled()
    }

    fun isLogoffEntity(state: LivingEntityRenderState) =
        isLogoffEntity((state as EntityRenderStateAddition).`liquid_bounce$getEntity`())

    fun isLogoffEntity(entity: Entity) = this.running
        && lastSeenPlayers.any { (_, logOffPlayer) -> entity == logOffPlayer.entity }

}
