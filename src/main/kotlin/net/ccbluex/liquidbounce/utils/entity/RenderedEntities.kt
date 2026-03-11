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
package net.ccbluex.liquidbounce.utils.entity

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.Targets
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.world.entity.LivingEntity

private val entities = ReferenceArrayList<LivingEntity>()

/**
 * A readonly [Collection] containing all [LivingEntity] instances that meet the [shouldBeShown] condition.
 *
 * This collection will be auto updated on [GameTickEvent],
 * and be cleared on [WorldChangeEvent] or at the unsubscription of last [EventListener].
 */
object RenderedEntities : Collection<LivingEntity> by entities, EventListener {
    private val registry = ReferenceOpenHashSet<EventListener>()

    private val onUpdate = ReferenceArrayList<Pair<EventListener, Runnable>>()

    context(listener: EventListener)
    fun onUpdated(callback: Runnable) {
        onUpdate += listener to callback
    }

    private fun update() {
        onUpdate.removeIf { (listener, callback) ->
            if (listener !in registry) {
                true
            } else {
                callback.run()
                false
            }
        }
    }

    override val running: Boolean
        get() = registry.isNotEmpty()

    fun subscribe(subscriber: EventListener) {
        registry.add(subscriber)
    }

    fun unsubscribe(subscriber: EventListener) {
        registry.remove(subscriber)
        if (registry.isEmpty()) {
            entities.clear()
            update()
        }
    }

    private fun refresh() {
        entities.clear()

        val shouldCheckCombineMobs = ModuleCombineMobs.running

        for (entity in world.entitiesForRendering()) {
            if (entity is LivingEntity && entity.shouldBeShown()) {
                if (shouldCheckCombineMobs && ModuleCombineMobs.trackEntity(entity, true)) {
                    continue
                }

                entities += entity
            }
        }

        update()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        if (inGame) {
            refresh()
        }
    }

    @Suppress("unused")
    private val perspectiveChangeHandler = handler<PerspectiveEvent> {
        if (GlobalSettingsTarget.visual.contains(Targets.SELF)) {
            refresh()
        }
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        entities.clear()
        update()
    }

}
