/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.script.RequiredByScript
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.text.TranslatableText

/**
 * Should handle events when enabled. Allows the client-user to toggle features. (like modules)
 */
open class ToggleableConfigurable(@Exclude val module: Module? = null, name: String, enabled: Boolean) : Listenable,
    Configurable(name, valueType = ValueType.TOGGLEABLE) {

    val translationBaseKey: String
        get() = "${module?.translationBaseKey}.value.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    var enabled by boolean("Enabled", enabled)

    override fun handleEvents() = super.handleEvents() && enabled

    override fun parent() = module

    fun getEnabledValue(): Value<*> {
        return this.value[0]
    }
}

/**
 * Allows to configure and manage modes
 */
open class ChoiceConfigurable(
    @Exclude val module: Module,
    name: String,
    var activeChoice: Choice,
    choicesCallback: (ChoiceConfigurable) -> Array<Choice>
) : Configurable(name, valueType = ValueType.CHOICE) {

    val choices: Array<Choice>
    val translationBaseKey: String
        get() = "${module.translationBaseKey}.value.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    init {
        this.choices = choicesCallback(this)
    }

    fun newState(state: Boolean) {
        if (state) {
            this.activeChoice.enable()
        } else {
            this.activeChoice.disable()
        }
    }

    fun setFromValueName(name: String) {
        this.activeChoice = choices.first { it.choiceName == name }
    }

    @RequiredByScript
    fun getChoicesStrings(): Array<String> {
        return this.choices.map { it.name }.toTypedArray()
    }

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
abstract class Choice(name: String) : Configurable(name), Listenable, NamedChoice {

    private val translationBaseKey: String
        get() = "${this.parent.translationBaseKey}.choice.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    override val choiceName: String
        get() = this.name

    /**
     * Quick access
     */
    protected val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.client.mc
    protected val player: ClientPlayerEntity
        get() = mc.player!!
    protected val world: ClientWorld
        get() = mc.world!!
    protected val network: ClientPlayNetworkHandler
        get() = mc.networkHandler!!

    val isActive: Boolean
        get() = this.parent.activeChoice === this

    abstract val parent: ChoiceConfigurable

    /**
     * Called when module is turned on
     */
    open fun enable() { }

    /**
     * Called when module is turned off
     */
    open fun disable() { }

    /**
     * Events should be handled when mode is enabled
     */
    override fun handleEvents() = super.handleEvents() && isActive

    /**
     * Parent listenable
     */
    override fun parent() = this.parent.module

}

/**
 * Empty mode. It does nothing. Use it when you want a client-user to disable a feature.
 */
class NoneChoice(override val parent: ChoiceConfigurable) : Choice("None")
