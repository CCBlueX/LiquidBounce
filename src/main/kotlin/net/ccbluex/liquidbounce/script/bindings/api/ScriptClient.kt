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
 *
 *
 */
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.CombatManager

/**
 * The main hub of the ScriptAPI client that provides access to a useful set of members.
 *
 * Access variables using `client` in the script
 * client.getEventManager()...
 * client.getConfigSystem()...
 * client.getModuleManager()...
 *
 * @since 1.0
 */
@Suppress("unused")
object ScriptClient {

    val eventManager = EventManager
    val configSystem = ConfigSystem
    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val scriptManager = ScriptManager
    val combatManager = CombatManager

    /**
     * Shows [message] in the client-chat
     */
    @Suppress("unused")
    @JvmName("displayChatMessage")
    fun displayChatMessage(message: String) = chat(message)

}
