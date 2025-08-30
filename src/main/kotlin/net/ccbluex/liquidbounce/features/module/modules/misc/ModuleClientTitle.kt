@file:Suppress("unused")
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.SharedConstants

object ModuleClientTitle: ClientModule("ClientTitle", Category.MISC, aliases = arrayOf("ModifyTitle")) {

    private val originTitle = LiquidBounce.CLIENT_NAME + " v" + LiquidBounce.clientVersion + " | " + SharedConstants.getGameVersion().name
    private val clientTitle by text("Title", "").onChanged {
        val title = it.ifEmpty {
            originTitle
        }
       if (enabled && !HideAppearance.isHidingNow) {
           mc.window.setTitle(title)
       }
    }
    override fun onEnabled() {
        val title = clientTitle.ifEmpty {
            originTitle
        }
        mc.window.setTitle(title)
    }

    override fun onDisabled() {
        mc.window.setTitle(originTitle)
        super.onDisabled()
    }
}
