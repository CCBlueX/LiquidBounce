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
import kotlin.concurrent.thread

@ModuleInfo(name = "RichPresence", description = "Show discord presence", category = ModuleCategory.MISC)
class RichPresence : Module() {

    override fun onToggle(state: Boolean) {
        val rpc = LiquidBounce.clientRichPresence
        when (state) {
            false -> rpc.shutdown()
            true -> thread {
                try {
                    rpc.setup()
                } catch (throwable: Throwable) {
                    ClientUtils.getLogger().error("Failed to setup Discord RPC.", throwable)
                    ClientUtils.displayChatMessage("§cFailed to setup Discord RPC. (probably a connection problem)")
                }
            }
        }
    }
}