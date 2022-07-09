package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts

val IFontRenderer.serialized: String?
    get() = if (isGameFontRenderer()) "${getGameFontRenderer().defaultFont}" else if (this == Fonts.minecraftFont) "Minecraft" else Fonts.getFontDetails(this)?.let { "$it" }
