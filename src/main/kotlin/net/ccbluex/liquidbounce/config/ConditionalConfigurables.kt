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

    var enabled by boolean(name, enabled)

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
    var active: String,
    val initialize: (ChoiceConfigurable) -> Unit
) : Configurable(name, valueType = ValueType.CHOICE) {

    val translationBaseKey: String
        get() = "${module.translationBaseKey}.value.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    fun newState(state: Boolean) {
        val choice = choices.find { active.equals(it.name, true) } ?: return

        if (state) {
            choice.enable()
        } else {
            choice.disable()
        }
    }

    @Exclude
    val choices: MutableList<Choice> = mutableListOf()

    fun getChoicesStrings(): Array<String> {
        return this.choices.map { it.name }.toTypedArray()
    }

    // TODO Cancel sequence hanndlers on update, etc.
    fun setFromValueName(name: String) {
        this.active = name
    }
}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
open class Choice(name: String, @Exclude private val configurable: ChoiceConfigurable) : Configurable(name), Listenable {

    private val translationBaseKey: String
        get() = "${configurable.translationBaseKey}.choice.${name.toLowerCamelCase()}"

    val description: TranslatableText
        get() = TranslatableText("$translationBaseKey.description")

    init {
        configurable.choices += this
    }

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
        get() = configurable.active.equals(name, true)

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
    override fun parent() = configurable.module

}

/**
 * Empty mode. It does nothing. Use it when you want a client-user to disable a feature.
 */
class NoneChoice(configurable: ChoiceConfigurable) : Choice("None", configurable)
