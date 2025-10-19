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
 * and show a single entity instead with a count of how many entities are crammed together.
 *
 * This is useful for example in 2b2t where there are a lot of entities in spawn.
 * The idea behind this module originates from the video
 * "2b2t's WAR Against Chicken Lag" https://www.youtube.com/watch?v=Qqmz76Z5az0
 */
object ModuleCombineMobs : ClientModule("CombineMobs", Category.RENDER) {

    @JvmRecord
    private data class CombineKey(val type: EntityType<*>, val babyGroup: Boolean)

    private val renderTracked = HashMap<CombineKey, Long2IntOpenHashMap>()
    private val nametagTracked = HashMap<CombineKey, Long2IntOpenHashMap>()

    override fun onDisabled() {
        renderTracked.clear()
        nametagTracked.clear()
    }

    /**
     * On each frame, we start with a clean slate
     */
    @Suppress("unused")
    val renderGameHandler = handler<GameRenderEvent> {
        renderTracked.clear()
        nametagTracked.clear()
    }

    private fun keyFor(mob: MobEntity): CombineKey {
        val babyGroup = mob.isBaby
        return CombineKey(mob.type, babyGroup)
    }

    @JvmOverloads
    fun trackEntity(entity: Entity, forNametag: Boolean = false): Boolean {
        val mob = entity as? MobEntity ?: return false
        val target = if (forNametag) nametagTracked else renderTracked

        val key = keyFor(mob)
        val pos = mob.blockPos.asLong()

        val posMap = target.getOrPut(key, ::Long2IntOpenHashMap)
        val count = posMap.addTo(pos, 1)

        return count > 0
    }

    fun getCombinedCount(entity: Entity): Int {
        val mob = entity as? MobEntity ?: return 1

        val key = keyFor(mob)
        val pos = mob.blockPos.asLong()

        val count = renderTracked[key]?.getOrDefault(pos, 0) ?: 0
        if (count > 0) return count

        return nametagTracked[key]?.getOrDefault(pos, 1) ?: 1
    }
}
