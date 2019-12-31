package net.ccbluex.liquidbounce.ui.elements

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class GuiPasswordField(componentId: Int, fontrendererObj: FontRenderer, x: Int, y: Int, par5Width: Int, par6Height: Int) : GuiTextField(componentId, fontrendererObj, x, y, par5Width, par6Height) {

    /**
     * Draw text box
     */
    override fun drawTextBox() {
        val realText = text

        val stringBuilder = StringBuilder()
        for (i in text.indices) stringBuilder.append('*')
        text = stringBuilder.toString()

        super.drawTextBox()
        text = realText
    }

}