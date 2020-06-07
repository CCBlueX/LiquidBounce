/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils

@ModuleInfo(name = "DiscordRPC", description = "Displays LiquidBounce status on your Discord profile.", category = ModuleCategory.MISC)
class DiscordRPC : Module() {

    init {
        state = true
        array = false
    }

    override fun onEnable() {
        try {
            if (!LiquidBounce.clientRichPresence.running) {
                val setupDiscordRPC = Thread {
                    try {
                        LiquidBounce.clientRichPresence.setup()
                    } catch (e: Throwable) {
                        ClientUtils.getLogger().error("Failed to enable Discord RPC module.", e)
                    }
                }
                setupDiscordRPC.start()
            }
        } catch (e: Throwable) {
            ClientUtils.getLogger().debug("Discord RPC not initialized yet.")
        }
    }

    override fun onDisable() {
        LiquidBounce.clientRichPresence.shutdown()
    }

}