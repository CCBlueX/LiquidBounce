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
package net.ccbluex.liquidbounce.script.bindings.features

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.script.PolyglotScript
import net.ccbluex.liquidbounce.utils.client.*
import java.util.function.Supplier
import kotlin.reflect.KClass

class ScriptModule(val script: PolyglotScript, moduleObject: Map<String, Any>) : ClientModule(
    name = moduleObject["name"] as String,
    category = Category.fromReadableName(moduleObject["category"] as String)!!
) {

    private val events = hashMapOf<String, org.graalvm.polyglot.Value>()
    private val _values = linkedMapOf<String, Value<*>>()
    override var tag: String? = null
        set(value) {
            field = value
            EventManager.callEvent(RefreshArrayListEvent)
        }

    private var _description: String? = null
    override var description: Supplier<String?> = Supplier { _description ?: "" }

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    override val settings by lazy { _values }

    init {
        if (moduleObject.containsKey("settings")) {
            val settingsObject = moduleObject["settings"] as Map<String, Value<*>>

            for ((name, value) in settingsObject) {
                _values[name] = value(value)
            }
        }

        if (moduleObject.containsKey("tag")) {
            tag = moduleObject["tag"] as String
        }

        if (moduleObject.containsKey("description")) {
            _description = moduleObject["description"] as String
        }
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     *   1. `() => void` (enable/disable)
     *   2. `(Event) => void` (handler<T>)
     *   3. `async (Event) => void` (sequenceHandler<T>)
     */
    fun on(eventName: String, handler: org.graalvm.polyglot.Value) {
        if (!handler.canExecute()) {
            logger.error("Invalid event handler for $eventName")
            return
        }

        events[eventName] = handler
        hookHandler(eventName)
    }

    override fun onEnabled() = callEvent("enable")

    override fun onDisabled() = callEvent("disable")

    /**
     * Calls the function of the [event] with the [payload] of the event.
     *
     * @param payload when event is "enable" or "disable", it will be null
     */
    private fun callEvent(event: String, payload: Event? = null) {
        try {
            events[event]?.executeVoid(payload)
        } catch (throwable: Throwable) {
            if (inGame) {
                chat(
                    regular("["),
                    warning(script.file.name),
                    regular("] "),
                    markAsError(script.scriptName),
                    regular("::"),
                    markAsError(name),
                    regular("::"),
                    markAsError(event),
                    regular(" threw ["),
                    highlight(throwable.javaClass.simpleName),
                    regular("]: "),
                    variable(throwable.message ?: ""),
                    metadata = MessageMetadata(prefix = false)
                )

            }

            logger.error("${script.scriptName}::$name -> Event Function $event threw an error", throwable)

            // Disable the module if an error occurs
            enabled = false
        }
    }

    /**
     * Register new event hook
     */
    private fun hookHandler(eventName: String) {
        // Get event case-insensitive
        val clazz = LOWERCASE_NAME_EVENT_MAP[eventName.lowercase()] ?: return

        EventManager.registerEventHook(
            clazz.java,
            EventHook(
                this,
                {
                    callEvent(eventName, it)
                }
            )
        )
    }

    companion object {
        /**
         * Maps the lowercase name of the event to the event's kotlin class
         */
        private val LOWERCASE_NAME_EVENT_MAP: Map<String, KClass<out Event>> =
            ALL_EVENT_CLASSES.associateBy { it.eventName.lowercase() }
    }
}
