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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.MobEntity

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
object ModuleCombineMobs : ClientModule("CombineMobs", Category.RENDER) {

    /**
     * Key: type Value: position->count
     */
    private val trackedEntitySinceRender = hashMapOf<EntityType<*>, Long2IntOpenHashMap>()

    /**
     * As soon we disable the module, we want to clear the tracked entities
     */
    override fun onDisabled() {
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
        if (entity !is MobEntity) {
            return false
        }

        val entityType = entity.type

        val pos = entity.blockPos.asLong()

        val trackedEntities = trackedEntitySinceRender.getOrPut(entityType, ::Long2IntOpenHashMap)
        val count = trackedEntities.getOrDefault(pos, 0)
        trackedEntities.put(pos, count + 1)

        return count > 0
    }

}
