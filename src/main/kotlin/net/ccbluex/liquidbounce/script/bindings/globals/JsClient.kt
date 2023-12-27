package net.ccbluex.liquidbounce.script.bindings.globals

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.CombatManager

object JsClient {

    @JvmField
    val eventManager = EventManager
    @JvmField
    val configSystem = ConfigSystem

    @JvmField
    val moduleManager = ModuleManager
    @JvmField
    val commandManager = CommandManager
    @JvmField
    val scriptManager = ScriptManager

    @JvmField
    val combatManager = CombatManager

    /**
     * Shows [message] in the client-chat
     */
    @Suppress("unused")
    @JvmName("displayChatMessage")
    fun displayChatMessage(message: String) = chat(message)

}
