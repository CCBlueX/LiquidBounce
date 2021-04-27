/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.event

import net.minecraft.entity.Entity
import net.minecraft.network.Packet

typealias Handler<T> = (T) -> Unit

class EventHook<T : Event>(val handlerClass: Listenable, val handler: Handler<T>, val ignoresCondition: Boolean, val priority: Int = 0)

interface Listenable {

    /**
     * Allows to disable event handling when condition is false.
     */
    fun handleEvents(): Boolean = parent()?.handleEvents() ?: true

    /**
     * Parent listenable
     */
    fun parent(): Listenable? = null

}

inline fun <reified T : Event> Listenable.handler(ignoreCondition: Boolean = false, priority: Int = 0, noinline handler: Handler<T>) {
    EventManager.registerEventHook(T::class.java, EventHook(this, handler, ignoreCondition, priority))
}

/**
 * Wrapped AttackEvent to handle it as generic event
 * @param source the source event
 * @param enemy the enemy
 * @param <E> the enemy type
 */
class WrappedAttackEvent<out E : Entity>(val source: AttackEvent, val enemy: E)

inline fun <reified T> Listenable.attackHandler(
    ignoreCondition: Boolean = false,
    priority: Int = 0,
    noinline handler: Handler<WrappedAttackEvent<T>>,
) where T : Entity {
    handler<AttackEvent>(ignoreCondition, priority) {
        if (it.enemy is T) {
            val wrappedPacketEvent = WrappedAttackEvent(it, it.enemy)
            handler(wrappedPacketEvent)
        }
    }
}

/**
 * Wrapped PacketEvent to handle it as generic event
 * @param source the source event
 * @param packet the packet
 * @param <P> the packet type
 */
class WrappedPacketEvent<out P : Packet<*>>(val source: PacketEvent, val packet: P)

inline fun <reified T> Listenable.packetHandler(
    ignoreCondition: Boolean = false,
    priority: Int = 0,
    noinline handler: Handler<WrappedPacketEvent<T>>,
)
    where T : Packet<*> {
    handler<PacketEvent>(ignoreCondition, priority) {
        if (it.packet is T) {
            val wrappedPacketEvent = WrappedPacketEvent(it, it.packet)
            handler(wrappedPacketEvent)
        }
    }
}

/**
 * Registers an event hook for events of type [T] and launches a sequence
 */
inline fun <reified T : Event> Listenable.sequenceHandler(ignoreCondition: Boolean = false, noinline eventHandler: SuspendableHandler<T>) {
    handler<T>(ignoreCondition) { event -> Sequence(eventHandler, event) }
}

/**
 * Registers a repeatable sequence which repeats the execution of code.
 */
fun Listenable.repeatable(eventHandler: (SuspendableHandler<DummyEvent>)) {
    var sequence: RepeatingSequence? = null

    handler<ToggleModuleEvent>(ignoreCondition = true) {
        if (this == it.module || this.parent() == it.module) {
            if (this.handleEvents()) {
                if (sequence == null) {
                    sequence = RepeatingSequence(eventHandler)
                }
            } else if (sequence != null) {
                sequence?.cancel()
                sequence = null
            }
        }
    }
}
