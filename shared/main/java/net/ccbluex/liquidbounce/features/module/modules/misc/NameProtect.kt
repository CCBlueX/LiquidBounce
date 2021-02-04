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
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.TextValue

@ModuleInfo(name = "NameProtect", description = "Changes playernames clientside.", category = ModuleCategory.MISC)
class NameProtect : Module()
{
	val allPlayersValue = BoolValue("AllPlayers", false)
	val skinProtectValue = BoolValue("SkinProtect", true)
	private val fakeNameValue = TextValue("FakeName", "&cMe")
	private val allPlayersModeValue = ListValue("AllPlayers-Mode", arrayOf("Custom", "Obfuscate", "Empty"), "Custom")
	private val allPlayersCustomFakeNameValue = TextValue("AllPlayers-CustomName", "Protected User")

	@EventTarget(ignoreCondition = true)
	fun onText(event: TextEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if ((event.text ?: return).contains("\u00A78[\u00A79\u00A7l" + LiquidBounce.CLIENT_NAME + "\u00A78] \u00A73")) return

		for (friend in LiquidBounce.fileManager.friendsConfig.friends) event.text = StringUtils.replace(event.text, friend.playerName, translateAlternateColorCodes(friend.alias) + "\u00A7f")

		if (!state) return
		event.text = StringUtils.replace(event.text, thePlayer.name, translateAlternateColorCodes(fakeNameValue.get()) + "\u00A7f")

		if (allPlayersValue.get())
		{
			val customFakeName = allPlayersCustomFakeNameValue.get()

			mc.netHandler.playerInfoMap.asSequence().map { it.gameProfile.name }.forEach {
				event.text = StringUtils.replace(
					event.text, it, when (allPlayersModeValue.get().toLowerCase())
					{
						"obfuscate" -> "\u00A7k$it"
						"empty" -> ""
						else -> customFakeName
					}
				)
			}
		}
	}
}
