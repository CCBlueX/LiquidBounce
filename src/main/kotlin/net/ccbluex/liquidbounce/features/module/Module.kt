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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.int
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.logger
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.util.InputUtil
import net.minecraft.world.World
import org.lwjgl.glfw.GLFW

/**
 * A module also called 'hack' is able to be enabled and handle events
 */
open class Module(val name: String, val category: Category, var description: String = "", defaultBind: Int = GLFW.GLFW_KEY_UNKNOWN,
                  defaultState: Boolean = false, val disableActivation: Boolean = false, hide: Boolean = false) : Listenable, Configurable(name) {

    // Module options
    var enabled by boolean("enabled", defaultState, change = { old, new ->
        runCatching {
            // Call enable or disable function
            if (new) {
                enable()
            }else{
                disable()
            }
        }.onSuccess {
            // Save new module state when module activation is enabled
            if (disableActivation) {
                error("module disabled activation")
            }
            // Call out module event
            EventManager.callEvent(ModuleEvent(this, new))
        }.onFailure {
            // Log error
            logger.error("Module toggle failed (old: $old, new: $new)", it)
            // In case of an error module should stay disabled
            throw it
        }
    })
    var bind by int("bind", defaultBind)
    var hidden by boolean("hidden", hide)

    // Tag to be displayed on the HUD
    open val tag: String?
        get() = null

    /**
     * Quick access
     */
    protected val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.mc
    protected val player: ClientPlayerEntity
        get() = mc.player!!
    protected val world: World
        get() = mc.world!!

    /**
     * Execute when module is turned on
     */
    open fun enable() { }

    /**
     * Execute when module is turned off
     */
    open fun disable() { }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = enabled

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
open class Mode(val name: String, val module: Module) : Listenable, Configurable(name) {

    var state = false

    protected val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.mc
    protected val player: ClientPlayerEntity
        get() = mc.player!!
    protected val world: World
        get() = mc.world!!

    /**
     * Events should be handled when mode is enabled
     */
    override fun handleEvents() = module.enabled && state

}

/**
 * Registers an event hook for events of type [T] and launches a sequence
 */
inline fun <reified T : Event> Listenable.sequenceHandler(ignoreCondition: Boolean = false, noinline eventHandler: SuspendableHandler<T>) {
    handler<T>(ignoreCondition) { event -> Sequence(eventHandler, event) }
}

/**
 * Registers a repeatable sequence which continues to execute until the module is turned off
 */
inline fun Listenable.repeatableSequence(noinline eventHandler: SuspendableHandler<ModuleEvent>) {
    var sequence: Sequence<ModuleEvent>? = null
    handler<ModuleEvent>(true) { event ->
        if (event.module != this)
            return@handler

        sequence = if (event.newState) {
            Sequence(eventHandler, event, loop = true)
        } else {
            sequence?.cancel()
            null
        }
    }
}
