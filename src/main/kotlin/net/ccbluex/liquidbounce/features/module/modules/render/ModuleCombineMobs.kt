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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.MobEntity
import net.minecraft.util.math.BlockPos

/**
 * Combine Mobs
 *
 * This module will disable rendering of entities of the same type that are crammed together
 * TODO: and show a single entity instead with a count of how many entities are crammed together.
 *
 * This is useful for example in 2b2t where there are a lot of entities in spawn.
 * The idea behind this module originates from the video
 * "2b2t's WAR Against Chicken Lag" https://www.youtube.com/watch?v=Qqmz76Z5az0
 */
object ModuleCombineMobs : Module("CombineMobs", Category.RENDER) {

    private val trackedEntitySinceRender = mutableMapOf<EntityType<*>, MutableList<BlockPos>>()

    /**
     * As soon we disable the module, we want to clear the tracked entities
     */
    override fun disable() {
        trackedEntitySinceRender.clear()
    }

    /**
     * On each frame, we start with a clean slate
     */
    @Suppress("unused")
    val renderGameHandler = handler<GameRenderEvent> {
        trackedEntitySinceRender.clear()
    }

    fun trackEntity(entity: Entity): Boolean {
        val entityType = entity.type

        if (entity !is MobEntity) {
            return false
        }

        val pos = entity.blockPos

        val trackedEntities = trackedEntitySinceRender.getOrPut(entityType) { mutableListOf() }
        val countOf = trackedEntities.count { it == pos }
        trackedEntities += pos
        trackedEntitySinceRender[entityType] = trackedEntities

        return countOf > 0
    }

}
