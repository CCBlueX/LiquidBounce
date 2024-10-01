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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed

typealias Handler<T> = (T) -> Unit

class EventHook<T : Event>(
    val handlerClass: Listenable,
    val handler: Handler<T>,
    val ignoresCondition: Boolean,
    val priority: Int = 0
)

interface Listenable {

    /**
     * Allows disabling event handling when condition is false.
     */
    fun handleEvents(): Boolean = parent()?.handleEvents() ?: !isDestructed

    /**
     * Parent listenable
     */
    fun parent(): Listenable? = null

    /**
     * Children listenables
     */
    fun children(): List<Listenable> = emptyList()

    /**
     * Unregisters the event handler from the manager. This decision is FINAL!
     * After the class was unregistered we cannot restore the handlers.
     */
    fun unregister() {
        EventManager.unregisterEventHandler(this)

        for (child in children()) {
            child.unregister()
        }
    }

}

inline fun <reified T : Event> Listenable.handler(
    ignoreCondition: Boolean = false,
    priority: Int = 0,
    noinline handler: Handler<T>
) {
    EventManager.registerEventHook(T::class.java, EventHook(this, handler, ignoreCondition, priority))
}

/**
 * Registers an event hook for events of type [T] and launches a sequence
 */
inline fun <reified T : Event> Listenable.sequenceHandler(
    ignoreCondition: Boolean = false,
    priority: Int = 0,
    noinline eventHandler: SuspendableHandler<T>
) {
    handler<T>(ignoreCondition, priority) { event -> Sequence(this, eventHandler, event) }
}

/**
 * Registers a repeatable sequence which repeats the execution of code on GameTickEvent.
 */
fun Listenable.repeatable(eventHandler: SuspendableHandler<DummyEvent>) {
    // We store our sequence in this variable.
    // That can be done because our variable will survive the scope of this function
    // and can be used in the event handler function. This is a very useful pattern to use in Kotlin.
    var sequence: RepeatingSequence? = RepeatingSequence(this, eventHandler)

    // Ignore condition makes sense because we do not want our sequence to run after we do not handle events anymore
    handler<GameTickEvent>(ignoreCondition = true) {
        // Check if we should start or stop the sequence
        if (this.handleEvents()) {
            // Check if the sequence is already running
            if (sequence == null) {
                // If not, start it
                // This will start a new repeating sequence which will run until the condition is false
                sequence = RepeatingSequence(this, eventHandler)
            }
        } else if (sequence != null) { // This condition is only true if the sequence is running
            // If the sequence is running, we should stop it
            sequence?.cancel()
            sequence = null
        }
    }
}
