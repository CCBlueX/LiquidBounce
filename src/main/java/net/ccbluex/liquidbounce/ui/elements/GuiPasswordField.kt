/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.elements

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField

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