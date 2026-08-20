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

package net.ccbluex.liquidbounce.utils.world

import it.unimi.dsi.fastutil.objects.ReferenceCollection
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.entity.LevelEntityGetter
import kotlin.reflect.KProperty

class EntityLookup<T : Entity> private constructor(
    owner: EventListener,
    private val updateCycle: Int,
    private val entities: ReferenceCollection<T>,
    private val collector: Collector<T>,
) : MinecraftShortcuts {

    private var ticks = 0

    fun clear() {
        entities.clear()

        ticks = 0
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ReferenceCollection<T> = entities

    private val onUpdate = owner.handler<GameTickEvent> {
        ticks++
        ticks %= updateCycle

        if (ticks != 0) return@handler

        entities.clear()
        with(collector) {
            mc.level?.entityGetter?.collect(entities)
        }
    }

    fun interface Collector<T : Entity> {
        fun LevelEntityGetter<Entity>.collect(destination: ReferenceCollection<T>)
    }

    companion object {
        @JvmStatic
        @JvmName("create")
        fun <T : Entity> EventListener.EntityLookup(collector: Collector<T>): EntityLookup<T> {
            return EntityLookup(owner = this, updateCycle = 1, ReferenceOpenHashSet(), collector)
        }
    }

}
