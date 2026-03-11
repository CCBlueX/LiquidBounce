/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.config.types.group

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import java.util.function.ToIntFunction

/**
 * Allows configuring and manage modes
 */
class ModeValueGroup<T : Mode>(
    @Exclude @ProtocolExclude val eventListener: EventListener,
    name: String,
    activeModeIndexCallback: ToIntFunction<List<T>>,
    modesCallback: (ModeValueGroup<T>) -> Array<T>
) : ValueGroup(name, valueType = ValueType.CHOICE) {

    var modes: MutableList<T> = ObjectArrayList.wrap(modesCallback(this))
        internal set
    private var defaultMode: T = modes[activeModeIndexCallback.applyAsInt(modes)]
    var activeMode: T = defaultMode
        private set

    init {
        for (choice in modes) {
            choice.base = this
        }
    }

    internal fun updateChildState(state: Boolean) {
        if (state) {
            this.activeMode.enable()
        } else {
            this.activeMode.disable()
        }
    }

    private fun setAndUpdate(newMode: T) {
        if (this.activeMode === newMode) {
            return
        }

        if (this.activeMode.running) {
            this.activeMode.disable()
        }

        // Don't remove this! This is important. We need to call the listeners of the mode to update
        // the other systems accordingly. For whatever reason the conditional configurable is bypassing the value system
        // which the other configurables use, so we do it manually.
        set(mutableListOf(newMode), apply = {
            this.activeMode = it.first() as T
        })

        if (this.activeMode.running) {
            this.activeMode.enable()
        }
    }

    override fun setByString(name: String) {
        val newMode = modes.firstOrNull { choice ->
            choice.tag == name || name in choice.aliases
        }

        if (newMode == null) {
            throw IllegalArgumentException("ChoiceConfigurable `${this.name}` has no option named $name" +
                " (available options are ${this.modes.joinToString { it.tag }})")
        }

        this.setAndUpdate(newMode)
    }

    override fun restore() {
        this.setAndUpdate(defaultMode)
    }

    @ScriptApiRequired
    fun getModeStrings(): Array<String> = modes.mapToArray { it.name }

}

abstract class Mode(
    name: String,
    aliases: List<String> = emptyList()
) : ValueGroup(name, aliases = aliases), EventListener, Tagged, MinecraftShortcuts {

    final override val tag: String
        get() = this.name

    final override val tagAliases: List<String>
        get() = this.aliases

    abstract val parent: ModeValueGroup<*>

    open fun enable() { }

    open fun disable() { }

    /**
     * Check if the choice is selected on the parent.
     */
    internal val isSelected: Boolean
        get() = this.parent.activeMode === this

    /**
     * We check if the parent is active and if the mode is active, if so
     * we handle the events.
     */
    override val running: Boolean
        get() = super.running && isSelected

    override fun parent() = this.parent.eventListener

    protected fun <T: Mode> modes(name: String, active: T, choices: Array<T>) =
        modes(this, name, active, choices)

    protected fun <T: Mode> modes(
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ModeValueGroup<T>) -> Array<T>
    ) = modes(this, name, activeIndex, choicesCallback)
}

/**
 * Empty mode without any functionality. Use as a disable mode.
 */
class NoneMode(override val parent: ModeValueGroup<*>) : Mode("None")
