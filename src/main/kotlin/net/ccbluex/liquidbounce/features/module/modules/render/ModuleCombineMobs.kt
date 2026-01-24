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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart

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
object ModuleCombineMobs : ClientModule("CombineMobs", ModuleCategories.RENDER) {

    @JvmRecord
    private data class CombineKey(val type: EntityType<*>, val babyGroup: Boolean)

    private val renderTracker = Object2ObjectOpenHashMap<CombineKey, Long2IntOpenHashMap>()
    private val nametagTracker = Object2ObjectOpenHashMap<CombineKey, Long2IntOpenHashMap>()

    private val combineArmorStands by boolean("CombineArmorStands", false)
    private val combineMinecarts by boolean("CombineMinecarts", false)

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        RenderedEntities.onUpdated(nametagTracker::clear)
        super.onEnabled()
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        renderTracker.clear()
        nametagTracker.clear()
    }

    @Suppress("unused")
    private val renderGameHandler = handler<GameRenderEvent> {
        renderTracker.clear()
    }

    private fun keyFor(mob: Entity): CombineKey {
        if (mob !is LivingEntity) {
            return CombineKey(mob.type, false)
        }
        val babyGroup = mob.isBaby
        return CombineKey(mob.type, babyGroup)
    }

    @JvmOverloads
    fun trackEntity(entity: Entity, forNametag: Boolean = false): Boolean {
        val canCombine = entity is Mob ||
            (entity is ArmorStand && combineArmorStands) ||
            (entity is AbstractMinecart && combineMinecarts)
        if (!canCombine) return false

        return (if (forNametag) nametagTracker else renderTracker)
            .getOrPut(keyFor(entity), ::Long2IntOpenHashMap)
            .addTo(entity.blockPosition().asLong(), 1) > 0
    }

    fun getCombinedCount(entity: Entity): Int {
        if (!running) return 1

        val key = keyFor(entity)
        val pos = entity.blockPosition().asLong()

        val count = renderTracker[key]?.getOrDefault(pos, 0) ?: 0
        if (count > 0) return count

        return nametagTracker[key]?.getOrDefault(pos, 1) ?: 1
    }
}
