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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.tabs.Tabs
import net.ccbluex.liquidbounce.native.Natives
import net.ccbluex.liquidbounce.renderer.engine.RenderEngine
import net.ccbluex.liquidbounce.renderer.ultralight.WebPlatform
import net.ccbluex.liquidbounce.renderer.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.extensions.RotationManager
import net.ccbluex.liquidbounce.utils.extensions.globalEnemyConfigurable
import net.ccbluex.liquidbounce.utils.extensions.loginAltening
import net.ccbluex.liquidbounce.utils.mc
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
     * Client logger to print out console messages
     */
    val logger = LogManager.getLogger(CLIENT_NAME)!!

    /**
     * Should be executed to start the client.
     */
    val startHandler = handler<ClientStartEvent> {
        logger.info("Launching $CLIENT_NAME v$CLIENT_VERSION by $CLIENT_AUTHOR")
        logger.debug("Loading from cloud: '$CLIENT_CLOUD'")

        // Initialize client features
        EventManager

        // Config
        ConfigSystem
        globalEnemyConfigurable

        // Features
        ModuleManager
        CommandManager
        ThemeManager
        ScriptManager
        RotationManager
        Tabs
        Chat

        // Download natives
        Natives.downloadNatives()

        // Initialize the render engine
        RenderEngine.init()

        // Load up web platform
        WebPlatform.init()

        // Register commands and modules
        CommandManager.registerInbuilt()
        ModuleManager.registerInbuilt()

        // Load user scripts
        ScriptManager.loadScripts()

        // Load config system from disk
        ConfigSystem.load()

        mc.sessionService.loginAltening("xmd8s-fph68@alt.com")

        // Connect to chat server
        Chat.connect()

        logger.info("Successfully loaded client!")
    }

    /**
     * Should be executed to stop the client.
     */
    val shutdownHandler = handler<ClientShutdownEvent> {
        logger.info("Shutting down client...")
        ConfigSystem.store()
    }

}
