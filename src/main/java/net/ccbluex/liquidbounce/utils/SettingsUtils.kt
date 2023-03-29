/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.Spammer
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.EntityUtils.targetAnimals
import net.ccbluex.liquidbounce.utils.EntityUtils.targetDead
import net.ccbluex.liquidbounce.utils.EntityUtils.targetInvisible
import net.ccbluex.liquidbounce.utils.EntityUtils.targetMobs
import net.ccbluex.liquidbounce.utils.EntityUtils.targetPlayer
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard

/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
object SettingsUtils {

    /**
     * Execute settings [script]
     */
    fun executeScript(script: String) {
        script.lines().forEachIndexed { index, s ->
            if (s.isEmpty() || s.startsWith('#')) return@forEachIndexed

            val args = s.split(" ").toTypedArray()

            if (args.size <= 1) {
                displayChatMessage("§7[§3§lAutoSettings§7] §cSyntax error at line '$index' in setting script.\n§8§lLine: §7$s")
                return@forEachIndexed
            }

            when (args[0]) {
                "chat" -> displayChatMessage("§7[§3§lAutoSettings§7] §e${translateAlternateColorCodes(StringUtils.toCompleteString(args, 1))}")
                "unchat" -> displayChatMessage(translateAlternateColorCodes(StringUtils.toCompleteString(args, 1)))

                "load" -> {
                    val urlRaw = StringUtils.toCompleteString(args, 1)
                    val url = if (urlRaw.startsWith("http"))
                        urlRaw
                    else
                        "$CLIENT_CLOUD/settings/${urlRaw.lowercase()}"

                    try {
                        displayChatMessage("§7[§3§lAutoSettings§7] §7Loading settings from §a§l$url§7...")
                        executeScript(get(url))
                        displayChatMessage("§7[§3§lAutoSettings§7] §7Loaded settings from §a§l$url§7.")
                    } catch (e: Exception) {
                        displayChatMessage("§7[§3§lAutoSettings§7] §7Failed to load settings from §a§l$url§7.")
                    }
                }

                "targetPlayer", "targetPlayers" -> {
                    targetPlayer = args[1].equals("true", ignoreCase = true)
                    displayChatMessage("§7[§3§lAutoSettings§7] §a§l${args[0]}§7 set to §c§l${targetPlayer}§7.")
                }

                "targetMobs" -> {
                    targetMobs = args[1].equals("true", ignoreCase = true)
                    displayChatMessage("§7[§3§lAutoSettings§7] §a§l${args[0]}§7 set to §c§l${targetMobs}§7.")
                }

                "targetAnimals" -> {
                    targetAnimals = args[1].equals("true", ignoreCase = true)
                    displayChatMessage("§7[§3§lAutoSettings§7] §a§l${args[0]}§7 set to §c§l${targetAnimals}§7.")
                }

                "targetInvisible" -> {
                    targetInvisible = args[1].equals("true", ignoreCase = true)
                    displayChatMessage("§7[§3§lAutoSettings§7] §a§l${args[0]}§7 set to §c§l${targetInvisible}§7.")
                }

                "targetDead" -> {
                    targetDead = args[1].equals("true", ignoreCase = true)
                    displayChatMessage("§7[§3§lAutoSettings§7] §a§l${args[0]}§7 set to §c§l${targetDead}§7.")
                }

                else -> {
                    // Text values can have spaces in them
                    if (args.size < 3) {
                        displayChatMessage("§7[§3§lAutoSettings§7] §cSyntax error at line '$index' in setting script.\n§8§lLine: §7$s")
                        return@forEachIndexed
                    }

                    val moduleName = args[0]
                    val valueName = args[1]
                    var value = args[2]
                    val module = moduleManager[moduleName]

                    if (module == null) {
                        displayChatMessage("§7[§3§lAutoSettings§7] §cModule §a§l$moduleName§c was not found!")
                        return@forEachIndexed
                    }

                    if (valueName.equals("toggle", ignoreCase = true)) {
                        module.state = value.equals("true", ignoreCase = true)
                        displayChatMessage("§7[§3§lAutoSettings§7] §a§l${module.name} §7was toggled §c§l${if (module.state) "on" else "off"}§7.")
                        return@forEachIndexed
                    }

                    if (valueName.equals("bind", ignoreCase = true)) {
                        module.keyBind = Keyboard.getKeyIndex(value)
                        displayChatMessage("§7[§3§lAutoSettings§7] §a§l${module.name} §7was bound to §c§l${Keyboard.getKeyName(module.keyBind)}§7.")
                        return@forEachIndexed
                    }

                    val moduleValue = module[valueName]
                    if (moduleValue == null) {
                        displayChatMessage("§7[§3§lAutoSettings§7] §cValue §a§l$valueName§c don't found in module §a§l$moduleName§c.")
                        return@forEachIndexed
                    }

                    try {
                        when (moduleValue) {
                            is BoolValue -> moduleValue.changeValue(value.toBoolean())
                            is FloatValue -> moduleValue.changeValue(value.toFloat())
                            is IntegerValue -> moduleValue.changeValue(value.toInt())
                            is TextValue -> {
                                // Load text values with spaces
                                value = StringUtils.toCompleteString(args, 2)
                                moduleValue.changeValue(value)
                            }
                            is ListValue -> moduleValue.changeValue(value)
                        }

                        displayChatMessage("§7[§3§lAutoSettings§7] §a§l${module.name}§7 value §8§l${moduleValue.name}§7 set to §c§l$value§7.")
                    } catch (e: Exception) {
                        displayChatMessage("§7[§3§lAutoSettings§7] §a§l${e.javaClass.name}§7(${e.message}) §cAn Exception occurred while setting §a§l$value§c to §a§l${moduleValue.name}§c in §a§l${module.name}§c.")
                    }
                }
            }
        }

        saveConfig(valuesConfig)
    }

    /**
     * Generate settings script
     */
    fun generateScript(values: Boolean, binds: Boolean, states: Boolean): String {
        var string = ""
        val all = values && binds && states

        for (module in moduleManager.modules) {
            if (module.category == ModuleCategory.RENDER || module is NameProtect || module is Spammer) continue

            if (values)
                for (value in module.values) {
                    // Skip hidden values in ClickGUI
                    if (all || value.isSupported())
                        string += "${module.name} ${value.name} ${value.get()}\n"
                }

            if (states)
                string += "${module.name} toggle ${module.state}\n"

            if (binds)
                string += "${module.name} bind ${Keyboard.getKeyName(module.keyBind)}\n"
        }

        return string
    }
}