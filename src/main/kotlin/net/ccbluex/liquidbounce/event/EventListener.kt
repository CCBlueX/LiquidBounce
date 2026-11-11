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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.session.ClientSession
import net.ccbluex.liquidbounce.features.session.SessionContext
import net.ccbluex.liquidbounce.features.session.sessionState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.features.misc.SelfDestruct.isDestructed
import java.util.function.Consumer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class EventHook<T : Event>(
    val handlerClass: EventListener,
    val priority: Short,
    /**
     * Set on hooks that wait for something to happen in one session. Events of other sessions pass them by.
     */
    val session: ClientSession?,
    val handler: Consumer<T>,
) {

    @JvmOverloads
    constructor(handlerClass: EventListener, priority: Short = 0, handler: Consumer<T>) :
        this(handlerClass, priority, null, handler)

}

@AddonApi
interface EventListener : DebuggedOwner {

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
     * Calls [handler] for every [type] event while this listener is running. Higher [priority] runs
     * first. Close the result to stop early; [unregister] does the same for all of them.
     */
    @AddonApi
    fun <E : Event> on(type: Class<E>, priority: Short, handler: Consumer<E>): AutoCloseable {
        val hook = EventManager.registerEventHook(type, EventHook(this, priority, handler))
        return AutoCloseable { EventManager.unregisterEventHook(type, hook) }
    }

    @AddonApi
    fun <E : Event> on(type: Class<E>, handler: Consumer<E>): AutoCloseable = on(type, 0, handler)

    /**
     * [task] every game tick while this listener is running.
     */
    @AddonApi
    fun onTick(task: Runnable): AutoCloseable = on(GameTickEvent::class.java) { task.run() }

    /**
     * Runs [task] once, [ticks] game ticks from now, unless this listener stops running first.
     */
    @AddonApi
    fun after(ticks: Int, task: Runnable): AutoCloseable {
        val job = eventListenerScope.launch {
            waitTicks(ticks)
            task.run()
        }
        return AutoCloseable { job.cancel() }
    }

    /**
     * Runs [task] every [ticks] game ticks while this listener is running, the first time after [ticks].
     */
    @AddonApi
    fun every(ticks: Int, task: Runnable): AutoCloseable {
        val job = eventListenerScope.launch {
            while (isActive) {
                waitTicks(ticks)
                task.run()
            }
        }
        return AutoCloseable { job.cancel() }
    }

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

/**
 * A listener that serves the window: the theme, the browser, the HUD. It only hears the events of the
 * active session, where a plain [EventListener] hears those of every session.
 */
interface ActiveSessionListener : EventListener {

    override val running: Boolean
        get() = super.running && SessionContext.current.isActive

}

inline fun <E : Event> EventListener.newEventHook(
    priority: Short = 0,
    handler: Consumer<E>,
): EventHook<E> = EventHook(this, priority, handler)

fun <T : Event> EventListener.handler(
    eventClass: Class<T>,
    priority: Short = 0,
    handler: Consumer<T>,
): EventHook<T> = EventManager.registerEventHook(eventClass, newEventHook(priority, handler))

inline fun <reified T : Event> EventListener.handler(
    priority: Short = 0,
    handler: Consumer<T>,
): EventHook<T> = handler(T::class.java, priority, handler)

inline fun <reified T : Event> EventListener.until(
    priority: Short = 0,
    crossinline handler: (T) -> Boolean
): EventHook<T> {
    lateinit var eventHook: EventHook<T>
    eventHook = EventHook(this, priority, SessionContext.current) {
        if (!this.running || handler(it)) {
            EventManager.unregisterEventHook(T::class.java, eventHook)
        }
    }
    return EventManager.registerEventHook(T::class.java, eventHook)
}

inline fun <reified T : Event> EventListener.once(
    priority: Short = 0,
    crossinline handler: (T) -> Unit
): EventHook<T> = until(priority) { event -> // Don't use `repeated` 'cause for no overhead
    handler(event)
    true // This will unregister the handler after the first call
}

inline fun <reified T : Event> EventListener.repeated(
    times: Int = 1,
    priority: Short = 1,
    crossinline handler: (T) -> Unit
): EventHook<T> {
    require(times > 0) { "times must be > 0" }

    var called = 0
    return until<T>(priority) { event ->
        handler(event)
        ++called >= times
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
    private val state = sessionState { initialValue }

    @Suppress("unused") // May be useful?
    private val eventHook = handler<E>(priority) { event ->
        state.set(accumulator(event, state.get()))
    }

    override fun getValue(thisRef: EventListener, property: KProperty<*>): V = state.get()
    override fun setValue(thisRef: EventListener, property: KProperty<*>, value: V) = state.set(value)
    override fun toString(): String = "ComputedProperty<${E::class.java.simpleName}>(${state.get()})"
}
