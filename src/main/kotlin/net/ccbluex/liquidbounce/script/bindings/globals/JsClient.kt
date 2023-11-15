package net.ccbluex.liquidbounce.script.bindings.globals

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.chat

object JsClient {

    val eventManager = EventManager
    val configSystem = ConfigSystem

    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val scriptManager = ScriptManager

    /**
     * Shows [message] in the client-chat
     */
    @Suppress("unused")
    fun displayChatMessage(message: String) = chat(message)

}
