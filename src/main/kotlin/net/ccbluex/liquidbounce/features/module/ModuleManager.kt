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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert
import org.lwjgl.glfw.GLFW

/**
 * Should be sorted by Module::name
 */
private val modules = ArrayList<ClientModule>(256)

/**
 * A fairly simple module manager
 */
object ModuleManager : EventListener, Iterable<ClientModule> by modules {

    val modulesConfigurable = ConfigSystem.root("modules", modules)

    /**
     * Handles keystrokes for module binds.
     * This also runs in GUIs, so that if a GUI is opened while a key is pressed,
     * any modules that need to be disabled on key release will be properly disabled.
     */
    @Suppress("unused")
    private val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        when (event.action) {
            GLFW.GLFW_PRESS -> if (mc.currentScreen == null) {
                    filter { m -> m.bind.matchesKey(event.keyCode, event.scanCode) }
                    .forEach { m ->
                        m.enabled = !m.enabled || m.bind.action == InputBind.BindAction.HOLD
                    }
                }
            GLFW.GLFW_RELEASE ->
                filter { m ->
                    m.bind.matchesKey(event.keyCode, event.scanCode) &&
                        m.bind.action == InputBind.BindAction.HOLD
                }.forEach { m ->
                    m.enabled = false
                }
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        when (event.action) {
            GLFW.GLFW_PRESS -> if (mc.currentScreen == null) {
                filter { m -> m.bind.matchesMouse(event.button) }
                    .forEach { m ->
                        m.enabled = !m.running || m.bind.action == InputBind.BindAction.HOLD
                    }
            }
            GLFW.GLFW_RELEASE ->
                filter { m ->
                    m.bind.matchesMouse(event.button) && m.bind.action == InputBind.BindAction.HOLD
                }.forEach { m -> m.enabled = false }
        }
    }

    /**
     * Handles world change and enables modules that are not enabled yet
     */
    @Suppress("unused")
    private val handleWorldChange = handler<WorldChangeEvent> { event ->
        // Delayed start handling
        if (event.world != null) {
            for (module in modules) {
                if (!module.enabled || module.calledSinceStartup) continue

                try {
                    module.calledSinceStartup = true
                    module.onEnabled()
                } catch (e: Exception) {
                    logger.error("Failed to enable module ${module.name}", e)
                }
            }
        }

        // Store modules configuration after world change, happens on disconnect as well
        ConfigSystem.storeConfigurable(modulesConfigurable)
    }

    /**
     * Handles disconnect and if [Module.disableOnQuit] is true disables module
     */
    @Suppress("unused")
    private val handleDisconnect = handler<DisconnectEvent> {
        for (module in modules) {
            if (module.disableOnQuit) {
                try {
                    module.enabled = false
                } catch (e: Exception) {
                    logger.error("Failed to disable module ${module.name}", e)
                }
            }
        }
    }

    /**
     * Register inbuilt client modules
     */
    fun registerInbuilt() {
        allClientModules.forEach { module ->
            addModule(module)
            module.walkKeyPath()
            module.verifyFallbackDescription()
        }
    }

    fun addModule(module: ClientModule) {
        module.initConfigurable()
        module.onRegistration()
        modules.sortedInsert(module, ClientModule::name)
    }

    fun removeModule(module: ClientModule) {
        if (module.running) {
            module.onDisabled()
        }
        module.unregister()
        modules -= module
    }

    fun clear() {
        modules.clear()
    }

    /**
     * This is being used by UltralightJS for the implementation of the ClickGUI. DO NOT REMOVE!
     */
    @JvmName("getCategories")
    @ScriptApiRequired
    fun getCategories() = Category.entries.mapArray { it.readableName }

    @JvmName("getModules")
    @ScriptApiRequired
    fun getModules(): Iterable<ClientModule> = modules

    @JvmName("getModuleByName")
    @ScriptApiRequired
    fun getModuleByName(module: String) = find { it.name.equals(module, true) }

    operator fun get(moduleName: String) = modules.find { it.name.equals(moduleName, true) }

}
