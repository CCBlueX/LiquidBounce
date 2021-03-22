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
package net.ccbluex.liquidbounce

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.chat.Chat
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.tabs.Tabs
import net.ccbluex.liquidbounce.native.Natives
import net.ccbluex.liquidbounce.renderer.engine.RenderEngine
import net.ccbluex.liquidbounce.renderer.ultralight.WebPlatform
import net.ccbluex.liquidbounce.renderer.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.extensions.globalEnemyConfigurable

import org.apache.logging.log4j.LogManager

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LiquidBounce : Listenable {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_VERSION = "1.0.0"
    const val CLIENT_AUTHOR = "CCBlueX"
    const val CLIENT_CLOUD = "https://cloud.liquidbounce.net/LiquidBounce"

    /**
     * Client feature managers
     */
    val eventManager = EventManager
    val configSystem = ConfigSystem
    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val scriptManager = ScriptManager
    val themeManager = ThemeManager
    val chat = Chat()


    /**
     * Client logger to print out console messages
     *
     * TODO: Figure out something better to keep track of errors and other useful debug stuff, especially in case of errors.
     *  It would be useful to also log client messages into own logs or in case of a unsuccessful start to show up a panic/error screen
     */
    val logger = LogManager.getLogger(CLIENT_NAME)!!

    /**
     * Should be executed to start the client.
     */
    val startHandler = handler<ClientStartEvent> {
        Natives.downloadNatives()

        commandManager.registerInbuilt()
        // Initialize the executor
        CommandExecutor
        // Initialize the enemy configurable
        globalEnemyConfigurable
        // Initialize the render engine
        RenderEngine.init()

        // Register tabs
        Tabs

        // Load up web platform
        WebPlatform.init()

        moduleManager.registerInbuilt()
        scriptManager.loadScripts()
        configSystem.load()

        chat.connect()
    }

    /**
     * Should be executed to stop the client.
     */
    val shutdownHandler = handler<ClientShutdownEvent> {
        configSystem.store()
    }

}
