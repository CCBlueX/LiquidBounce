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
package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.entity.LivingEntity

private val entities = ArrayList<LivingEntity>()

/**
 * A readonly [Collection] containing all [LivingEntity] instances that meet the [shouldBeShown] condition.
 *
 * This collection will be auto updated on [GameTickEvent],
 * and be cleared on [WorldChangeEvent] or at the unsubscription of last [EventListener].
 */
object RenderedEntities : Collection<LivingEntity> by entities, EventListener {
    private val registry = ReferenceOpenHashSet<EventListener>()

    override val running: Boolean
        get() = registry.isNotEmpty()

    fun subscribe(subscriber: EventListener) {
        registry.add(subscriber)
    }

    fun unsubscribe(subscriber: EventListener) {
        registry.remove(subscriber)
        if (registry.isEmpty()) {
            entities.clear()
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        if (!inGame) {
            return@handler
        }

        entities.clear()
        for (entity in world.entities) {
            if (entity is LivingEntity && entity.shouldBeShown()) {
                entities += entity
            }
        }
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        entities.clear()
    }
}
