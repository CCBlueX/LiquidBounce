package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.config.types.NamedChoice

// TODO: Split this into detailed configuration
internal enum class NametagShowOptions(
    override val choiceName: String
) : NamedChoice {
    ENCHANTMENTS("Enchantments"),
    BORDER("Border");

    fun isShowing() = this in ModuleNametags.show
}

