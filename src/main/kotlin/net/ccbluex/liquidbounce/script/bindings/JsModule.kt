/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
package net.ccbluex.liquidbounce.script.bindings

import jdk.nashorn.api.scripting.JSObject
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.logger

@Suppress("unused")
class JsModule(private val moduleObject: JSObject) : Module(
    name = moduleObject.getMember("name") as String,
    category = Category.fromReadableName(moduleObject.getMember("category") as String)!!
) {

    private val events = HashMap<String, JSObject>()

    private var _tag: String? = null
    override val tag: String?
        get() = _tag

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    val settings by lazy { value }

    init {
        if (moduleObject.hasMember("settings")) {
            val settings = moduleObject.getMember("settings") as JSObject

            for (settingName in settings.keySet())
                value.add(settings.getMember(settingName) as Value<*>)
        }

        if (moduleObject.hasMember("tag")) {
            _tag = moduleObject.getMember("tag") as String
        }
    }

    /**
     * Called from inside the script to register a new event handler.
     * @param eventName Name of the event.
     * @param handler JavaScript function used to handle the event.
     */
    fun on(eventName: String, handler: JSObject) {
        events[eventName] = handler
        hookHandler(eventName)
    }

    override fun enable() = callEvent("enable")

    override fun disable() = callEvent("disable")

    /**
     * Calls the handler of a registered event.
     *
     * @param eventName Name of the event to be called.
     * @param payload Event data passed to the handler function.
     */
    private fun callEvent(eventName: String, payload: Any? = null) {
        try {
            events[eventName]?.call(moduleObject, payload)
        } catch (throwable: Throwable) {
            logger.error("Script caused exception in module $name on $eventName event!", throwable)
        }
    }

    /**
     * Register new event hook
     */
    private fun hookHandler(eventName: String) {
        val (_, clazz) = EventManager.mappedEvents.find { (name, _) -> name.equals(eventName, true) } ?: return

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

}
