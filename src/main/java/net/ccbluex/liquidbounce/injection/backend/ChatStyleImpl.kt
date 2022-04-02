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
import net.minecraft.util.ChatStyle

class ChatStyleImpl(val wrapped: ChatStyle) : IChatStyle {
    override var chatClickEvent: IClickEvent?
        get() = wrapped.chatClickEvent?.wrap()
        set(value) {
            wrapped.chatClickEvent = value?.unwrap()
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

inline fun IChatStyle.unwrap(): ChatStyle = (this as ChatStyleImpl).wrapped
inline fun ChatStyle.wrap(): IChatStyle = ChatStyleImpl(this)