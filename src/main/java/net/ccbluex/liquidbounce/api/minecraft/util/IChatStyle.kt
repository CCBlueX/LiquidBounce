/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent

interface IChatStyle {
    var chatClickEvent: IClickEvent?
    var underlined: Boolean
    var color: WEnumChatFormatting?
}