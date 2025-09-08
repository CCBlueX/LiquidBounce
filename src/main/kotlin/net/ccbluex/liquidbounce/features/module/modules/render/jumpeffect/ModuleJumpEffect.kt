package net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes.ImageMode
import net.ccbluex.liquidbounce.features.module.modules.render.jumpeffect.modes.RenderMode
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b


object ModuleJumpEffect : ClientModule("JumpEffect", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.jumpEffect"

    val modes = choices(
        "Mode", ImageMode, arrayOf(
            ImageMode,
            RenderMode
        )
    )
    val colorMode = choices("ColorMode", 3) {
        arrayOf(
            GenericCustomColorMode(it, Color4b.LIQUID_BOUNCE.withAlpha(200), Color4b.CYAN.withAlpha(150)),
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
