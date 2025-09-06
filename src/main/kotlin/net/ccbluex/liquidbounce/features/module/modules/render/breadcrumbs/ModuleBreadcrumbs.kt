package net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.modes.*
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b

object ModuleBreadcrumbs : ClientModule("Breadcrumbs", Category.RENDER, aliases = arrayOf("PlayerTrails")) {

    override val baseKey: String
        get() = "liquidbounce.module.breadcrumbs"

    val modes = choices(
        "Mode", TrailMode, arrayOf(TrailMode,RiseMode
        )
    )

    val colorMode = choices("ColorMode", 3) {
        arrayOf(
            GenericCustomColorMode(it, Color4b.WHITE.with(a = 80), Color4b.WHITE.with(a = 100)),
            GenericStaticColorMode(it, Color4b(0, 255, 4)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it)
        )
    }

    override fun onEnabled() {
        modes.activeChoice.enable()
    }

    override fun onDisabled() {
        modes.activeChoice.disable()
    }
}
