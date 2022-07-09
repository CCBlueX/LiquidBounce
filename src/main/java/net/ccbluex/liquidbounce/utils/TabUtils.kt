/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.minecraft.client.gui.GuiTextField

object TabUtils
{
    @JvmStatic
    fun tab(vararg textFields: GuiTextField)
    {
        var index = 0
        val tabsSize = textFields.size

        if (tabsSize == 1) textFields[index].isFocused = true
        else while (index < tabsSize)
        {
            val textField = textFields[index]

            if (textField.isFocused)
            {
                textField.isFocused = false
                index++

                if (index >= textFields.size) index = 0

                textFields[index].isFocused = true
                break
            }

            index++
        }
    }
}
