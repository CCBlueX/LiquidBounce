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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.reflect.KClass

class ScriptChoice(choiceObject: Map<String, Any>, override val parent: ChoiceConfigurable<Choice>) : Choice(
    name = choiceObject["name"] as String,
) {

    private val events = hashMapOf<String, (Any?) -> Unit>()
    private val _values = linkedMapOf<String, Value<*>>()

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    val settings by lazy { _values }

    init {
        if (choiceObject.containsKey("settings")) {
            val settingsObject = choiceObject["settings"] as Map<String, Value<*>>

            for ((name, value) in settingsObject) {
                _values[name] = value(value)
            }
        }
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: (Any?) -> Unit) {
        events[eventName] = handler
        hookHandler(eventName)
    }

    override fun enable() = callEvent("enable")

    override fun disable() = callEvent("disable")

    /**
     * Calls the function of the [event]  with the [payload] of the event.
     */
    private fun callEvent(event: String, payload: Event? = null) {
        try {
            events[event]?.invoke(payload)
        } catch (throwable: Throwable) {
            logger.error("Script caused exception in module $name on $event event!", throwable)
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
