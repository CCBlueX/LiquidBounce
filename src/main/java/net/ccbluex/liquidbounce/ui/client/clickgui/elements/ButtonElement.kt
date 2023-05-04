/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements

import net.ccbluex.liquidbounce.LiquidBounce.clickGui
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color

@SideOnly(Side.CLIENT)
abstract class ButtonElement(open val displayName: String) : Element() {
    open val color
        get() = Color.WHITE.rgb

    var hoverTime = 0
        set(value) {
            field = value.coerceIn(0, 7)
        }

    override val height = 16

    override fun drawScreenAndClick(mouseX: Int, mouseY: Int, mouseButton: Int?): Boolean {
        clickGui.style.drawButtonElement(mouseX, mouseY, this)
        return false
    }
}