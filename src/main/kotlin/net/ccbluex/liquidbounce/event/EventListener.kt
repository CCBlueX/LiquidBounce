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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

typealias Handler<T> = (T) -> Unit

class EventHook<T : Event>(
    val handlerClass: EventListener,
    val handler: Handler<T>,
    val priority: Short = 0
)

interface EventListener {

    /**
     * Returns whether the listenable is running or not, this is based on the parent listenable
     * and if no parent is present, it will return the opposite of [isDestructed].
     *
     * When destructed, the listenable will not handle any events. This is likely to be overridden by
     * the implementing class to provide a toggleable feature.
     *
     * This can be ignored by handlers when [ignoreNotRunning] is set to true on the [EventHook].
     */
    val running: Boolean
        get() = parent()?.running ?: !isDestructed

    /**
     * Parent [EventListener]
     */
    fun parent(): EventListener? = null

    /**
     * Children [EventListener]
     */
    fun children(): List<EventListener> = emptyList()

    /**
     * Unregisters the event handler from the manager. This decision is FINAL!
     * After the class was unregistered we cannot restore the handlers.
     */
    fun unregister() {
        EventManager.unregisterEventHandler(this)
        removeEventListenerScope()

        for (child in children()) {
            child.unregister()
        }
    }

}

inline fun <reified T : Event> EventListener.handler(
    priority: Short = 0,
    noinline handler: Handler<T>
): EventHook<T> {
    return EventManager.registerEventHook(T::class.java,
        EventHook(this, handler, priority)
    )
}

inline fun <reified T : Event> EventListener.until(
    priority: Short = 0,
    crossinline handler: (T) -> Boolean
): EventHook<T> {
    lateinit var eventHook: EventHook<T>
    eventHook = EventHook(this, {
        if (!this.running || handler(it)) {
            EventManager.unregisterEventHook(T::class.java, eventHook)
        }
    }, priority)
    return EventManager.registerEventHook(T::class.java, eventHook)
}

inline fun <reified T : Event> EventListener.once(
    priority: Short = 0,
    crossinline handler: Handler<T>
): EventHook<T> = until(priority) { event ->
    handler(event)
    true // This will unregister the handler after the first call
}

/**
 * Registers an event hook for events of type [T] and launches a sequence
 */
inline fun <reified T : Event> EventListener.sequenceHandler(
    priority: Short = 0,
    crossinline eventHandler: SuspendableEventHandler<T>
) {
    handler<T>(priority) { event -> Sequence(this) { eventHandler(event) } }
}

/**
 * Registers a repeatable sequence which repeats the execution of code on GameTickEvent.
 */
fun EventListener.tickHandler(eventHandler: SuspendableHandler) {
    // We store our sequence in this variable.
    // That can be done because our variable will survive the scope of this function
    // and can be used in the event handler function. This is a very useful pattern to use in Kotlin.
    var sequence: TickSequence? = TickSequence(this, eventHandler)

    SequenceManager.handler<GameTickEvent> {
        // Check if we should start or stop the sequence
        if (this.running) {
            // Check if the sequence is already running
            if (sequence == null) {
                // If not, start it
                // This will start a new repeating sequence which will run until the condition is false
                sequence = TickSequence(this, eventHandler)
            }
        } else if (sequence != null) { // This condition is only true if the sequence is running
            // If the sequence is running, we should stop it
            sequence?.cancel()
            sequence = null
        }
    }
}

/**
 * Returns computed [ReadWriteProperty] based on the [accumulator] of specific event.
 *
 * The value of property will be updated on event received with [accumulator].
 *
 * Example:
 * ```kotlin
 * var ticksSinceEnabled by computedOn<GameTickEvent, Int>(0) { _, prev -> prev + 1 }
 *
 * fun enabled() { ticksSinceEnabled = 0 }
 * ```
 *
 * @author MukjepScarlet
 * @since 0.30.1
 */
inline fun <reified E : Event, V> EventListener.computedOn(
    initialValue: V,
    priority: Short = 0,
    crossinline accumulator: (event: E, prev: V) -> V,
): ReadWriteProperty<EventListener, V> = object : ReadWriteProperty<EventListener, V> {
    @Volatile // Make this value visible to all threads
    private var value = initialValue

    @Suppress("unused") // May be useful?
    private val eventHook = handler<E>(priority) { event ->
        value = accumulator(event, value)
    }

    override fun getValue(thisRef: EventListener, property: KProperty<*>): V = value
    override fun setValue(thisRef: EventListener, property: KProperty<*>, value: V) {
        this.value = value
    }
    override fun toString(): String = "ComputedProperty<${E::class.java.simpleName}>($value)"
}
