/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGui
import net.minecraft.client.gui.Gui

open class GuiImpl<T : Gui>(val wrapped: T) : IGui

fun IGui.unwrap(): Gui = (this as GuiImpl<*>).wrapped
fun Gui.wrap(): IGui = GuiImpl(this)
