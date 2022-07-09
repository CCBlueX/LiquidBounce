/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.TextEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.withClientPrefix
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.TextValue
import net.ccbluex.liquidbounce.value.ValueGroup

// TODO: Hide GameID (rewinside)
// TODO: Hide other things that can be used to recognize you
@ModuleInfo(name = "NameProtect", description = "Changes playernames clientside.", category = ModuleCategory.MISC)
class NameProtect : Module()
{
    val skinProtectValue = BoolValue("SkinProtect", true)
    private val fakeNameValue = TextValue("FakeName", "&cMe")

    private val allPlayerGroup = ValueGroup("AllPlayer")
    val allPlayerEnabledValue = BoolValue("Enabled", false, "AllPlayers")
    private val allPlayerModeValue = ListValue("Mode", arrayOf("Custom", "Obfuscate", "Empty"), "Custom", "AllPlayers-Mode")
    private val allPlayerCustomFakeNameValue = object : TextValue("CustomName", "Protected User", "AllPlayers-CustomName")
    {
        override fun showCondition() = allPlayerModeValue.get().equals("Custom", ignoreCase = true)
    }

    init
    {
        allPlayerGroup.addAll(allPlayerEnabledValue, allPlayerModeValue, allPlayerCustomFakeNameValue)
    }

    @EventTarget(ignoreCondition = true)
    fun onText(event: TextEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if ((event.text ?: return).contains("".withClientPrefix())) return

        LiquidBounce.fileManager.friendsConfig.friends.forEach { event.text = StringUtils.replace(event.text, it.playerName, translateAlternateColorCodes(it.alias) + "\u00A7f") }

        if (!state) return
        event.text = StringUtils.replace(event.text, thePlayer.name, translateAlternateColorCodes(fakeNameValue.get()) + "\u00A7r")

        if (allPlayerEnabledValue.get())
        {
            val customFakeName = allPlayerCustomFakeNameValue.get()

            mc.netHandler.playerInfoMap.asSequence().map { it.gameProfile.name }.forEach {
                event.text = StringUtils.replace(event.text, it, when (allPlayerModeValue.get().toLowerCase())
                {
                    "obfuscate" -> "\u00A7k$it"
                    "empty" -> ""
                    else -> customFakeName
                })
            }
        }
    }
}
