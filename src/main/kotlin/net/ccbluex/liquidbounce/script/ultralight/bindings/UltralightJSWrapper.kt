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

package net.ccbluex.liquidbounce.script.ultralight.bindings

import com.labymedia.ultralight.javascript.JavascriptObject
import com.labymedia.ultralight.javascript.JavascriptPropertyAttributes
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.renderer.ultralight.WebPlatform
import net.ccbluex.liquidbounce.renderer.ultralight.WebView
import net.ccbluex.liquidbounce.renderer.ultralight.support.ViewContextProvider

/**
 * Referenced by JS as `client`
 */
class ClientJSWrapper(private val viewContextProvider: ViewContextProvider, val webView: WebView) : Listenable {
    /**
     * Contains all events that are registered in the current context
     */
    private val registeredEvents = HashMap<Class<out Event>, ArrayList<EventHook<*>>>()

    val moduleManager = ModuleManager

    companion object {
        /**
         * Contains mappings from event names to the corresponding classes
         */
        private val EVENT_MAP = HashMap<String, Class<out Event>>()

        init {
            // Register the known events
            for ((name, eventClass) in EventManager.mappedEvents) {
                EVENT_MAP[name] = eventClass.java
            }
        }
    }

    fun on(name: String, handler: JavascriptObject) {
        if (!handler.isFunction)
            throw IllegalArgumentException("$handler is not a function.")

        // Do we know an event that has this name? What is the event class behind this name?
        val eventClass = EVENT_MAP[name] ?: throw IllegalArgumentException("Unknown event: $name")


        // Get the list of the current event type
        val hookList = registeredEvents.computeIfAbsent(eventClass) { ArrayList() }

        // The event function is stored in this property
        val propertyName = "engine__${name}__${hookList.size}"

        // Make a property with the name engine__ plus the name of the event. This is made to
        // make the function accessible for the event handler
        WebPlatform.contextThread.scheduleBlocking {
            viewContextProvider.syncWithJavascript {
                it.context.globalObject.setProperty(propertyName, handler, JavascriptPropertyAttributes.NONE)
            }
        }

        // Create an event hook that will call the JS function
        val eventHook = EventHook<Event>(this, { event ->
            WebPlatform.contextThread.scheduleBlocking {
                viewContextProvider.syncWithJavascript {
                    it.context.globalObject.getProperty(propertyName).toObject().callAsFunction(
                        it.context.globalObject,
                        webView.databind.conversionUtils.toJavascript(it.context, event)
                    )
                }
            }

        }, false)

        // Add the event hook to the list
        hookList.add(eventHook)

        // Register the event hook
        EventManager.registerEventHook(eventClass, eventHook)
    }

    /**
     * Unregisters all events that are registered by this wrapper
     */
    fun unregisterEvents() {
        for ((clazz, hooks) in this.registeredEvents) {
            for (hook in hooks) {
                EventManager.unregisterEventHook(clazz, hook)
            }
        }

        this.registeredEvents.clear()
    }
}
