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

package net.ccbluex.liquidbounce.config.types.nesting

import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.removeEventListenerScope
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * A [ToggleableConfigurable] has a state that can be toggled on and off. It also allows you
 * to register event handlers that are only active when the state is on,
 * it also features [onEnabled] and [onDisabled] which are called when the state is toggled.
 */
abstract class ToggleableConfigurable(
    @Exclude @ProtocolExclude val parent: EventListener? = null,
    name: String,
    enabled: Boolean,
    aliases: List<String> = emptyList(),
) : Configurable(name, valueType = ValueType.TOGGLEABLE, aliases = aliases), EventListener, Toggleable,
    MinecraftShortcuts {

    @ScriptApiRequired
    @get:JvmName("getEnabledValue")
    val enabledValue: Value<Boolean> = boolean("Enabled", enabled)
        .also(::onEnabledValueRegistration)
        .onChange { state -> onToggled(state) }

    @ScriptApiRequired
    override var enabled by enabledValue

    open fun onEnabledValueRegistration(value: Value<Boolean>): Value<Boolean> {
        return value
    }

    override fun onToggled(state: Boolean): Boolean {
        if (!inGame) {
            return state
        }

        return onToggled(state, false)
    }

    fun onToggled(state: Boolean, isParentUpdate: Boolean): Boolean {
        // We cannot use [parent.running] because we are interested in the state of the parent,
        // not if it is running. We do not care if we are the root.
        if (!isParentUpdate && parent is Toggleable && !parent.enabled) {
            return state
        }

        if (!state) {
            runCatching {
                // Remove and cancel coroutine scope
                removeEventListenerScope()
            }.onFailure {
                logger.error("Failed to cancel sequences or remove scope for $this", it)
            }
        }

        val state = super.onToggled(state)
        this@ToggleableConfigurable.updateChildState(state)
        return state
    }

    /**
     * Because we pass the parent to the Listenable, we can simply
     * call the super.handleEvents() and it will return false if the upper-listenable is disabled.
     */
    override val running: Boolean
        get() = super.running && enabled

    final override fun parent() = parent

    protected fun <T : Choice> choices(name: String, active: T, choices: Array<T>) =
        choices(this, name, active, choices)

    protected fun <T : Choice> choices(
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(this, name, activeIndex, choicesCallback)

}

/**
 * Updates the state of all child [Configurable]s.
 *
 * All implementations of [Toggleable] with super class [Configurable]
 * should call this function in [Toggleable.onToggled].
 */
private fun Configurable.updateChildState(state: Boolean) {
    for (value in inner) {
        when (value) {
            is ToggleableConfigurable -> if (state && value.enabled) {
                value.onToggled(state = true, isParentUpdate = true)
            } else if (!state && value.enabled) {
                value.onToggled(state = false, isParentUpdate = true)
            }
            is ChoiceConfigurable<*> -> value.updateChildState(state)
            is Configurable -> value.updateChildState(state)
            is Toggleable -> if (state && value.enabled) {
                value.onToggled(true)
            } else if (!state && value.enabled) {
                value.onToggled(false)
            }
        }
    }
}
