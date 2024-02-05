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
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.ScriptApi
import net.ccbluex.liquidbounce.script.bindings.features.JsChoice
import net.ccbluex.liquidbounce.web.socket.protocol.ProtocolExclude
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld

/**
 * Should handle events when enabled. Allows the client-user to toggle features. (like modules)
 */
abstract class ToggleableConfigurable(
    @Exclude @ProtocolExclude val parent: Listenable? = null,
    name: String,
    enabled: Boolean
) : Listenable, Configurable(name, valueType = ValueType.TOGGLEABLE) {

    var enabled by boolean("Enabled", enabled)

    /**
     * Collection of the most used variables
     * to make the code more readable.
     *
     * However, we do not check for nulls here, because
     * we are sure that the client is in-game, if not
     * fiddling with the handler code.
     */
    protected val mc: MinecraftClient
        inline get() = net.ccbluex.liquidbounce.utils.client.mc
    protected val player: ClientPlayerEntity
        inline get() = mc.player!!
    protected val world: ClientWorld
        inline get() = mc.world!!
    protected val network: ClientPlayNetworkHandler
        inline get() = mc.networkHandler!!
    protected val interaction: ClientPlayerInteractionManager
        inline get() = mc.interactionManager!!

    /**
     * Because we pass the parent to the Listenable, we can simply
     * call the super.handleEvents() and it will return false if the upper-listenable is disabled.
     */
    override fun handleEvents() = super.handleEvents() && enabled

    override fun parent() = parent

    @ScriptApi
    @Suppress("unused")
    fun getEnabledValue(): Value<*> = this.value[0]
}

/**
 * Allows to configure and manage modes
 */
class ChoiceConfigurable(
    @Exclude @ProtocolExclude val module: Module,
    name: String,
    activeChoiceCallback: (ChoiceConfigurable) -> Choice,
    choicesCallback: (ChoiceConfigurable) -> Array<Choice>
) : Configurable(name, valueType = ValueType.CHOICE) {

    var choices: MutableList<Choice> = choicesCallback(this).toMutableList()
    var activeChoice: Choice = activeChoiceCallback(this)

    fun newState(state: Boolean) {
        if (state) {
            this.activeChoice.enable()
        } else {
            this.activeChoice.disable()
        }
    }

    fun setFromValueName(name: String) {
        val newChoice = choices.firstOrNull { it.choiceName == name }

        if (newChoice == null) {
            throw IllegalArgumentException("ChoiceConfigurable `${this.name}` has no option named $name" +
                " (available options are ${this.choices.joinToString { it.choiceName }})")
        }

        this.activeChoice = newChoice
    }

    @ScriptApi
    fun getChoicesStrings(): Array<String> = this.choices.map { it.name }.toTypedArray()

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
abstract class Choice(name: String) : Configurable(name), Listenable, NamedChoice {

    override val choiceName: String
        get() = this.name

    /**
     * Collection of the most used variables
     * to make the code more readable.
     *
     * However, we do not check for nulls here, because
     * we are sure that the client is in-game, if not
     * fiddling with the handler code.
     */
    protected val mc: MinecraftClient
        inline get() = net.ccbluex.liquidbounce.utils.client.mc
    protected val player: ClientPlayerEntity
        inline get() = mc.player!!
    protected val world: ClientWorld
        inline get() = mc.world!!
    protected val network: ClientPlayNetworkHandler
        inline get() = mc.networkHandler!!
    protected val interaction: ClientPlayerInteractionManager
        inline get() = mc.interactionManager!!

    val isActive: Boolean
        get() = this.parent.activeChoice === this

    abstract val parent: ChoiceConfigurable

    open fun enable() { }

    open fun disable() { }

    /**
     * We check if the parent is active and if the mode is active, if so
     * we handle the events.
     */
    override fun handleEvents() = super.handleEvents() && isActive

    override fun parent() = this.parent.module

}

/**
 * Empty choice.
 * It does nothing.
 * Use it when you want a client-user to disable a feature.
 */
class NoneChoice(override val parent: ChoiceConfigurable) : Choice("None")
