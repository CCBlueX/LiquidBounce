/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiSlot

abstract class WrappedGuiSlot(mc: IMinecraft, width: Int, height: Int, top: Int, bottom: Int, slotHeight: Int) {
    lateinit var represented: IGuiSlot

    init {
        represented.width = width
        represented.height = height
        represented.top = top
        represented.bottom = bottom
        represented.slotHeight = slotHeight
        represented.left = 0
        represented.right = width
    }

    protected abstract fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int)
    protected abstract fun drawBackground()
    protected abstract fun elementClicked(var1: Int, doubleClick: Boolean, var3: Int, var4: Int)
    protected abstract fun getSize(): Int
    protected abstract fun isSelected(id: Int): Boolean
}