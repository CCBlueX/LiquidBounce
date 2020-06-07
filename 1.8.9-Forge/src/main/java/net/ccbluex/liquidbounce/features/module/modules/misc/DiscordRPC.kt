/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.discord.DiscordRichPresence
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils

@ModuleInfo(name = "DiscordRPC", description = "Displays LiquidBounce status on your Discord profile.", category = ModuleCategory.MISC)
class DiscordRPC : Module() {

    init {
        if (LiquidBounce.fileManager.firstStart) {
            state = true
            array = false
        }
    }

    private fun enableDiscordRPC() {
        val setupDiscordRPC = Thread {
            try {
                DiscordRichPresence.clientRichPresence.setup()
                if (!state) {
                    state = true
                }
            } catch (e: Throwable) {
                ClientUtils.getLogger().error("Failed to enable Discord RPC module.", e)
                state = false
            }
        }
        setupDiscordRPC.start()
    }

    override fun onEnable() {
        try {
            if (!DiscordRichPresence.clientRichPresence.running) {
                enableDiscordRPC()
            }
        } catch (e: Throwable) {
            ClientUtils.getLogger().info("Initializing Discord RPC.")
            enableDiscordRPC()
        }
    }

    override fun onDisable() {
        DiscordRichPresence.clientRichPresence.shutdown()
    }

}