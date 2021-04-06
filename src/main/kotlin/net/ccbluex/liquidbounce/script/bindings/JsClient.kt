package net.ccbluex.liquidbounce.script.bindings

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.script.ScriptManager

object JsClient {

    val eventManager = EventManager
    val configSystem = ConfigSystem

    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val themeManager = ThemeManager
    val scriptManager = ScriptManager

}
