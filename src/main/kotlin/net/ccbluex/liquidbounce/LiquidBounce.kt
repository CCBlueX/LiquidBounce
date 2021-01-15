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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.chat.Chat
import net.ccbluex.liquidbounce.features.command.CommandExecutor
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.renderer.engine.RenderEngine
import net.ccbluex.liquidbounce.sciter.SciterScreen
import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.LogManager

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LiquidBounce {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_VERSION = "1.0.0"

    /**
     * Client feature managers
     */
    val eventManager = EventManager
    val configSystem = ConfigSystem
    val moduleManager = ModuleManager
    val commandManager = CommandManager
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
    fun start() {
        CommandManager.registerInbuilt()
        // Initialize the executor
        CommandExecutor
        // Initialize the render engine
        RenderEngine.init()

        moduleManager.registerInbuilt()
        commandManager.registerInbuilt()
        configSystem.load()
        chat.connect()

        // open up sciter window
//        SciterWindow
        MinecraftClient.getInstance().openScreen(SciterScreen("hello"))
    }

    /**
     * Should be executed to stop the client.
     */
    fun stop() {
        configSystem.store()
    }

}
