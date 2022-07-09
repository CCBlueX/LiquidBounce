/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.gui

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiGameOver
import net.minecraft.client.gui.GuiGameOver

class GuiGameOverImpl<out T : GuiGameOver>(wrapped: T) : GuiScreenImpl<T>(wrapped), IGuiGameOver
{
    override val enableButtonsTimer: Int
        get() = wrapped.enableButtonsTimer
}

fun IGuiGameOver.unwrap(): GuiGameOver = (this as GuiGameOverImpl<*>).wrapped
fun GuiGameOver.wrap(): IGuiGameOver = GuiGameOverImpl(this)
