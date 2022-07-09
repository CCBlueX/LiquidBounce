/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.Spammer
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.extensions.withDoubleQuotes
import net.ccbluex.liquidbounce.utils.extensions.withParentheses
import net.ccbluex.liquidbounce.utils.extensions.withPrefix
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.entity.EntityPlayerSP
import org.lwjgl.input.Keyboard

/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
object SettingsUtils
{
    private fun chat(thePlayer: EntityPlayerSP?, colorCodes: String, message: String) = ClientUtils.displayChatMessage(thePlayer, message.withPrefix("AutoSettings", textColorCodes = colorCodes))

    /**
     * Execute settings [script]
     */
    fun executeScript(script: String)
    {
        val thePlayer = MinecraftInstance.mc.thePlayer

        script.lines().filter(String::isNotEmpty).filter { !it.startsWith('#') }.forEachIndexed { index, s ->
            val args = s.split(" ").toTypedArray()

            if (args.size <= 1)
            {
                chat(thePlayer, "\u00A7c", "Syntax error at \u00A7nline $index\u00A7c in setting script. ${s.withDoubleQuotes("\u00A77", "\u00A78").withParentheses("\u00A78", "\u00A78")}")
                return@forEachIndexed
            }

            when (args[0].toLowerCase())
            {
                "chat" -> chat(thePlayer, "\u00A7e", translateAlternateColorCodes(StringUtils.toCompleteString(args, 1)))
                "unchat" -> ClientUtils.displayChatMessage(thePlayer, translateAlternateColorCodes(StringUtils.toCompleteString(args, 1)))

                "load" ->
                {
                    val urlRaw = StringUtils.toCompleteString(args, 1)
                    val url = if (urlRaw.startsWith("http")) urlRaw
                    else "${LiquidBounce.CLIENT_CLOUD}/settings/${urlRaw.toLowerCase()}"

                    try
                    {
                        chat(thePlayer, "\u00A77", "Loading settings from ${url.withDoubleQuotes("\u00A7b\u00A7l", "\u00A78")}\u00A77...")
                        executeScript(get(url))
                        chat(thePlayer, "\u00A7a", "Successfully loaded settings from ${url.withDoubleQuotes("\u00A7b\u00A7l", "\u00A78")}\u00A7a...")
                    }
                    catch (e: Exception)
                    {
                        chat(thePlayer, "\u00A7c", "Failed to load settings from ${url.withDoubleQuotes("\u00A7b\u00A7l", "\u00A78")}\u00A7c. ($e)")
                    }
                }

                "targetplayer", "targetplayers" ->
                {
                    EntityUtils.targetPlayer = args[1].equals("true", ignoreCase = true)
                    chat(thePlayer, "\u00A7a", "\u00A7l${args[0]}\u00A77 set to ${if (EntityUtils.targetPlayer) "\u00A7a" else "\u00A7c"}\u00A7l${EntityUtils.targetPlayer}\u00A77.")
                }

                "targetmobs" ->
                {
                    EntityUtils.targetMobs = args[1].equals("true", ignoreCase = true)
                    chat(thePlayer, "\u00A7a", "\u00A7l${args[0]}\u00A77 set to ${if (EntityUtils.targetMobs) "\u00A7a" else "\u00A7c"}\u00A7l${EntityUtils.targetMobs}\u00A77.")
                }

                "targetanimals" ->
                {
                    EntityUtils.targetAnimals = args[1].equals("true", ignoreCase = true)
                    chat(thePlayer, "\u00A7a", "\u00A7l${args[0]}\u00A77 set to ${if (EntityUtils.targetAnimals) "\u00A7a" else "\u00A7c"}\u00A7l${EntityUtils.targetAnimals}\u00A77.")
                }

                "targetinvisible" ->
                {
                    EntityUtils.targetInvisible = args[1].equals("true", ignoreCase = true)
                    chat(thePlayer, "\u00A7a", "\u00A7l${args[0]}\u00A77 set to ${if (EntityUtils.targetInvisible) "\u00A7a" else "\u00A7c"}\u00A7l${EntityUtils.targetInvisible}\u00A77.")
                }

                "targetarmorstand" ->
                {
                    EntityUtils.targetArmorStand = args[1].equals("true", ignoreCase = true)
                    chat(thePlayer, "\u00A7a", "\u00A7l${args[0]}\u00A77 set to ${if (EntityUtils.targetArmorStand) "\u00A7a" else "\u00A7c"}\u00A7l${EntityUtils.targetArmorStand}\u00A77.")
                }

                "targetdead" ->
                {
                    EntityUtils.targetDead = args[1].equals("true", ignoreCase = true)
                    chat(thePlayer, "\u00A7a", "\u00A7l${args[0]}\u00A77 set to ${if (EntityUtils.targetDead) "\u00A7a" else "\u00A7c"}\u00A7l${EntityUtils.targetDead}\u00A77.")
                }

                else ->
                {
                    if (args.size != 3)
                    {
                        chat(thePlayer, "\u00A7c", "Syntax error at line '$index' in setting script. ${s.withDoubleQuotes("\u00A77", "\u00A78").withParentheses("\u00A78", "\u00A78")}")
                        return@forEachIndexed
                    }

                    val moduleName = args[0]
                    val valueName = args[1]
                    val value = args[2]
                    val module = LiquidBounce.moduleManager.getModule(moduleName)

                    if (module == null)
                    {
                        chat(thePlayer, "\u00A7c", "Module ${moduleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A7c was not found!")
                        return@forEachIndexed
                    }

                    val actualModuleName = module.name

                    if (valueName.equals("toggle", ignoreCase = true))
                    {
                        module.state = value.equals("true", ignoreCase = true)
                        chat(thePlayer, "\u00A7a", "Module ${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A7a was toggled ${if (module.state) "\u00A7aON" else "\u00A7cOFF"}\u00A77.")
                        return@forEachIndexed
                    }

                    if (valueName.equals("bind", ignoreCase = true))
                    {
                        val binds = value.split(';')
                        module.keyBinds = binds.mapTo(HashSet()) { Keyboard.getKeyIndex(it) }
                        chat(thePlayer, "\u00A7a", "Module ${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A7a was bound to\u00A7c\u00A7l${binds.joinToString()}\u00A77.")
                        return@forEachIndexed
                    }

                    val moduleValue = module.getValue(valueName)
                    if (moduleValue == null)
                    {
                        chat(thePlayer, "\u00A7c", "Value named ${valueName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A7 not found in module  ${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A77.")
                        return@forEachIndexed
                    }

                    try
                    {
                        when (moduleValue)
                        {
                            is BoolValue -> moduleValue.changeValue(value.toBoolean())
                            is IntegerValue -> moduleValue.changeValue(value.toInt())

                            is IntegerRangeValue ->
                            {
                                val pieces = value.split('-', limit = 2)
                                moduleValue.changeMinValue(pieces[0].toInt())
                                moduleValue.changeMinValue(pieces[1].toInt())
                            }

                            is FloatValue -> moduleValue.changeValue(value.toFloat())

                            is FloatRangeValue ->
                            {
                                val pieces = value.split('-', limit = 2)
                                moduleValue.changeMinValue(pieces[0].toFloat())
                                moduleValue.changeMinValue(pieces[1].toFloat())
                            }

                            is TextValue -> moduleValue.changeValue(value)
                            is ListValue -> moduleValue.changeValue(value)
                        }

                        chat(thePlayer, "\u00A7a", "${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A77 value ${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A77 set to \u00A7b\u00A7l$value\u00A77.")
                    }
                    catch (e: Exception)
                    {
                        chat(thePlayer, "\u00A74", "An Exception occurred while setting \u00A7b\u00A7l$value\u00A74 to ${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A74 in ${actualModuleName.withDoubleQuotes("\u00A7a\u00A7l", "\u00A78")}\u00A74. ${"$e".withParentheses("\u00A74\u00A7l", "\u00A78")}")
                    }
                }
            }
        }

        FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
    }

    /**
     * Generate settings script
     */
    fun generateScript(values: Boolean, binds: Boolean, states: Boolean): String
    {
        val stringBuilder = StringBuilder()

        LiquidBounce.moduleManager.modules.filter { it.category != ModuleCategory.RENDER }.filter { it !is NameProtect }.filter { it !is Spammer }.forEach { module ->
            if (values)
            {
                val flatValues = module.flatValues
                flatValues.filterIsInstance<Value<*>>().forEach { value -> stringBuilder.append(module.name).append(" ").append(value.name).append(" ").append(value.get()).append("\n") }
                flatValues.filterIsInstance<RangeValue<*>>().forEach { value -> stringBuilder.append(module.name).append(" ").append(value.name).append(" ").append(value.getMin()).append("-").append(value.getMax()).append("\n") }
            }

            if (states) stringBuilder.append(module.name).append(" toggle ").append(module.state).append("\n")

            if (binds) stringBuilder.append(module.name).append(" bind ").append(module.keyBinds.joinToString(separator = ";") { Keyboard.getKeyName(it) }).append("\n")
        }

        return "$stringBuilder"
    }
}
