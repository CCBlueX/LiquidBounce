/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.input.Mouse

@ModuleInfo(name = "MidClick", description = "Allows you to add a player as a friend by right clicking him. (a.k.a. MiddleClickFriend)", category = ModuleCategory.MISC)
class MidClick : Module()
{
    private var wasDown = false

    @EventTarget
    fun onRender(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
    {
        if (mc.currentScreen != null) return

        if (!wasDown && Mouse.isButtonDown(2))
        {
            val entity = (mc.objectMouseOver ?: return).entityHit
            val thePlayer = mc.thePlayer ?: return

            if (entity is EntityPlayer)
            {
                val playerName = stripColor(entity.name)
                val friendsConfig = LiquidBounce.fileManager.friendsConfig

                if (!friendsConfig.isFriend(playerName))
                {
                    friendsConfig.addFriend(playerName)
                    FileManager.saveConfig(friendsConfig)
                    ClientUtils.displayChatMessage(thePlayer, "\u00A7a\u00A7l$playerName\u00A7c was added to your friends.")
                }
                else
                {
                    friendsConfig.removeFriend(playerName)
                    FileManager.saveConfig(friendsConfig)
                    ClientUtils.displayChatMessage(thePlayer, "\u00A7a\u00A7l$playerName\u00A7c was removed from your friends.")
                }
            }
            else ClientUtils.displayChatMessage(thePlayer, "\u00A7c\u00A7lError: \u00A7aYou need to select a player.")
        }

        wasDown = Mouse.isButtonDown(2)
    }
}
