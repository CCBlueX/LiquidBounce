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
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.logger
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.world.World
import org.lwjgl.glfw.GLFW

open class Module(val name: String, val category: Category, val bind: Int = GLFW.GLFW_KEY_UNKNOWN,
                  defaultState: Boolean = false, val disableActivation: Boolean = false) : Listenable, Configurable(name) {

    private var enabled by boolean("enabled", defaultState)

    var state
        set(value) {
            runCatching {
                // Call enable or disable function
                if (value) {
                    enable()
                }else{
                    disable()
                }
            }.onSuccess {
                // Save new module state when module activation is enabled
                if (!disableActivation) {
                    enabled = value
                }
            }.onFailure {
                // Log error
                logger.error("Module toggle failed (old: $enabled, new: $value)", it)
                // In case of a error module should stay disabled
                enabled = false
            }
        }
        get() = enabled

    protected val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.mc
    protected val player: ClientPlayerEntity
        get() = mc.player!!
    protected val world: World
        get() = mc.world!!

    open fun enable() { }

    open fun disable() { }

    /**
     * Registers an event hook for events of type [T]
     */
    inline fun <reified T : Event> sequenceHandler(ignoreCondition: Boolean = false, noinline eventHandler: SuspendableHandler<T>) {
        handler<T>(ignoreCondition) { event -> Sequence(eventHandler, event) }
    }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state

}
