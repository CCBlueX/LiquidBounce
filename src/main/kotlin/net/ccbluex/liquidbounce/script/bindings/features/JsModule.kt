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
package net.ccbluex.liquidbounce.script.bindings.features

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.Script
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.reflect.KClass

class JsModule(val script: Script, moduleObject: Map<String, Any>) : Module(
    name = moduleObject["name"] as String,
    category = Category.fromReadableName(moduleObject["category"] as String)!!
) {

    private val events = hashMapOf<String, (Any?) -> Unit>()
    private val _values = linkedMapOf<String, Value<*>>()
    private var _tag: String? = null
    override val tag: String?
        get() = _tag

    private var _description: String? = null
    override val description: String
        get() = _description ?: ""

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
            _tag = moduleObject["tag"] as String
        }

        if (moduleObject.containsKey("description")) {
            _description = moduleObject["description"] as String
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
            chat(
                Text.literal("[SAPI] ").styled { it.withColor(Formatting.LIGHT_PURPLE) },
                variable(script.scriptName),
                regular("::"),
                variable(name),
                regular("::"),
                variable(event),
                regular(" threw ["),
                Text.literal(throwable.javaClass.simpleName).styled { it.withColor(Formatting.DARK_PURPLE) },
                regular("]: "),
                variable(throwable.message ?: ""),
                prefix = false
            )
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
                },
                false
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
