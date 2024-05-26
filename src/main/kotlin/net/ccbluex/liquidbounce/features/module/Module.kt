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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.config.AutoConfig.loadingNow
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.ScriptApi
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.effect.StatusEffect
import org.lwjgl.glfw.GLFW

interface QuickImports {
    /**
     * Collection of the most used variables
     * to make the code more readable.
     *
     * However, we do not check for nulls here, because
     * we are sure that the client is in-game, if not
     * fiddling with the handler code.
     */
    val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.client.mc
    val player: ClientPlayerEntity
        get() = mc.player!!
    val world: ClientWorld
        get() = mc.world!!
    val network: ClientPlayNetworkHandler
        get() = mc.networkHandler!!
    val interaction: ClientPlayerInteractionManager
        get() = mc.interactionManager!!
}

/**
 * A module also called 'hack' can be enabled and handle events
 */
@Suppress("LongParameterList")
open class Module(
    name: String, // name parameter in configurable
    @Exclude val category: Category, // module category
    bind: Int = GLFW.GLFW_KEY_UNKNOWN, // default bind
    state: Boolean = false, // default state
    @Exclude val disableActivation: Boolean = false, // disable activation
    hide: Boolean = false, // default hide
    @Exclude val disableOnQuit: Boolean = false, // disables module when player leaves the world,
    @Exclude val aliases: Array<out String> = emptyArray() // additional names under which the module is known
) : Listenable, Configurable(name), QuickImports {

    val valueEnabled = boolean("Enabled", state).also {
        // Might not include the enabled state of the module depending on the category
        if (category == Category.MISC || category == Category.FUN || category == Category.RENDER) {
            if (this is ModuleAntiBot) {
                return@also
            }

            it.doNotInclude()
        }
    }.notAnOption()

    private var calledSinceStartup = false

    // Module options
    var enabled by valueEnabled.onChange { new ->
        // Check if the module is locked
        locked?.let { locked ->
            if (locked.get()) {
                notification(
                    this.name,
                    translation("liquidbounce.generic.locked"),
                    NotificationEvent.Severity.ERROR
                )

                // Keeps it turned off
                return@onChange false
            }
        }

        runCatching {
            if (!inGame) {
                return@runCatching
            }

            calledSinceStartup = true

            // Call enable or disable function
            if (new) {
                enable()
            } else {
                disable()
            }
        }.onSuccess {
            // Save new module state when module activation is enabled
            if (disableActivation) {
                return@onChange false
            }

            if (!loadingNow) {
                notification(
                    if (new) translation("liquidbounce.generic.enabled")
                    else translation("liquidbounce.generic.disabled"),
                    this.name,
                    if (new) NotificationEvent.Severity.ENABLED else NotificationEvent.Severity.DISABLED
                )
            }

            // Call out module event
            EventManager.callEvent(ToggleModuleEvent(name, hidden, new))

            // Call to state-aware sub-configurables
            inner.filterIsInstance<ChoiceConfigurable<*>>().forEach { it.newState(new) }
            inner.filterIsInstance<ToggleableConfigurable>().forEach { it.newState(new) }
        }.onFailure {
            // Log error
            logger.error("Module failed to ${if (new) "enable" else "disable"}.", it)
            // In case of an error, module should stay disabled
            throw it
        }

        new
    }

    var bind by key("Bind", bind)
        .doNotInclude()
    var hidden by boolean("Hidden", hide)
        .doNotInclude()
        .onChange {
            EventManager.callEvent(RefreshArrayListEvent())
            it
        }

    /**
     * If this value is on true, we cannot enable the module, as it likely does not bypass.
     */
    private var locked: Value<Boolean>? = null

    open val translationBaseKey: String
        get() = "liquidbounce.module.${name.toLowerCamelCase()}"

    private val descriptionKey
        get() = "$translationBaseKey.description"

    open val description: String
        get() = translation(descriptionKey).convertToString()

    // Tag to be displayed on the HUD
    open val tag: String?
        get() = null

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    @ScriptApi
    open val settings by lazy { inner.associateBy { it.name } }

    init {
        if (!LanguageManager.hasFallbackTranslation(descriptionKey)) {
            logger.warn("$name is missing fallback description key $descriptionKey")
        }
    }

    /**
     * Called when module is turned on
     */
    open fun enable() {}

    /**
     * Called when module is turned off
     */
    open fun disable() {}

    /**
     * Called when the module is added to the module manager
     */
    open fun init() {}

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = enabled && inGame

    /**
     * Handles disconnect and if [disableOnQuit] is true disables module
     */
    @Suppress("unused")
    val onDisconnect = handler<DisconnectEvent>(ignoreCondition = true) {
        if (enabled && disableOnQuit) {
            enabled = false
        }
    }

    @Suppress("unused")
    val onWorldChange = handler<WorldChangeEvent>(ignoreCondition = true) {
        if (enabled && !calledSinceStartup && it.world != null) {
            calledSinceStartup = true
            enable()
        }
    }

    /**
     * If we want a module to have the requires bypass option, we specifically call it
     * on init. This will add the option and enable the feature.
     */
    fun enableLock() {
        this.locked = boolean("Locked", false)
    }

    protected fun <T: Choice> choices(name: String, active: T, choices: Array<T>) =
        choices(this, name, active, choices)

    protected fun <T : Choice> choices(
        name: String,
        activeCallback: (ChoiceConfigurable<T>) -> T,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(this, name, activeCallback, choicesCallback)

    fun message(key: String, vararg args: Any) = translation("$translationBaseKey.messages.$key", *args)

}
