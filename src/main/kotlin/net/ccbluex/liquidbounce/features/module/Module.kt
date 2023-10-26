/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

/**
 * A module also called 'hack' can be enabled and handle events
 */
open class Module(
    name: String, // name parameter in configurable
    @Exclude val category: Category, // module category
    bind: Int = GLFW.GLFW_KEY_UNKNOWN, // default bind
    state: Boolean = false, // default state
    @Exclude val disableActivation: Boolean = false, // disable activation
    hide: Boolean = false, // default hide
    @Exclude val disableOnQuit: Boolean = false // disables module when player leaves the world
) : Listenable, Configurable(name) {

    val valueEnabled = boolean("Enabled", state)
        .doNotInclude()

    // Module options
    var enabled by valueEnabled.listen { new ->
        runCatching {
            // Call enable or disable function
            if (new) {
                enable()
            } else {
                disable()
            }

            // If successful might store configuration
            ConfigSystem.storeConfigurable(ModuleManager.modulesConfigurable)
        }.onSuccess {
            // Save new module state when module activation is enabled
            if (disableActivation) {
                return@listen false
            }

            notification(
                if (new) Text.translatable("liquidbounce.generic.enabled") else Text.translatable("liquidbounce.generic.disabled"),
                this.name,
                if (new) NotificationEvent.Severity.ENABLED else NotificationEvent.Severity.DISABLED
            )

            // Ignore handleEvents condition to prevent enabled modules from freezing post game load
            val notInGame = (mc.player == null || mc.world == null) && new

            // Call out module event
            EventManager.callEvent(ToggleModuleEvent(this, new, notInGame))

            // Call to choices
            value.filterIsInstance<ChoiceConfigurable>().forEach { it.newState(new) }
        }.onFailure {
            // Log error
            logger.error("Module failed to ${if (new) "enable" else "disable"}.", it)
            // In case of an error, module should stay disabled
            throw it
        }

        new
    }

    var bind by int("Bind", bind, 0..0)
        .doNotInclude()
    var hidden by boolean("Hidden", hide)
        .doNotInclude()

    open val translationBaseKey: String
        get() = "liquidbounce.module.${name.toLowerCamelCase()}"

    open val description: String
        get() = "$translationBaseKey.description"

    // Tag to be displayed on the HUD
    open val tag: String?
        get() = null

    /**
     * Quick access
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
    override fun handleEvents() = enabled && mc.player != null && mc.world != null

    /**
     * Handles disconnect from world and if [disableOnQuit] is true disables module
     */
    val onDisconnect = handler<WorldDisconnectEvent> {
        if (disableOnQuit) {
            enabled = false
        }
    }

    /**
     * Returns if module is hidden. Hidden modules are not displayed in the module list.
     * Used for HTML UI. DO NOT REMOVE!
     */
    fun isHidden() = hidden

    fun message(key: String, vararg args: Any) = Text.translatable("$translationBaseKey.messages.$key", *args)

}
