/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.event

import java.util.*

/**
 * A modern and fast event handler using lambda handlers
 */
class EventManager {

    private val registry = HashMap<Class<out Event>, MutableList<EventHook<in Event>>>()

    /**
     * Registers an event hook for events of type [T]
     */
    inline fun <reified T: Event> handler(listener: Listenable, ignoreCondition: Boolean = false, noinline eventHandler: (T) -> Unit) {
        registerEventHook(T::class.java, EventHook(listener, eventHandler, ignoreCondition))
    }

    /**
     * Used by [handler]
     */
    fun <T: Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        registry.computeIfAbsent(eventClass) { mutableListOf() }.add(eventHook as EventHook<in Event>)
    }

    /**
     * Unregister listener
     *
     * @param listenable for unregister
     */
    fun unregisterListener(listenable: Listenable) {
        for ((key, handlerList) in registry) {
            handlerList.removeIf { it.handlerClass == listenable }

            registry[key] = handlerList
        }
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun callEvent(event: Event) {
        val target = registry[event.javaClass] ?: return

        for (eventHook in target) {
            if (!eventHook.ignoresCondition && !eventHook.handlerClass.handleEvents())
                continue
            
            eventHook.handler(event)
        }
    }

}