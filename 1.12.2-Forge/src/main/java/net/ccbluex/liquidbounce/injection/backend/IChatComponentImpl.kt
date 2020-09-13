/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.util.IChatStyle
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.minecraft.util.text.ITextComponent

class IChatComponentImpl(val wrapped: ITextComponent) : IIChatComponent {
    override val unformattedText: String
        get() = wrapped.unformattedText
    override val chatStyle: IChatStyle
        get() = wrapped.style.wrap()
    override val formattedText: String
        get() = wrapped.formattedText

    override fun appendText(text: String) {
        wrapped.appendText(text)
    }

    override fun appendSibling(component: IIChatComponent) {
        wrapped.appendSibling(component.unwrap())
    }

    override fun equals(other: Any?): Boolean {
        return other is IChatComponentImpl && other.wrapped == this.wrapped
    }
}

inline fun IIChatComponent.unwrap(): ITextComponent = (this as IChatComponentImpl).wrapped
inline fun ITextComponent.wrap(): IIChatComponent = IChatComponentImpl(this)