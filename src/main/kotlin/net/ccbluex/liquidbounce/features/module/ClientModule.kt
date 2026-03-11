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
package net.ccbluex.liquidbounce.features.module

import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig.loadingNow
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
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
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.input.InputBind
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * A module also called 'hack' can be enabled and handle events
 */
@Suppress("LongParameterList", "detekt:TooManyFunctions")
open class ClientModule(
    name: String, // name parameter in configurable
    @Exclude val category: ModuleCategory, // module category
    bind: Int = InputConstants.UNKNOWN.value, // default bind
    bindAction: InputBind.BindAction = InputBind.BindAction.TOGGLE, // default action
    state: Boolean = false, // default state
    @Exclude val notActivatable: Boolean = false, // disable settings that are not needed if the module can't be enabled
    @Exclude val disableActivation: Boolean = notActivatable, // disable activation
    @Exclude val disableOnQuit: Boolean = false, // disables module when player leaves the world,
    aliases: List<String> = emptyList(), // additional names under which the module is known
    hide: Boolean = false // default hide
) : ToggleableValueGroup(null, name, state, aliases = aliases), EventListener, MinecraftShortcuts {

    protected val logger: Logger = LogManager.getLogger("$CLIENT_NAME/$name")

    /**
     * If a module is running or not is separated from the enabled state. A module can be paused even when
     * it is enabled, or it can be running when it is not enabled.
     *
     * Note: This overwrites [ToggleableValueGroup] declaration of [running].
     */
    override val running: Boolean
        get() = super<EventListener>.running && inGame && (enabled || notActivatable)

    internal val bindValue = bind("Bind", InputBind(InputConstants.Type.KEYSYM, bind, bindAction))
        .doNotIncludeWhen { !AutoConfig.includeConfiguration.includeBinds }
        .independentDescription().apply {
            if (notActivatable) {
                notAnOption()
            }
        }
    val bind get() = bindValue.get()

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

    override val baseKey: String = "${ConfigSystem.KEY_PREFIX}.module.${name.toLowerCamelCase()}"

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
            if (category == ModuleCategories.MISC || category == ModuleCategories.FUN ||
                category == ModuleCategories.RENDER) {
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
     * Requires that [ValueGroup.walkKeyPath] has previously been run.
     */
    fun verifyFallbackDescription() {
        if (!LanguageManager.hasFallbackTranslation(descriptionKey!!)) {
            logger.warn("$name is missing fallback description key $descriptionKey")
        }
    }

    fun message(key: String, vararg args: Any) = translation("$baseKey.messages.$key", args = args)

    override fun toString(): String = "Module$name"

}
