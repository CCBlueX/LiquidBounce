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

package net.ccbluex.liquidbounce.config

import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.QuickImports
import net.ccbluex.liquidbounce.script.ScriptApi
import net.ccbluex.liquidbounce.integration.interop.protocol.ProtocolExclude

/**
 * Should handle events when enabled. Allows the client-user to toggle features. (like modules)
 */
abstract class ToggleableConfigurable(
    @Exclude @ProtocolExclude val parent: Listenable? = null,
    name: String,
    enabled: Boolean
) : Listenable, Configurable(name, valueType = ValueType.TOGGLEABLE), QuickImports {

    // TODO: Make enabled change also call newState
    var enabled by boolean("Enabled", enabled)

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
    override fun handleEvents() = super.handleEvents() && enabled

    override fun parent() = parent

    @ScriptApi
    @Suppress("unused")
    fun getEnabledValue(): Value<*> = this.inner[0]
}

/**
 * Allows to configure and manage modes
 */
class ChoiceConfigurable<T : Choice>(
    @Exclude @ProtocolExclude val listenable: Listenable,
    name: String,
    activeChoiceCallback: (ChoiceConfigurable<T>) -> T,
    choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
) : Configurable(name, valueType = ValueType.CHOICE) {

    var choices: MutableList<T> = choicesCallback(this).toMutableList()
    private var defaultChoice: T = activeChoiceCallback(this)
    var activeChoice: T = defaultChoice

    fun newState(state: Boolean) {
        if (state) {
            this.activeChoice.enable()
        } else {
            this.activeChoice.disable()
        }

        inner.filterIsInstance<ChoiceConfigurable<*>>().forEach { it.newState(state) }
        inner.filterIsInstance<ToggleableConfigurable>().forEach { it.newState(state) }
    }

    override fun setByString(name: String) {
        val newChoice = choices.firstOrNull { it.choiceName == name }

        if (newChoice == null) {
            throw IllegalArgumentException("ChoiceConfigurable `${this.name}` has no option named $name" +
                " (available options are ${this.choices.joinToString { it.choiceName }})")
        }

        if (this.activeChoice.handleEvents()) {
            this.activeChoice.disable()
        }

        // Don't remove this! This is important. We need to call the listeners of the choice in order to update
        // the other systems accordingly. For whatever reason the conditional configurable is bypassing the value system
        // which the other configurables use, so we do it manually.
        set(mutableListOf(newChoice), apply = {
            this.activeChoice = it[0] as T
        })

        if (this.activeChoice.handleEvents()) {
            this.activeChoice.enable()
        }
    }

    override fun restore() {
        if (this.activeChoice.handleEvents()) {
            this.activeChoice.disable()
        }

        set(mutableListOf(defaultChoice), apply = {
            this.activeChoice = it[0] as T
        })

        if (this.activeChoice.handleEvents()) {
            this.activeChoice.enable()
        }
    }

    @ScriptApi
    fun getChoicesStrings(): Array<String> = this.choices.map { it.name }.toTypedArray()

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
abstract class Choice(name: String) : Configurable(name), Listenable, NamedChoice, QuickImports {

    override val choiceName: String
        get() = this.name

    val isActive: Boolean
        get() = this.parent.activeChoice === this

    abstract val parent: ChoiceConfigurable<*>

    open fun enable() {}

    open fun disable() {}

    /**
     * We check if the parent is active and if the mode is active, if so
     * we handle the events.
     */
    override fun handleEvents() = super.handleEvents() && isActive

    override fun parent() = this.parent.listenable

    protected fun <T: Choice> choices(name: String, active: T, choices: Array<T>) =
        choices(this, name, active, choices)

    protected fun <T: Choice> choices(
        name: String,
        activeCallback: (ChoiceConfigurable<T>) -> T,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(this, name, activeCallback, choicesCallback)
}

/**
 * Empty choice.
 * It does nothing.
 * Use it when you want a client-user to disable a feature.
 */
class NoneChoice(override val parent: ChoiceConfigurable<*>) : Choice("None")
