/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.minecraft.client.gui.GuiTextField

class GuiTextFieldImpl(val wrapped: GuiTextField) : IGuiTextField {
    override val xPosition: Int
        get() = wrapped.x
    override var text: String
        get() = wrapped.text
        set(value) {
            wrapped.text = value
        }
    override var isFocused: Boolean
        get() = wrapped.isFocused
        set(value) {
            wrapped.isFocused = value
        }
    override var maxStringLength: Int
        get() = wrapped.maxStringLength
        set(value) {
            wrapped.maxStringLength = value
        }

    override fun updateCursorCounter() = wrapped.updateCursorCounter()

    override fun textboxKeyTyped(typedChar: Char, keyCode: Int) = wrapped.textboxKeyTyped(typedChar, keyCode)

    override fun drawTextBox() = wrapped.drawTextBox()

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        wrapped.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) = wrapped.textboxKeyTyped(typedChar, keyCode)

    override fun equals(other: Any?): Boolean {
        return other is GuiTextFieldImpl && other.wrapped == this.wrapped
    }
}

inline fun IGuiTextField.unwrap(): GuiTextField = (this as GuiTextFieldImpl).wrapped
inline fun GuiTextField.wrap(): IGuiTextField = GuiTextFieldImpl(this)