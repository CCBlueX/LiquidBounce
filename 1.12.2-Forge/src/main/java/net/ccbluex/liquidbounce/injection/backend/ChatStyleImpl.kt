/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.event.IClickEvent
import net.ccbluex.liquidbounce.api.minecraft.util.IChatStyle
import net.ccbluex.liquidbounce.api.minecraft.util.WEnumChatFormatting
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.util.text.Style

class ChatStyleImpl(val wrapped: Style) : IChatStyle {
    override var chatClickEvent: IClickEvent?
        get() = wrapped.clickEvent?.wrap()
        set(value) {
            wrapped.clickEvent = value?.unwrap()
        }
    override var underlined: Boolean
        get() = wrapped.underlined
        set(value) {
            wrapped.underlined = value
        }
    override var color: WEnumChatFormatting?
        get() = wrapped.color?.wrap()
        set(value) {
            wrapped.color = value?.unwrap()
        }

    override fun equals(other: Any?): Boolean {
        return other is ChatStyleImpl && other.wrapped == this.wrapped
    }

}

inline fun IChatStyle.unwrap(): Style = (this as ChatStyleImpl).wrapped
inline fun Style.wrap(): IChatStyle = ChatStyleImpl(this)