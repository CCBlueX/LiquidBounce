/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.file.FileManager.friendsConfig
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.TextValue

@ModuleInfo(name = "NameProtect", description = "Changes player names clientside.", category = ModuleCategory.MISC)
object NameProtect : Module() {

    val allPlayersValue = BoolValue("AllPlayers", false)

    val skinProtectValue = BoolValue("SkinProtect", true)
    private val fakeNameValue = TextValue("FakeName", "&cMe")

    /**
     * Handle text messages from font renderer
     */
    fun handleTextMessage(text: String): String {
        val p = mc.thePlayer ?: return text

        // If the message includes the client name, don't change it
        if ("§8[§9§l$CLIENT_NAME§8] §3" in text) {
            return text
        }

        // Modify
        var newText = text

        for (friend in friendsConfig.friends) {
            newText = newText.replace(friend.playerName, translateAlternateColorCodes(friend.alias) + "§f")
        }

        // If the Name Protect module is disabled, return the text already without further processing
        if (!state) {
            return newText
        }

        // Replace original name with fake name
        newText = newText.replace(p.name, translateAlternateColorCodes(fakeNameValue.get()) + "§f")

        // Replace all other player names with "Protected User"
        if (allPlayersValue.get()) {
            for (playerInfo in mc.netHandler.playerInfoMap) {
                newText = newText.replace(playerInfo.gameProfile.name, "Protected User")
            }
        }

        return newText
    }

}