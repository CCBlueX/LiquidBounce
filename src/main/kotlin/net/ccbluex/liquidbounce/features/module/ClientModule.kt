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
package net.ccbluex.liquidbounce.features.module

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.loadingNow
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.ModuleActivationEvent
import net.ccbluex.liquidbounce.event.events.ModuleToggleEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.util.InputUtil

/**
 * A module also called 'hack' can be enabled and handle events
 */
@Suppress("LongParameterList", "detekt:TooManyFunctions")
open class ClientModule(
    name: String, // name parameter in configurable
    @Exclude val category: Category, // module category
    bind: Int = InputUtil.UNKNOWN_KEY.code, // default bind
    bindAction: InputBind.BindAction = InputBind.BindAction.TOGGLE, // default action
    state: Boolean = false, // default state
    @Exclude val notActivatable: Boolean = false, // disable settings that are not needed if the module can't be enabled
    @Exclude val disableActivation: Boolean = notActivatable, // disable activation
    hide: Boolean = false, // default hide
    @Exclude val disableOnQuit: Boolean = false, // disables module when player leaves the world,
    aliases: List<String> = emptyList() // additional names under which the module is known
) : ToggleableConfigurable(null, name, state, aliases = aliases), EventListener, MinecraftShortcuts {

    /**
     * If a module is running or not is seperated from the enabled state. A module can be paused even when
     * it is enabled, or it can be running when it is not enabled.
     *
     * Note: This overwrites [ToggleableConfigurable] declaration of [running].
     */
    override val running: Boolean
        get() = super<EventListener>.running && inGame && (enabled || notActivatable)

    val bind by bind("Bind", InputBind(InputUtil.Type.KEYSYM, bind, bindAction))
        .doNotIncludeWhen { !AutoConfig.includeConfiguration.includeBinds }
        .independentDescription().apply {
            if (notActivatable) {
                notAnOption()
            }
        }
    var hidden by boolean("Hidden", hide)
        .doNotIncludeWhen { !AutoConfig.includeConfiguration.includeHidden }
        .independentDescription()
        .onChange {
            EventManager.callEvent(RefreshArrayListEvent)
            it
        }.apply {
            if (notActivatable) {
                notAnOption()
            }
        }

    /**
     * If this value is on true, we cannot enable the module, as it likely does not bypass.
     */
    private var locked: Value<Boolean>? = null

    override val baseKey: String = "liquidbounce.module.${name.toLowerCamelCase()}"

    // Tag to be displayed on the HUD
    open val tag: String?
        get() = this.tagValue?.getTagValue()?.toString()

    private var tagValue: Value<*>? = null

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    @ScriptApiRequired
    open val settings by lazy { inner.associateBy { it.name } }

    /**
     * For delayed enabling.
     * On client startup, the [onToggled] of enabled modules (in configuration) will be called when the player first
     * joins a world.
     */
    internal var calledSinceStartup = false

    /**
     * Called when the module is registered in the module manager.
     */
    open fun onRegistration() {}

    final override fun onEnabledValueRegistration(value: Value<Boolean>) =
        super.onEnabledValueRegistration(value).also { value ->
            // Might not include the enabled state of the module depending on the category
            if (category == Category.MISC || category == Category.FUN || category == Category.RENDER) {
                if (this is ModuleAntiBot) {
                    return@also
                }
                value.doNotIncludeAlways()
            }
        }.notAnOption().onChanged { newState ->
            if (newState) {
                eventListenerScope.launch { enabledEffect() }
            }
        }

    /**
     * Launches an async task on [eventListenerScope] when module is turned on.
     */
    open suspend fun enabledEffect() {}

    final override fun onToggled(state: Boolean): Boolean {
        // Check if the module is locked and cannot be enabled
        locked?.let { locked ->
            if (locked.get()) {
                notification(
                    this.name,
                    translation("liquidbounce.generic.locked"),
                    NotificationEvent.Severity.ERROR
                )

                return false
            }
        }

        if (!inGame) {
            return state
        }
        calledSinceStartup = true

        val state = super.onToggled(state)

        EventManager.callEvent(ModuleActivationEvent(name))

        // If the module is not activatable, we do not want to change state
        if (disableActivation) {
            return false
        }

        if (!loadingNow) {
            val (title, severity) = if (state) {
                translation("liquidbounce.generic.enabled") to NotificationEvent.Severity.ENABLED
            } else {
                translation("liquidbounce.generic.disabled") to NotificationEvent.Severity.DISABLED
            }
            notification(title, this.name, severity)
        }

        EventManager.callEvent(ModuleToggleEvent(name, hidden, state))
        return state
    }

    /**
     * If we want a module to have the requires bypass option, we specifically call it
     * on init. This will add the option and enable the feature.
     */
    fun enableLock() {
        this.locked = boolean("Locked", false)
    }

    fun tagBy(setting: Value<*>) {
        check(this.tagValue == null) { "Tag already set" }

        this.tagValue = setting

        // Refresh arraylist on tag change
        setting.onChanged {
            EventManager.callEvent(RefreshArrayListEvent)
        }
    }

    /**
     * Warns when no module description is set in the main translation file.
     *
     * Requires that [Configurable.walkKeyPath] has previously been run.
     */
    fun verifyFallbackDescription() {
        if (!LanguageManager.hasFallbackTranslation(descriptionKey!!)) {
            logger.warn("$name is missing fallback description key $descriptionKey")
        }
    }

    fun message(key: String, vararg args: Any) = translation("$baseKey.messages.$key", args = args)

    override fun toString(): String = "Module$name"

}
