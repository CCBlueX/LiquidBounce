/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.ui.client.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData

object ServerUtils : MinecraftInstance()
{
    @JvmStatic
    var lastServerData: ServerData? = null

    @JvmStatic
    val lastServerIp: String?
        get()
        {
            val lastServerData = lastServerData
            return if (lastServerData != null && !mc.isIntegratedServerRunning) lastServerData.serverIP else null
        }

    @JvmStatic
    val remoteIp: String
        get()
        {
            if ((mc.theWorld ?: return "World is null").isRemote)
            {
                val serverData = mc.currentServerData

                if (serverData != null) return serverData.serverIP
            }

            return "Singleplayer"
        }

    @JvmStatic
    fun connectToLastServer()
    {
        mc.displayGuiScreen(GuiConnecting(GuiMultiplayer(GuiMainMenu()), mc, lastServerData ?: return))
    }
}
