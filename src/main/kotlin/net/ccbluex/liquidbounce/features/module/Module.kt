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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.ToggleModuleEvent
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld
import net.minecraft.text.TranslatableText
import org.lwjgl.glfw.GLFW

/**
 * A module also called 'hack' is able to be enabled and handle events
 */
open class Module(
    name: String, // name parameter in configurable
    @Exclude
    val category: Category, // module category
    bind: Int = GLFW.GLFW_KEY_UNKNOWN, // default bind
    state: Boolean = false, // default state
    @Exclude
    val disableActivation: Boolean = false, // disable activation
    hide: Boolean = false, // default hide
) : Listenable, Configurable(name) {

    open val translationBaseKey: String
        get() = "liquidbounce.module.${name.toLowerCamelCase()}"

    val description: String
        get() = "$translationBaseKey.description"

    // Module options
    var enabled by boolean("Enabled", state)
        .listen { new ->
            runCatching {
                // Check if player is in-game
                if (mc.player == null || mc.world == null) {
                    return@runCatching
                }

                // Call enable or disable function
                if (new) {
                    enable()
                } else {
                    disable()
                }
            }.onSuccess {
                // Save new module state when module activation is enabled
                if (disableActivation) {
                    return@listen false
                }

                notification(
                    if (new) TranslatableText("liquidbounce.generic.enabled") else TranslatableText("liquidbounce.generic.disabled"),
                    this.name,
                    NotificationEvent.Severity.INFO
                )

                // Call out module event
                EventManager.callEvent(ToggleModuleEvent(this, new))

                // Call to choices
                value.filterIsInstance<ChoiceConfigurable>()
                    .forEach { it.newState(new) }
            }.onFailure {
                // Log error
                logger.error("Module failed to ${if (new) "enable" else "disable"}.", it)
                // In case of an error module should stay disabled
                throw it
            }

            new
        }

    var bind by int("Bind", bind, 0..0)
    var hidden by boolean("Hidden", hide)

    // Tag to be displayed on the HUD
    open val tag: String?
        get() = null

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
    protected val interaction: ClientPlayerInteractionManager
        get() = mc.interactionManager!!

    /**
     * Called when module is turned on
     */
    open fun enable() { }

    /**
     * Called when module is turned off
     */
    open fun disable() { }

    /**
     * Called when the module is added to the module manager
     */
    open fun init() { }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = enabled && mc.player != null && mc.world != null

    fun message(key: String, vararg args: Any): TranslatableText {
        return TranslatableText("$translationBaseKey.messages.$key", args)
    }

}
