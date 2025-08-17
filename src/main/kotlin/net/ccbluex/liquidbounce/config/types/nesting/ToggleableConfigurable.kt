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
import net.ccbluex.liquidbounce.event.SequenceManager.cancelAllSequences
import net.ccbluex.liquidbounce.event.removeEventListenerScope
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.inGame

/**
 * A [ToggleableConfigurable] has a state that can be toggled on and off. It also allows you
 * to register event handlers that are only active when the state is on,
 * it also features [onEnabled] and [onDisabled] which are called when the state is toggled.
 */
abstract class ToggleableConfigurable(
    @Exclude @ProtocolExclude val parent: EventListener? = null,
    name: String,
    enabled: Boolean,
    aliases: Array<out String> = emptyArray(),
) : Configurable(name, valueType = ValueType.TOGGLEABLE, aliases = aliases), EventListener, Toggleable,
    MinecraftShortcuts {

    @ScriptApiRequired
    override var enabled by boolean("Enabled", enabled)
        .also(::onEnabledValueRegistration)
        .onChange(::onToggled)

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
            // Cancel all sequences when the module is disabled, maybe disable first and then cancel?
            cancelAllSequences(this)
            // Remove and cancel coroutine scope
            removeEventListenerScope()
        }

        val state = super.onToggled(state)
        updateChildState(state)
        return state
    }

    /**
     * Because we pass the parent to the Listenable, we can simply
     * call the super.handleEvents() and it will return false if the upper-listenable is disabled.
     */
    override val running: Boolean
        get() = super.running && enabled

    override fun parent() = parent

    @ScriptApiRequired
    @Suppress("unused")
    fun getEnabledValue(): Value<*> = this.inner[0]
}

/**
 * Updates the state of all child [ChoiceConfigurable]s and [Toggleable]s
 */
fun <T> T.updateChildState(state: Boolean)
    where T : Configurable, T : EventListener {
    inner.filterIsInstance<ChoiceConfigurable<*>>().forEach { it.onToggled(state) }
    inner.filterIsInstance<ToggleableConfigurable>().forEach { it.onToggled(state, true) }
}
