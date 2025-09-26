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

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import java.util.function.ToIntFunction

/**
 * Allows configuring and manage modes
 */
class ChoiceConfigurable<T : Choice>(
    @Exclude @ProtocolExclude val eventListener: EventListener,
    name: String,
    activeChoiceIndexCallback: ToIntFunction<List<T>>,
    choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
) : Configurable(name, valueType = ValueType.CHOICE) {

    var choices: MutableList<T> = ObjectArrayList(choicesCallback(this))
        internal set
    private var defaultChoice: T = choices[activeChoiceIndexCallback.applyAsInt(choices)]
    var activeChoice: T = defaultChoice
        private set

    init {
        for (choice in choices) {
            choice.base = this
        }
    }

    internal fun updateChildState(state: Boolean) {
        if (state) {
            this.activeChoice.enable()
        } else {
            this.activeChoice.disable()
        }
    }

    private fun setAndUpdate(newChoice: T) {
        if (this.activeChoice === newChoice) {
            return
        }

        if (this.activeChoice.running) {
            this.activeChoice.disable()
        }

        // Don't remove this! This is important. We need to call the listeners of the choice in order to update
        // the other systems accordingly. For whatever reason the conditional configurable is bypassing the value system
        // which the other configurables use, so we do it manually.
        set(mutableListOf(newChoice), apply = {
            this.activeChoice = it.first() as T
        })

        if (this.activeChoice.running) {
            this.activeChoice.enable()
        }
    }

    override fun setByString(name: String) {
        val newChoice = choices.firstOrNull { it.choiceName == name }

        if (newChoice == null) {
            throw IllegalArgumentException("ChoiceConfigurable `${this.name}` has no option named $name" +
                " (available options are ${this.choices.joinToString { it.choiceName }})")
        }

        this.setAndUpdate(newChoice)
    }

    override fun restore() {
        this.setAndUpdate(defaultChoice)
    }

    @ScriptApiRequired
    fun getChoicesStrings(): Array<String> = choices.mapToArray { it.name }

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
abstract class Choice(name: String) : Configurable(name), EventListener, NamedChoice, MinecraftShortcuts {

    final override val choiceName: String
        get() = this.name

    abstract val parent: ChoiceConfigurable<*>

    open fun enable() { }

    open fun disable() { }

    /**
     * Check if the choice is selected on the parent.
     */
    internal val isSelected: Boolean
        get() = this.parent.activeChoice === this

    /**
     * We check if the parent is active and if the mode is active, if so
     * we handle the events.
     */
    override val running: Boolean
        get() = super.running && isSelected

    override fun parent() = this.parent.eventListener

    protected fun <T: Choice> choices(name: String, active: T, choices: Array<T>) =
        choices(this, name, active, choices)

    protected fun <T: Choice> choices(
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(this, name, activeIndex, choicesCallback)
}

/**
 * Empty choice.
 * It does nothing.
 * Use it when you want a client-user to disable a feature.
 */
class NoneChoice(override val parent: ChoiceConfigurable<*>) : Choice("None")
