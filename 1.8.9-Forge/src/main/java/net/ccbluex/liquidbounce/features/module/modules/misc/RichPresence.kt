package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import kotlin.concurrent.thread

@ModuleInfo(name = "RichPresence", description = "Show discord presence", category = ModuleCategory.MISC)
class RichPresence : Module() {

    override fun onToggle(state: Boolean) {
        val rpc = LiquidBounce.clientRichPresence
        when (state) {
            false -> rpc.shutdown()
            true -> thread { rpc.setup() }
        }
    }
}