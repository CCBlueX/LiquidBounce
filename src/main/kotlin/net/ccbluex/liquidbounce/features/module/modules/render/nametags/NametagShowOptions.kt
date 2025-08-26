package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.config.types.NamedChoice

internal enum class NametagShowOptions(
    override val choiceName: String
) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    PING("Ping"),
    ITEMS("Items"),
    ITEM_INFO("ItemInfo"),
    ENCHANTMENTS("Enchantments"),
    BORDER("Border");

    fun isShowing() = this in ModuleNametags.show
}
