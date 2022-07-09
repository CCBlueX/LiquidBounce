/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiSlot
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider

abstract class WrappedGuiSlot(mc: IMinecraft, width: Int, height: Int, top: Int, bottom: Int, slotHeight: Int)
{
    lateinit var represented: IGuiSlot

    init
    {
        @Suppress("LeakingThis") classProvider.wrapGuiSlot(this, mc, width, height, top, bottom, slotHeight)
    }

    abstract fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
    abstract fun drawBackground()
    abstract fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int)
    abstract fun getSize(): Int
    abstract fun isSelected(id: Int): Boolean
}
