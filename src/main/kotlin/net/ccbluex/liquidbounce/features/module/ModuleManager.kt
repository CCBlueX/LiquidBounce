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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.script.ScriptApi
import org.lwjgl.glfw.GLFW

private val modules = mutableListOf<Module>()

/**
 * A fairly simple module manager
 */
object ModuleManager : Listenable, Iterable<Module> by modules {

    val modulesConfigurable = ConfigSystem.root("modules", modules)

    /**
     * Handle key input for module binds
     */
    @Suppress("unused")
    val keyHandler = handler<KeyEvent> { ev ->
        if (ev.action == GLFW.GLFW_PRESS) {
            filter { it.bind == ev.key.keyCode } // modules bound to a specific key
                .forEach { it.enabled = !it.enabled } // toggle modules
        }
    }

    @Suppress("unused")
    val worldHandler = handler<WorldChangeEvent> {
        ConfigSystem.storeConfigurable(modulesConfigurable)
    }

    /**
     * Register inbuilt client modules
     */
    fun registerInbuilt() {
        // inbuilt is automatically generated
        inbuilt.apply {
            sortBy { it.name }
            forEach(::addModule)
        }
    }

    private fun addModule(module: Module) {
        module.initConfigurable()
        module.init()
        modules += module
    }

    private fun removeModule(module: Module) {
        if (module.enabled) {
            module.disable()
        }
        module.unregister()
        modules -= module
    }

    /**
     * Allow `ModuleManager += Module` syntax
     */
    operator fun plusAssign(module: Module) {
        addModule(module)
    }

    operator fun plusAssign(modules: MutableList<Module>) {
        modules.forEach(this::addModule)
    }

    operator fun minusAssign(module: Module) {
        removeModule(module)
    }

    operator fun minusAssign(modules: MutableList<Module>) {
        modules.forEach(this::removeModule)
    }

    fun clear() {
        modules.clear()
    }

    fun autoComplete(begin: String, args: List<String>, validator: (Module) -> Boolean = { true }): List<String> {
        val parts = begin.split(",")
        val matchingPrefix = parts.last()
        val resultPrefix = parts.dropLast(1).joinToString(",") + ","
        return filter { it.name.startsWith(matchingPrefix, true) && validator(it) }
            .map {
                if (parts.size == 1) {
                    it.name
                } else {
                    resultPrefix + it.name
                }
            }
    }

    fun parseModulesFromParameter(name: String?): List<Module> {
        if (name == null) return emptyList()
        return name.split(",").mapNotNull { getModuleByName(it) }
    }

    /**
     * This is being used by UltralightJS for the implementation of the ClickGUI. DO NOT REMOVE!
     */
    @JvmName("getCategories")
    @ScriptApi
    fun getCategories() = Category.values().map { it.readableName }.toTypedArray()

    @JvmName("getModules")
    fun getModules() = modules

    @JvmName("getModuleByName")
    @ScriptApi
    fun getModuleByName(module: String) = find { it.name.equals(module, true) }

    operator fun get(moduleName: String) = modules.find { it.name.equals(moduleName, true) }

}
