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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.script.ScriptApiRequired

/**
 * A [ToggleableConfigurable] has a state that can be toggled on and off. It also allows
 * to register event handlers that are only active when the state is on,
 * it also features [enable] and [disable] which are called when the state is toggled.
 */
abstract class ToggleableConfigurable(
    @Exclude @ProtocolExclude val parent: EventListener? = null,
    name: String,
    enabled: Boolean,
    aliases: Array<String> = emptyArray(),
) : EventListener, Configurable(name, valueType = ValueType.TOGGLEABLE, aliases = aliases), MinecraftShortcuts {

    // TODO: Make enabled change also call newState
    internal var enabled by boolean("Enabled", enabled)

    fun newState(state: Boolean) {
        if (!enabled) {
            return
        }

        if (state) {
            enable()
        } else {
            disable()
        }

        inner.filterIsInstance<ChoiceConfigurable<*>>().forEach { it.newState(state) }
        inner.filterIsInstance<ToggleableConfigurable>().forEach { it.newState(state) }
    }

    open fun enable() {}

    open fun disable() {}

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
